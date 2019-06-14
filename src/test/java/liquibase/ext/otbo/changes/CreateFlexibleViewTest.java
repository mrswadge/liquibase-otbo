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
import liquibase.change.Change;
import liquibase.change.ChangeFactory;
import liquibase.change.ChangeMetaData;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.ext.otbo.changes.CreateFlexibleViewChange;
import liquibase.ext.otbo.changes.testcore.BaseTestCase;
import liquibase.ext.otbo.preconditions.OtboMaterializedViewExistsPrecondition;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.precondition.core.ViewExistsPrecondition;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

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

		ChangeFactory changeFactory = ChangeFactory.getInstance(); // 3.7 onwards: liquibase.Scope.getCurrentScope().getSingleton(ChangeFactory.class);
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
		view.setSelectQuery( "select 1 from dual" );

		SqlStatement[] sqlStatements = view.generateStatements( new OracleDatabase() );
		for ( SqlStatement sqlStatement : sqlStatements ) {
			
		}
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

		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview1 AS select 1 as One from dual", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview2 AS select * from mytable", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview2 AS select * from mytable where rownum = 1", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview4 AS select 1 as One from dual", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview3 AS select * from mytable", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview3 AS select * from mytable where one like '%%'", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getLiquibaseSchemaName() ) );

		int i = 0;
		for ( ChangeSet changeSet : changeSets ) {
			for ( Change change : changeSet.getChanges() ) {
				Sql[] sql = SqlGeneratorFactory.getInstance().generateSql( change.generateStatements( database )[0], database );
				
				if ( change instanceof CreateFlexibleViewChange ) {
					for ( Sql s : sql ) {
						System.out.println( "--------------------------------------------------------------------------------------------------------" );
						System.out.println( "[" + i + "] ACTUAL: " + s.toSql() );
						System.out.println( "[" + i + "] EXPECT: " + expectedQuery.get( i ) );
						assertEquals( expectedQuery.get( i ), s.toSql() );
						i++;
					}
				}
			}
		}
	}

	@Test
	public void test() throws Exception {
		liquiBase.update( new Contexts() );
		
		/**
		 * Check the database.
		 * We expect:
		 * 1. MYVIEW1 to have been dropped.
		 * 2. MYVIEW2 to exist as a materialized view.
		 * 3. MYVIEW3 to exist as a real-time view.
		 */
		
		assertFalse( viewExists( "MYVIEW1" ) );
		assertFalse( mviewExists( "MYVIEW1" ) );
		
		assertFalse( viewExists( "MYVIEW2" ) );
		assertTrue( mviewExists( "MYVIEW2" ) );
		
		assertTrue( viewExists( "MYVIEW3" ) );
		assertFalse( mviewExists( "MYVIEW3" ) );
		
		assertFalse( viewExists("MYVIEW4") );
		assertFalse( mviewExists("MYVIEW4") );
	}

	private boolean mviewExists(String mviewName) {
		OtboMaterializedViewExistsPrecondition mviewExists = new OtboMaterializedViewExistsPrecondition();
		mviewExists.setViewName( mviewName );
		return mviewExists.check( liquiBase.getDatabase() );
	}

	private boolean viewExists(String mviewName) {
		try {
			ViewExistsPrecondition viewExists = new ViewExistsPrecondition();
			viewExists.setViewName( mviewName );
			viewExists.check( liquiBase.getDatabase(), null, null, null );
			return true;
		} catch ( PreconditionFailedException e ) {
			// e.printStackTrace();
			return false;
		} catch ( PreconditionErrorException e ) {
			throw new RuntimeException( e );
		}
	}
}
