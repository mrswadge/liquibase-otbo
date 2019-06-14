package liquibase.ext.otbo.preconditions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.precondition.AbstractPrecondition;
import liquibase.precondition.Precondition;
import liquibase.resource.ResourceAccessor;

public class OtboMaterializedViewExistsPrecondition extends OtboPrecondition<Precondition> {

	private String viewName;

	public String getViewName() {
		return viewName;
	}

	public void setViewName( String viewName ) {
		this.viewName = viewName;
	}

	public String getName() {
		return "otboMaterializedViewExists";
	}

	@Override
	protected Precondition fallback( Database database ) {
		// create a proxy instance so we have a return value to this method.
		return (Precondition) Proxy.newProxyInstance( this.getClass().getClassLoader(), new Class[] { Precondition.class }, new InvocationHandler() {
			public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
				throw new AbstractMethodError( "Fallback precondition for " + getName() +  " is not implemented." );
			}
		} );
	}
	
	public Warnings warn( Database database ) {
		// No redirect required.
		return new Warnings();
	}

	public ValidationErrors validate( Database database ) {
		// No redirect required.
		return new ValidationErrors();
	}

	public void check( Database database, DatabaseChangeLog changeLog, ChangeSet changeSet, ChangeExecListener changeExecListener ) throws PreconditionFailedException, PreconditionErrorException {
		if ( ! check( database ) ) {
			throw new PreconditionFailedException( String.format( "The view '%s.%s' was not found.", database.getLiquibaseSchemaName(), getViewName() ), changeLog, this );
		}
	}
	
	public boolean check( Database database ) {
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			JdbcConnection connection = (JdbcConnection) database.getConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				final String sql = "select count(*) from all_mviews where upper(mview_name) = upper(?) and upper(owner) = upper(?)";
				ps = connection.prepareStatement( sql );
				ps.setString( 1, getViewName() );
				ps.setString( 2, database.getLiquibaseSchemaName() );
				rs = ps.executeQuery();
				if ( !rs.next() || rs.getInt( 1 ) <= 0 ) {
					return false;
				} else {
					return true;
				}
			} catch ( SQLException e ) {
				throw new UnexpectedLiquibaseException( "Failed on database view investigation.", e );
			} catch ( DatabaseException e ) {
				throw new UnexpectedLiquibaseException( "Failed on database view investigation.", e );
			} finally {
				closeSilently( rs );
				closeSilently( ps );
			}
		} else {
			// assume that it does not exist.
			return false;
		}
	}

	@Override
	public void load( ParsedNode parsedNode, ResourceAccessor resourceAccessor ) throws ParsedNodeException {
		super.load( parsedNode, resourceAccessor );
    this.viewName = parsedNode.getChildValue(null, "viewName", String.class);
	}
}