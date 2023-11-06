package liquibase.ext.otbo.changes;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.RawSqlGenerator;
import liquibase.statement.core.RawSqlStatement;

public class ConvertViewsIntoMaterializedViewsGenerator extends AbstractSqlGenerator<ConvertViewsIntoMaterializedViewsStatement> {
	
	private static final Logger log = Scope.getCurrentScope().getLog(ConvertViewsIntoMaterializedViewsGenerator.class);
	
	@Override
	public boolean supports( ConvertViewsIntoMaterializedViewsStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}

	public ValidationErrors validate( ConvertViewsIntoMaterializedViewsStatement statement, Database database, SqlGeneratorChain<ConvertViewsIntoMaterializedViewsStatement> sqlGeneratorChain ) {
		return new ValidationErrors();
	}

	public Sql[] generateSql( ConvertViewsIntoMaterializedViewsStatement statement, Database database, SqlGeneratorChain<ConvertViewsIntoMaterializedViewsStatement> sqlGeneratorChain ) {
		String sql; 

		List<Sql> sqlList = new ArrayList<Sql>();
		RawSqlGenerator rawSqlGen = new RawSqlGenerator();
		
		if ( database instanceof OracleDatabase ) {
			sql = readSqlFile( "liquibase/ext/otbo/changes/createViewConversionExcludesTable.sql" );
			sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );
			
			sql = readSqlFile( "liquibase/ext/otbo/changes/createViewDependencyGraphTempTable.sql" );
			sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );
	
			sql = readSqlFile( "liquibase/ext/otbo/changes/convertViewsToMaterializedViews.sql" );
			sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );
			
			sql = readSqlFile( "liquibase/ext/otbo/changes/dropViewDependencyGraphTempTable.sql" );
			sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );
		} else if ( database instanceof MSSQLDatabase ) {
			sql = readSqlFile( "liquibase/ext/otbo/changes/convertViewsToMaterializedTables.sql" );
			sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );
		} else {
			throw new UnexpectedLiquibaseException( "Database is not supported." );
		}

		log.fine( sqlList.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} }  ).collect( Collectors.joining( "\n" ) ) );
		
		return sqlList.toArray( new Sql[0] );
	}
	
	private String readSqlFile( String location ) {
		ClassLoader loader = ConvertViewsIntoMaterializedViewsGenerator.class.getClassLoader();
		InputStream in = loader.getResourceAsStream( location );
		StringWriter sw = new StringWriter();
		Scanner s = new Scanner( in ).useDelimiter( "\\A" );
		String sql;
		if ( s.hasNext() ) {
			sql = s.next();
		} else {
			throw new UnexpectedLiquibaseException( "Could not locate the view conversion SQL." );
		}
		return sql;
	}
}
