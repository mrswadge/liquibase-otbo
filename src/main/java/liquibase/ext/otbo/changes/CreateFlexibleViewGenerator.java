package liquibase.ext.otbo.changes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewGenerator;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.ext.otbo.preconditions.OtboMaterializedViewExistsPrecondition;
import liquibase.ext.otbo.preconditions.OtboTableExistsPrecondition;
import liquibase.logging.LogService;
import liquibase.logging.Logger;
import liquibase.precondition.core.TableExistsPrecondition;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.CreateViewGenerator;
import liquibase.sqlgenerator.core.DropTableGenerator;
import liquibase.statement.core.CreateViewStatement;
import liquibase.statement.core.DropTableStatement;
import liquibase.util.SqlParser;

public class CreateFlexibleViewGenerator extends OtboSqlGenerator<CreateFlexibleViewStatement> {
	
	private static final Logger log = LogService.getLog(CreateFlexibleViewGenerator.class);
	
	@Override
	public boolean supports( CreateFlexibleViewStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}

	public ValidationErrors validate( CreateFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "viewName", statement.getViewName() );
		validationErrors.checkRequiredField( "selectQuery", statement.getSelectQuery() );
		return validationErrors;
	}

	/**
	 * 1. If there is a materialized view (oracle) or table (mssql only) with the same name as this view, we drop it.
	 * 2. We can then can create the standard view: 'create or replace force view as ... select 1 from dual' 
	 * 3. If the environment is set for materialized views, convert all views to materialized views (oracle) or tables (mssql) at the end using the v99.99.99 script.
	 */
	public Sql[] generateSql( CreateFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		// check if the view already exists, then formulate a plan from there!
		String viewName = statement.getViewName().toUpperCase();
		List<Sql> sequel = new ArrayList<Sql>();

		if ( materializedViewExists( viewName, database ) ) {
			DropMaterializedViewStatement dropMViewStmt = new DropMaterializedViewStatement( statement.getViewName() );
			dropMViewStmt.setSchemaName( database.getLiquibaseSchemaName() );
			DropMaterializedViewGenerator dropMViewGen = new DropMaterializedViewGenerator();
			sequel.addAll( Arrays.asList( dropMViewGen.generateSql( dropMViewStmt, database, null ) ) );
		} else if ( database instanceof MSSQLDatabase && tableExists( viewName, database ) ) {
			DropTableStatement dropTableStmt = new DropTableStatement( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), viewName, true );
			DropTableGenerator dropTableGen = new DropTableGenerator();
			sequel.addAll( Arrays.asList( dropTableGen.generateSql( dropTableStmt, database, null ) ) );
		} // else if there is a view we will use "create or replace view"
		
		// if the view exists as a regular view already, we don't actually care.
		String selectQuery = addSchemaPrefixToFunctionNames( statement.getSelectQuery(), database );
		CreateViewStatement createViewStmt = new CreateViewStatement( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), statement.getViewName(), selectQuery, true );

		CreateViewGenerator createViewGen = new CreateViewGenerator();
		sequel.addAll( Arrays.asList( createViewGen.generateSql( createViewStmt, database, null ) ) );

		log.debug( sequel.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} }  ).collect( Collectors.joining( "\n" ) ) );

		return sequel.toArray( new Sql[0] );
	}
	
	private String addSchemaPrefixToFunctionNames( String sql, Database database ) {
		if ( database instanceof MSSQLDatabase && database.getConnection() instanceof JdbcConnection ) {
			try {
				Statement statement = ( (JdbcConnection) database.getConnection() ).createStatement();
				ResultSet resultSet = statement.executeQuery( "SELECT NAME, SCHEMA_NAME(SCHEMA_ID) FROM SYS.OBJECTS WHERE TYPE = 'FN'" );
				List parsedSql = Arrays.asList( SqlParser.parse( sql, true, true ).toArray( true ) );
				while ( resultSet.next() ) {
					String functionName = resultSet.getString( 1 );
					String schemaName = resultSet.getString( 2 );
					for ( int i = 0; i < parsedSql.size() - 2; i++ ) {
						if ( parsedSql.get( i ).toString().equalsIgnoreCase( functionName ) ) {
							Object next = parsedSql.get( i + 1 );
							if ( next.toString().trim().equals( "" ) ) {
								next = parsedSql.get( i + 2 );
							}
							if ( next.equals( "(" ) ) {
								parsedSql.set( i, schemaName + "." + functionName );
							}
						}
					}
				}
				return String.join( "", parsedSql );
			} catch ( DatabaseException | SQLException e ) {
				throw new RuntimeException( e );
			}
		}
		return sql;
	}
}
