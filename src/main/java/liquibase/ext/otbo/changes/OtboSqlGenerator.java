package liquibase.ext.otbo.changes;

import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.ext.otbo.preconditions.OtboMaterializedViewExistsPrecondition;
import liquibase.precondition.core.TableExistsPrecondition;
import liquibase.precondition.core.ViewExistsPrecondition;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.SqlStatement;

public abstract class OtboSqlGenerator<T extends SqlStatement> extends AbstractSqlGenerator<T> {
	protected boolean viewExists( String viewName, Database database ) {
		try {
			ViewExistsPrecondition viewCheck = new ViewExistsPrecondition();
			viewCheck.setViewName( viewName );
			viewCheck.check( database, null, null, null );
			return true;
		} catch ( PreconditionFailedException e ) {
			// ignore
		} catch ( PreconditionErrorException e ) {
			throw new RuntimeException( e );
		}
		return false;
	}

	protected boolean materializedViewExists( String viewName, Database database ) {
		if ( database instanceof OracleDatabase ) {
			OtboMaterializedViewExistsPrecondition mviewCheck = new OtboMaterializedViewExistsPrecondition();
			mviewCheck.setViewName( viewName );
			return mviewCheck.check( database );
		}
		return false;
	}

	protected boolean tableExists( String viewName, Database database ) {
		try {
			TableExistsPrecondition tableExistsCondition = new TableExistsPrecondition();
			tableExistsCondition.setTableName( viewName );
			tableExistsCondition.check( database, null, null, null );
			return true;
		} catch ( PreconditionFailedException e ) {
			// ignore
		} catch ( PreconditionErrorException e ) {
			throw new RuntimeException( e );
		}
		return false;
	}
}
