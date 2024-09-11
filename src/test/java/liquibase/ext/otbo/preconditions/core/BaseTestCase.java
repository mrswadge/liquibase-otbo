package liquibase.ext.otbo.preconditions.core;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.util.Properties;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.resource.ClassLoaderResourceAccessor;

/*
 * Class used by tests to set up connection and clean database.
 */
public class BaseTestCase {

	private static String url;
	private static Driver driver;
	private static Properties info;
	protected static Connection connection;
	protected static JdbcConnection jdbcConnection;
	protected static Liquibase liquiBase;
	protected static String changeLogFile;

	public static void connectToDB() throws Exception {
		if ( connection == null ) {
			info = new Properties();
			info.load( new FileInputStream( "src/test/resources/tests.properties" ) );

			url = info.getProperty( "url" );
			driver = (Driver) Class.forName( DatabaseFactory.getInstance().findDefaultDriver( url ), true, Thread.currentThread().getContextClassLoader() ).newInstance();

			connection = driver.connect( url, info );

			if ( connection == null ) {
				throw new DatabaseException( "Connection could not be created to " + url + " with driver " + driver.getClass().getName() + ".  Possibly the wrong driver for the given database URL" );
			}

			jdbcConnection = new JdbcConnection( connection );
		}
	}

	public static void cleanDB() throws Exception {
		liquiBase = new Liquibase( changeLogFile, new ClassLoaderResourceAccessor(), jdbcConnection );
		liquiBase.dropAll();
		
		Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor( "jdbc", liquiBase.getDatabase() );
		ResultSet rs = null;
		try {
			rs = jdbcConnection.getMetaData().getTables( null, liquiBase.getDatabase().getDefaultSchemaName(), "%", new String[] { "MATERIALIZED VIEW" } );
			while ( rs.next() ) {
				executor.execute( new DropMaterializedViewStatement( rs.getString( "TABLE_NAME" ) ) );
			}
		} finally {
			closeSilently( rs );
		}
	}
	
	public static void closeSilently( ResultSet ... resultSets ) {
		for ( ResultSet rs : resultSets ) {
			try { if ( rs != null ) rs.close(); } catch ( Throwable t ) { } 
		}
	}
}
