package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewGenerator;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.DropTableGenerator;
import liquibase.sqlgenerator.core.DropViewGenerator;
import liquibase.statement.core.DropTableStatement;
import liquibase.statement.core.DropViewStatement;

public class DropFlexibleViewGenerator extends OtboSqlGenerator<DropFlexibleViewStatement> {
	
	private static final Logger log = Scope.getCurrentScope().getLog(DropFlexibleViewGenerator.class);
	
	@Override
	public boolean supports( DropFlexibleViewStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}

	public ValidationErrors validate( DropFlexibleViewStatement statement, Database database, SqlGeneratorChain<DropFlexibleViewStatement> sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "viewName", statement.getViewName() );
		return validationErrors;
	}

	/**
	 * Three types of views are built.
	 * 
	 * Dynamic Views - traditional views which update in realtime using the tables to which they refer.
	 * Materialized Views - Oracle only. A snapshot in time of a view that is essentially persisted into a table.
	 * Table Views - MSSQL only. A snapshot in time of a view that is selected into a table.
	 */
	public Sql[] generateSql( DropFlexibleViewStatement statement, Database database, SqlGeneratorChain<DropFlexibleViewStatement> sqlGeneratorChain ) {
		JdbcConnection connection = (JdbcConnection) database.getConnection();

		String viewName = statement.getViewName();
		List<Sql> sequel = new ArrayList<Sql>();
		
		if ( materializedViewExists( viewName, database ) ) {
			DropMaterializedViewStatement dropMViewStmt = new DropMaterializedViewStatement( viewName );
			dropMViewStmt.setSchemaName( database.getDefaultSchemaName() );
			DropMaterializedViewGenerator dropMViewGen = new DropMaterializedViewGenerator();
			sequel.addAll( Arrays.asList( dropMViewGen.generateSql( dropMViewStmt, database, null ) ) );
		} else if ( database instanceof MSSQLDatabase && tableExists( viewName, database ) ) {
			DropTableStatement dropTableStmt = new DropTableStatement( database.getDefaultCatalogName(), database.getDefaultSchemaName(), viewName, true );
			DropTableGenerator dropTableGen = new DropTableGenerator();
			sequel.addAll( Arrays.asList( dropTableGen.generateSql( dropTableStmt, database, null ) ) );
		} else if ( viewExists( viewName, database ) ) {
			DropViewStatement dropViewStmt = new DropViewStatement( database.getDefaultCatalogName(), database.getDefaultSchemaName(), viewName );
			DropViewGenerator dropViewGen = new DropViewGenerator();
			sequel.addAll( Arrays.asList( dropViewGen.generateSql( dropViewStmt, database, null ) ) );
		} else {
			log.warning( String.format( "The [materialized] view named %s was not found when it was attempted to be dropped from the database.", viewName ) );
		}

		log.fine( sequel.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} }  ).collect( Collectors.joining( "\n" ) ) );
		
		return sequel.toArray( new Sql[0] );
	}
}
