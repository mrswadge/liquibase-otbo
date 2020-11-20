package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewGenerator;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.ext.otbo.preconditions.OtboMaterializedViewExistsPrecondition;
import liquibase.logging.LogService;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.CreateViewGenerator;
import liquibase.sqlgenerator.core.DropColumnGenerator;
import liquibase.sqlgenerator.core.DropDefaultValueGenerator;
import liquibase.statement.core.CreateViewStatement;
import liquibase.statement.core.DropColumnStatement;
import liquibase.statement.core.DropDefaultValueStatement;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;

public class DropColumnCascadeGenerator extends OtboSqlGenerator<DropColumnCascadeStatement> {

	private static final Logger log = LogService.getLog( DropColumnCascadeGenerator.class );

	@Override
	public boolean supports( DropColumnCascadeStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}

	public ValidationErrors validate( DropColumnCascadeStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "tableName", statement.getTableName() );
		validationErrors.checkRequiredField( "columnName", statement.getColumnName() );
		return validationErrors;
	}

	public Sql[] generateSql( DropColumnCascadeStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		// check if the view already exists, then formulate a plan from there!
		String catalogName = statement.getCatalogName();
		String schemaName = statement.getSchemaName();
		String tableName = statement.getTableName();
		String columnName = statement.getColumnName();
		
		List<Sql> sequel = new ArrayList<Sql>();

		if ( database instanceof MSSQLDatabase ) {
			String escapedTableName = database.escapeTableName( catalogName, schemaName, tableName );

			// drop default value constraints
			DropDefaultValueStatement dropDefaultValueStmt = new DropDefaultValueStatement( catalogName, schemaName, tableName, columnName, null );
			DropDefaultValueGenerator dropDefaultValueGen = new DropDefaultValueGenerator();
			sequel.addAll( Arrays.asList( dropDefaultValueGen.generateSql( dropDefaultValueStmt, database, null ) ) );
			
			// drop indexes
			String sql = "DECLARE @sql [nvarchar](MAX) = STUFF(( " +
				"	SELECT N'; DROP INDEX ' + QUOTENAME([ind].[name]) + ' ON ' + QUOTENAME([t].[name]) " +
				"	FROM [sys].[indexes] [ind] " +
				"		INNER JOIN [sys].[index_columns] [ic] ON [ind].[object_id] = [ic].[object_id] and [ind].[index_id] = [ic].[index_id] " + 
				"		INNER JOIN [sys].[columns] [col] ON [ic].[object_id] = [col].[object_id] and [ic].[column_id] = [col].[column_id] " +
				"		INNER JOIN [sys].[tables] [t] ON [ind].[object_id] = [t].[object_id] " +
				"	WHERE " +
				"		[t].[is_ms_shipped] = 0 " + 
				"		AND [t].[name] = N'" + database.escapeStringForDatabase(escapedTableName) + "' " +
				"		AND [col].[name] = N'" + database.escapeStringForDatabase(statement.getColumnName()) + "' " +
				"		AND [ind].[is_primary_key] = 0 " +
				"		FOR XML PATH('') " +
				"), 1, 1, '' ) \r\n" +
				"EXEC sp_executesql @sql";
			sequel.add( new UnparsedSql(sql, getAffectedColumn( statement ) ) );
		} 

		DropColumnStatement dropColumnStmt = new DropColumnStatement( catalogName, schemaName, tableName, columnName );
		DropColumnGenerator dropColumnGen = new DropColumnGenerator();
		sequel.addAll( Arrays.asList( dropColumnGen.generateSql( dropColumnStmt, database, null ) ) );

		log.debug( sequel.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} } ).collect( Collectors.joining( "\n" ) ) );

		return sequel.toArray( new Sql[0] );
	}

	protected Column getAffectedColumn( DropColumnCascadeStatement statement ) {
		return new Column().setName( statement.getColumnName() ).setRelation( new Table().setName( statement.getTableName() ).setSchema( statement.getCatalogName(), statement.getSchemaName() ) );
	}

}
