package liquibase.ext.otbo.changes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Scope;
import liquibase.change.Change;
import liquibase.change.ChangeFactory;
import liquibase.change.ChangeMetaData;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.DatabaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.otbo.preconditions.OtboMaterializedViewExistsPrecondition;
import liquibase.ext.otbo.test.BaseTestCase;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.precondition.core.TableExistsPrecondition;
import liquibase.precondition.core.ViewExistsPrecondition;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;

public class CreateFlexibleViewTest extends BaseTestCase {

	@Before
	public void setUp() throws Exception {
		changeLogFile = "liquibase/ext/otbo/changes/changelog.test.xml";
		connectToDB();
		cleanDB();
	}

	@Test
	public void getChangeMetaData() {
		CreateFlexibleViewChange view = new CreateFlexibleViewChange();

		ChangeFactory changeFactory = Scope.getCurrentScope().getSingleton( ChangeFactory.class );
		assertEquals( "createFlexibleView", changeFactory.getChangeMetaData( view ).getName() );
		assertEquals( "Create a new database view or materialized view depending on context.", changeFactory.getChangeMetaData( view ).getDescription() );
		assertEquals( ChangeMetaData.PRIORITY_DEFAULT, changeFactory.getChangeMetaData( view ).getPriority() );
	}

	@Test
	public void getConfirmationMessage() {
		final String VIEW_NAME = "myview";
		CreateFlexibleViewChange view = new CreateFlexibleViewChange();
		view.setViewName( VIEW_NAME );

		assertEquals( String.format( "Flexible View %s created", VIEW_NAME ), String.format( "Flexible View %s created", view.getViewName() ) );
		assertEquals( String.format( "Flexible View %s created", VIEW_NAME ), view.getConfirmationMessage() );
	}

	@Test
	public void generateStatement() {
		CreateFlexibleViewChange view = new CreateFlexibleViewChange();
		view.setViewName( "myview" );
		view.setSelectQuery( "select * from mytable" );

		SqlStatement[] sqlStatements = view.generateStatements( liquiBase.getDatabase() );
		assertEquals( 1, sqlStatements.length );
		assertTrue( sqlStatements[0] instanceof CreateFlexibleViewStatement );

		CreateFlexibleViewStatement stmt = (CreateFlexibleViewStatement) sqlStatements[0];
		assertEquals( "myview", stmt.getViewName() );
		assertEquals( "select * from mytable", stmt.getSelectQuery() );
	}

