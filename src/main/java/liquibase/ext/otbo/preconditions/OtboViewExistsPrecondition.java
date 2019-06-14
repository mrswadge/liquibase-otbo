package liquibase.ext.otbo.preconditions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.precondition.Precondition;
import liquibase.precondition.core.ViewExistsPrecondition;
import liquibase.resource.ResourceAccessor;

public class OtboViewExistsPrecondition extends OtboPrecondition<ViewExistsPrecondition> {

	private String viewName;

	public String getViewName() {
		return viewName;
	}

	public void setViewName( String viewName ) {
		this.viewName = viewName;
	}

	public String getName() {
		return "otboViewExists";
	}

	public Warnings warn( Database database ) {
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			return new Warnings();
		} else {
			return redirect.warn( database );
		}
	}

	public ValidationErrors validate( Database database ) {
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			return new ValidationErrors();
		} else {
			return redirect.validate( database );
		}
	}

	@Override
	protected ViewExistsPrecondition fallback( Database database ) {
		ViewExistsPrecondition fallback = new ViewExistsPrecondition();
		fallback.setCatalogName( database.getLiquibaseCatalogName() );
		fallback.setSchemaName( database.getLiquibaseSchemaName() );
		fallback.setViewName( getViewName() );
		return fallback;
	}
	
	public void check( Database database, DatabaseChangeLog changeLog, ChangeSet changeSet, ChangeExecListener changeExecListener ) throws PreconditionFailedException, PreconditionErrorException {
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			JdbcConnection connection = (JdbcConnection) database.getConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				final String sql = "select count(*) from all_views where upper(view_name) = upper(?) and upper(owner) = upper(?)";
				ps = connection.prepareStatement( sql );
				ps.setString( 1, getViewName() );
				ps.setString( 2, database.getLiquibaseSchemaName() );
				rs = ps.executeQuery();
				if ( !rs.next() || rs.getInt( 1 ) <= 0 ) {
					throw new PreconditionFailedException( String.format( "The view '%s.%s' was not found.", database.getLiquibaseSchemaName(), getViewName() ), changeLog, this );
				}
			} catch ( SQLException e ) {
				throw new PreconditionErrorException( e, changeLog, this );
			} catch ( DatabaseException e ) {
				throw new PreconditionErrorException( e, changeLog, this );
			} finally {
				closeSilently( rs );
				closeSilently( ps );
			}
		} else {
			redirect.check( database, changeLog, changeSet, changeExecListener );
		}
	}

	@Override
	public void load( ParsedNode parsedNode, ResourceAccessor resourceAccessor ) throws ParsedNodeException {
		super.load( parsedNode, resourceAccessor );
    this.viewName = parsedNode.getChildValue(null, "viewName", String.class);
	}
}