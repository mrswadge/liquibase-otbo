package liquibase.ext.otbo.preconditions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.precondition.Precondition;
import liquibase.precondition.core.SequenceExistsPrecondition;
import liquibase.resource.ResourceAccessor;

public class OtboSequenceExistsPrecondition extends OtboPrecondition<SequenceExistsPrecondition> {

	private String sequenceName;

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName( String sequenceName ) {
		this.sequenceName = sequenceName;
	}

	public String getName() {
		return "otboSequenceExists";
	}

	@Override
	protected SequenceExistsPrecondition fallback( Database database ) {
		SequenceExistsPrecondition fallback = new SequenceExistsPrecondition();
		fallback.setCatalogName( database.getDefaultCatalogName() );
		fallback.setSchemaName( database.getDefaultSchemaName() );
		fallback.setSequenceName( getSequenceName() );
		return fallback;
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

	public void check( Database database, DatabaseChangeLog changeLog, ChangeSet changeSet, ChangeExecListener changeExecListener ) throws PreconditionFailedException, PreconditionErrorException {
		if ( database.getConnection() instanceof OfflineConnection ) {
			throw new PreconditionFailedException( String.format( "The sequence '%s.%s' was not found.", database.getDefaultSchemaName(), getSequenceName() ), changeLog, this );
		}
		
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			JdbcConnection connection = (JdbcConnection) database.getConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				final String sql = "select count(*) from all_sequences where upper(sequence_name) = upper(?) and upper(sequence_owner) = upper(?)";
				ps = connection.prepareStatement( sql );
				ps.setString( 1, getSequenceName() );
				ps.setString( 2, database.getDefaultSchemaName() );
				rs = ps.executeQuery();
				if ( !rs.next() || rs.getInt( 1 ) <= 0 ) {
					throw new PreconditionFailedException( String.format( "The sequence '%s.%s' was not found.", database.getDefaultSchemaName(), getSequenceName() ), changeLog, this );
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
    this.sequenceName = parsedNode.getChildValue(null, "sequenceName", String.class);
	}
}