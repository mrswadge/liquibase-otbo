package liquibase.ext.otbo.preconditions;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import liquibase.Scope;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.otbo.test.BaseTestCase;
import liquibase.precondition.Precondition;
import liquibase.precondition.PreconditionFactory;
import liquibase.statement.core.RawSqlStatement;
import org.junit.Before;
import org.junit.Test;

public class OraclePreconditionsTest extends BaseTestCase {

	@Before
	public void setUp() throws Exception {
		changeLogFile = "liquibase/ext/otbo/preconditions/changelog.test.xml";
		connectToDB();
		cleanDB();
		update();
	}

	@Test
	public void test() throws Exception {
		final List<String> expected = Arrays.asList( 
				"iftableexists", "iftablenotexists", "ifviewexists", "ifviewnotexists", 
				"ifindexexists1", "ifindexexists2", "ifindexnotexists1", "ifpkexists",
				"ifpknotexists", "iffkexists", "iffknotexists", "ifsequenceexists",
				"ifsequencenotexists"
		);
		Executor executor = Scope.getCurrentScope().getSingleton( ExecutorService.class ).getExecutor( "jdbc", liquiBase.getDatabase() );
		List<String> successes = executor.queryForList( new RawSqlStatement( "select * from testresults" ), String.class );
		assertTrue( successes.containsAll( expected ) );
	}

	@Test
	public void testRegistry() throws Exception {
		Map<String, Class<? extends Precondition>> preconditions = PreconditionFactory.getInstance().getPreconditions();
		checkRegistry( preconditions, OtboCheckConstraintExistsPrecondition.class );
		checkRegistry( preconditions, OtboColumnExistsPrecondition.class );
		checkRegistry( preconditions, OtboForeignKeyExistsPrecondition.class );
		checkRegistry( preconditions, OtboIndexExistsPrecondition.class );
		checkRegistry( preconditions, OtboMaterializedViewExistsPrecondition.class );
		checkRegistry( preconditions, OtboPrimaryKeyExistsPrecondition.class );
		checkRegistry( preconditions, OtboSequenceExistsPrecondition.class );
		checkRegistry( preconditions, OtboTableExistsPrecondition.class );
		checkRegistry( preconditions, OtboUniqueConstraintExistsPrecondition.class );
		checkRegistry( preconditions, OtboViewExistsPrecondition.class );
	}

	private void checkRegistry( Map<String, Class<? extends Precondition>> registry, Class<? extends Precondition> clazz ) throws Exception {
		Precondition precondition = clazz.getConstructor().newInstance();
		String name = precondition.getName();
		Class<? extends Precondition> mappedClazz = registry.get( name );
		assertEquals( clazz, mappedClazz );
	}
	
}
