package liquibase.ext.otbo.changes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewGenerator;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateViewGenerator;
import liquibase.sqlgenerator.core.DropTableGenerator;
import liquibase.statement.core.CreateViewStatement;
import liquibase.statement.core.DropTableStatement;
import liquibase.util.SqlParser;

public class CreateFlexibleViewGenerator extends OtboSqlGenerator<CreateFlexibleViewStatement> {
	
	private static final Logger log = Scope.getCurrentScope().getLog(CreateFlexibleViewGenerator.class);
	
	@Override
	public boolean supports( CreateFlexibleViewStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}

	public ValidationErrors validate( CreateFlexibleViewStatement statement, Database database, SqlGeneratorChain<CreateFlexibleViewStatement> sqlGeneratorChain ) {
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
	public Sql[] generateSql( CreateFlexibleViewStatement statement, Database database, SqlGeneratorChain<CreateFlexibleViewStatement> sqlGeneratorChain ) {
		// check if the view already exists, then formulate a plan from there!
		String viewName = statement.getViewName().toUpperCase();
		List<Sql> sequel = new ArrayList<Sql>();

		if ( materializedViewExists( viewName, database ) ) {
			DropMaterializedViewStatement dropMViewStmt = new DropMaterializedViewStatement( statement.getViewName() );
			dropMViewStmt.setSchemaName( database.getDefaultSchemaName() );
			DropMaterializedViewGenerator dropMViewGen = new DropMaterializedViewGenerator();
			sequel.addAll( Arrays.asList( dropMViewGen.generateSql( dropMViewStmt, database, null ) ) );
		} else if ( tableExists( viewName, database ) ) {
			DropTableStatement dropTableStmt = new DropTableStatement( database.getDefaultCatalogName(), database.getDefaultSchemaName(), viewName, true );
			DropTableGenerator dropTableGen = new DropTableGenerator();
			sequel.addAll( Arrays.asList( dropTableGen.generateSql( dropTableStmt, database, null ) ) );
		} // else if there is a view we will use "create or replace view"
		
		// if the view exists as a regular view already, we don't actually care.
		String selectQuery = addSchemaPrefixToFunctionNames( statement.getSelectQuery(), database );
		CreateViewStatement createViewStmt = new CreateViewStatement( database.getDefaultCatalogName(), database.getDefaultSchemaName(), statement.getViewName(), selectQuery, true );

		CreateViewGenerator createViewGen = new CreateViewGenerator();
		sequel.addAll( Arrays.asList( createViewGen.generateSql( createViewStmt, database, null ) ) );

		log.fine( sequel.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} }  ).collect( Collectors.joining( "\n" ) ) );

		return sequel.toArray( new Sql[0] );
	}
	
	private String addSchemaPrefixToFunctionNames( String sql, Database database ) {
		if ( database instanceof MSSQLDatabase && database.getConnection() instanceof JdbcConnection ) {
			try {
				Statement statement = ( (JdbcConnection) database.getConnection() ).createStatement();
				ResultSet resultSet = statement.executeQuery( "SELECT NAME, SCHEMA_NAME(SCHEMA_ID), "
						+ "(SELECT MAX(PARAMETER_ID) FROM SYS.PARAMETERS P WHERE P.OBJECT_ID = O.OBJECT_ID) "
						+ "FROM SYS.OBJECTS O WHERE TYPE = 'FN'" );
				List parsedSql = Arrays.asList( SqlParser.parse( sql, true, true ).toArray( true ) );
				Map<Integer, List<String>> schemaMap = new HashMap<>();
				while ( resultSet.next() ) {
					String functionName = resultSet.getString( 1 );
					String schemaName = resultSet.getString( 2 );
					int parameterCount = resultSet.getInt( 3 );
					for ( int i = 0; i < parsedSql.size() - 2; i++ ) {
						if ( parsedSql.get( i ).toString().equalsIgnoreCase( functionName ) ) {
							int b = 0;
							int p = -1;
							for ( int j = i + 1; j < parsedSql.size(); j++ ) {
								Object next = parsedSql.get( j );
								if ( next.toString().trim().equals( "" ) ) {
									continue;
								} else if ( next.equals( "(" ) ) {
									b++;
								} else if ( next.equals( ")" ) ) {
									b--;
								} else if ( next.equals( "," ) && b == 1 ) {
									p++;
								}
								if ( b < 1 ) {
									break;
								} else if ( p < 1 ) {
									p++;
								}
							}
							if ( p == parameterCount ) {
								if ( !schemaMap.containsKey( i ) ) {
									schemaMap.put( i, new ArrayList<>() );
								}
								schemaMap.get( i ).add( schemaName );
							}
						}
					}
				}
				for ( Entry<Integer, List<String>> entry : schemaMap.entrySet() ) {
					int i = entry.getKey();
					List<String> schemaList = entry.getValue();
					String functionName = parsedSql.get( i ).toString();
					String schemaName = schemaList.get( 0 );
					if ( schemaList.size() > 1 ) {
						for ( String s : schemaList ) {
							if ( s.equalsIgnoreCase( "opentwins" ) ) {
								schemaName = s;
								break;
							} else if ( s.equalsIgnoreCase( database.getDefaultSchemaName() ) ) {
								schemaName = s;
							}
						}
					}
					parsedSql.set( i, "[" + schemaName + "].[" + functionName + "]" );
				}
				return String.join( "", parsedSql );
			} catch ( DatabaseException | SQLException e ) {
				throw new RuntimeException( e );
			}
		}
		return sql;
	}
}