	@Test
	public void parseAndGenerate() throws Exception {
		Database database = liquiBase.getDatabase();
		ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

		ChangeLogParameters changeLogParameters = new ChangeLogParameters();

		DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance().getParser( changeLogFile, resourceAccessor ).parse( changeLogFile, changeLogParameters, resourceAccessor );
		liquiBase.checkLiquibaseTables( true, changeLog, new Contexts(), new LabelExpression() );
		changeLog.validate( database );

		List<ChangeSet> changeSets = changeLog.getChangeSets();

		List<String> expectedQuery = new ArrayList<String>();

		if ( database instanceof OracleDatabase ) {
			expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview1 AS select myfunc0(one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview2 AS select myfunc1(one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview2 AS select myfunc2(one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview4 AS select myfunc0(one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview3 AS select myfunc1(one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview3 AS select myfunc2(one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getDefaultSchemaName() ) );
		} else if ( database instanceof MSSQLDatabase ) {
			expectedQuery.add( String.format( "IF NOT EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[%s].[myview1]'))", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "ALTER VIEW myview1 AS select [%s].[myfunc0](one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "IF NOT EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[%s].[myview2]'))", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "ALTER VIEW myview2 AS select [%s].[myfunc1](one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "IF NOT EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[%s].[myview2]'))", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "ALTER VIEW myview2 AS select [%s].[myfunc2](one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "IF NOT EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[%s].[myview4]'))", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "ALTER VIEW myview4 AS select [%s].[myfunc0](one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "IF NOT EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[%s].[myview3]'))", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "ALTER VIEW myview3 AS select [%s].[myfunc1](one, two) as result from mytable", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "IF NOT EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[%s].[myview3]'))", database.getDefaultSchemaName() ) );
			expectedQuery.add( String.format( "ALTER VIEW myview3 AS select [%s].[myfunc2](one, two) as result from mytable", database.getDefaultSchemaName() ) );
		}

		int i = 0;
		for ( ChangeSet changeSet : changeSets ) {
			for ( Change change : changeSet.getChanges() ) {
				Sql[] sql = SqlGeneratorFactory.getInstance().generateSql( change.generateStatements( database )[0], database );
				
				if ( change instanceof CreateFlexibleViewChange ) {
					for ( Sql s : sql ) {
						System.out.println( "--------------------------------------------------------------------------------------------------------" );
						System.out.println( "[" + i + "] ACTUAL: " + s.toSql().split("\n")[0] );
						System.out.println( "[" + i + "] EXPECT: " + expectedQuery.get( i ) );
						assertEquals( expectedQuery.get( i ), s.toSql().split("\n")[0] );
						i++;
					}
				}
			}
		}
	}

	@Test
	public void test() throws Exception {
		update();
		
		/**
		 * Check the database.
		 * We expect:
		 * 1. MYVIEW1 to have been dropped.
		 * 2. MYVIEW2 to exist as a materialized view.
		 * 3. MYVIEW3 to exist as a real-time view.
		 * 4. MYVIEW4 to have been dropped.
		 */
		
		assertFalse( viewExists( "MYVIEW1" ) );
		assertFalse( mviewExists( "MYVIEW1" ) );
		
		assertFalse( viewExists( "MYVIEW2" ) );
		assertTrue( mviewExists( "MYVIEW2" ) );
		assertEquals( "second", getResult( "MYVIEW2" ) );
		
		assertTrue( viewExists( "MYVIEW3" ) );
		assertFalse( mviewExists( "MYVIEW3" ) );
		assertEquals( "second", getResult( "MYVIEW3" ) );
		
		assertFalse( viewExists("MYVIEW4") );
		assertFalse( mviewExists("MYVIEW4") );
	}

	private boolean viewExists( String viewName ) {
		try {
			ViewExistsPrecondition viewExists = new ViewExistsPrecondition();
			viewExists.setViewName( viewName );
			viewExists.check( liquiBase.getDatabase(), null, null, null );
			return true;
		} catch ( PreconditionFailedException e ) {
			return false;
		} catch ( PreconditionErrorException e ) {
			throw new RuntimeException( e );
		}
	}

	private boolean mviewExists( String mviewName ) {
		if ( liquiBase.getDatabase() instanceof OracleDatabase ) {
			OtboMaterializedViewExistsPrecondition mviewExists = new OtboMaterializedViewExistsPrecondition();
			mviewExists.setViewName( mviewName );
			return mviewExists.check( liquiBase.getDatabase() );
		}
		return tableExists(mviewName);
	}

	private boolean tableExists(String tableName) {
		try {
			TableExistsPrecondition tableExists = new TableExistsPrecondition();
			tableExists.setTableName( tableName );
			tableExists.check( liquiBase.getDatabase(), null, null, null );
			return true;
		} catch ( PreconditionFailedException e ) {
			return false;
		} catch ( PreconditionErrorException e ) {
			throw new RuntimeException( e );
		}
	}

	private String getResult( String tableName ) {
		try {
			Executor executor = Scope.getCurrentScope().getSingleton( ExecutorService.class ).getExecutor( "jdbc", liquiBase.getDatabase() );
			return executor.queryForObject( new RawSqlStatement( "select result from " + tableName ), String.class );
		} catch ( DatabaseException e ) {
			throw new RuntimeException( e );
		}
	}
}
