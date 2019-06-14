//package liquibase.ext.otbo.changes;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import liquibase.database.Database;
//import liquibase.database.core.MSSQLDatabase;
//import liquibase.database.core.OracleDatabase;
//import liquibase.logging.LogService;
//import liquibase.logging.Logger;
//import liquibase.sql.Sql;
//import liquibase.sqlgenerator.SqlGeneratorChain;
//import liquibase.sqlgenerator.core.AddDefaultValueGenerator;
//import liquibase.sqlgenerator.core.DropDefaultValueGenerator;
//import liquibase.statement.core.AddDefaultValueStatement;
//import liquibase.statement.core.DropDefaultValueStatement;
//import liquibase.structure.core.Column;
//import liquibase.structure.core.Table;
//
//public class AddOrReplaceDefaultValueGenerator extends AddDefaultValueGenerator {
//
//	private static final Logger log = LogService.getLog( AddOrReplaceDefaultValueGenerator.class );
//
//	@Override
//	public boolean supports( AddDefaultValueStatement statement, Database database ) {
//		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
//	}
//
//	@Override
//	public Sql[] generateSql( AddDefaultValueStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
//		// check if the view already exists, then formulate a plan from there!
//		String catalogName = statement.getCatalogName();
//		String schemaName = statement.getSchemaName();
//		String tableName = statement.getTableName();
//		String columnName = statement.getColumnName();
//		
//		List<Sql> sequel = new ArrayList<Sql>();
//
//		// drop it like it's hot.
//		if ( database instanceof MSSQLDatabase ) {
//			log.info( "*** DATABASE DETECTED AS MSSQL ***" );
//			// drop default value constraints
//			DropDefaultValueStatement dropDefaultValueStmt = new DropDefaultValueStatement( catalogName, schemaName, tableName, columnName, null );
//			DropDefaultValueGenerator dropDefaultValueGen = new DropDefaultValueGenerator();
//			sequel.addAll( Arrays.asList( dropDefaultValueGen.generateSql( dropDefaultValueStmt, database, null ) ) );
//		} else {
//			log.info( "*** DATABASE DETECTED AS SOME OTHER SHIT ***" );
//		}
//
//		sequel.addAll( Arrays.asList( super.generateSql( statement, database, sqlGeneratorChain ) ) );
//
//		log.info( sequel.stream().map( new java.util.function.Function<Object, String>() {
//			public String apply( Object o ) {
//				return String.valueOf( o );
//			} } ).collect( Collectors.joining( "\n" ) ) );
//
//		return sequel.toArray( new Sql[0] );
//	}
//
//	protected Column getAffectedColumn( DropColumnCascadeStatement statement ) {
//		return new Column().setName( statement.getColumnName() ).setRelation( new Table().setName( statement.getTableName() ).setSchema( statement.getCatalogName(), statement.getSchemaName() ) );
//	}
//
//}
