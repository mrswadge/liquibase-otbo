package liquibase.ext.otbo.changes;

import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Before;
import org.junit.Test;

import liquibase.ext.otbo.test.BaseTestCase;

/**
 * Copied from the Oracle Create Materialized View addon, although I'm not entirely sure of the aim
 * of this test.
 * @author sbs
 *
 */
public class CreateFlexibleViewDBTest extends BaseTestCase {

	private IDataSet loadedDataSet;
	private final String TABLE_NAME = "mytabledb";

	@Before
	public void setUp() throws Exception {
		changeLogFile = "liquibase/ext/otbo/changes/changelog.test.xml";
		connectToDB();
		cleanDB();
	}

	protected IDatabaseConnection getConnection() throws Exception {
		return new DatabaseConnection( connection );
	}

	protected IDataSet getDataSet() throws Exception {
		loadedDataSet = new FlatXmlDataSetBuilder().build( this.getClass().getClassLoader().getResourceAsStream( "liquibase/ext/otbo/changes/input.xml" ) );
		return loadedDataSet;
	}

	@Test
	public void testCompare() throws Exception {
		QueryDataSet actualDataSet = new QueryDataSet( getConnection() );

		update();
		actualDataSet.addTable( TABLE_NAME, "SELECT * from " + TABLE_NAME );
		loadedDataSet = getDataSet();

		Assertion.assertEquals( loadedDataSet, actualDataSet );
	}

}
