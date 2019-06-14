package liquibase.ext.otbo.preconditions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.precondition.Precondition;
import liquibase.resource.ResourceAccessor;

public abstract class OtboPrecondition<R extends Precondition> implements Precondition {

	protected R redirect = null;
	
	void closeSilently( PreparedStatement ps ) {
		if ( ps != null ) {
			try {
				ps.close();
			} catch ( SQLException e ) {
			}
		}
	}

	void closeSilently( ResultSet rs ) {
		if ( rs != null ) {
			try {
				rs.close();
			} catch ( SQLException e ) {
			}
		}
	}

	public void check( Database database, DatabaseChangeLog changeLog, ChangeSet changeSet ) throws PreconditionFailedException, PreconditionErrorException {
		check( database, changeLog, changeSet, null );
	}
	
	public void load( ParsedNode parsedNode, ResourceAccessor resourceAccessor ) throws ParsedNodeException {
	}

	public String getSerializedObjectName() {
		return null;
	}

	public Set<String> getSerializableFields() {
		return null;
	}

	public Object getSerializableFieldValue( String field ) {
		return null;
	}

	public SerializationType getSerializableFieldType( String field ) {
		return null;
	}

	public String getSerializableFieldNamespace( String field ) {
		return null;
	}

	public String getSerializedObjectNamespace() {
		return null;
	}

	public ParsedNode serialize() throws ParsedNodeException {
		return null;
	}
	
	protected R redirected( Database database ) {
		if ( ! ( database instanceof OracleDatabase ) ) {
			if (redirect == null) {
				redirect = fallback( database );
			}
		}
		return redirect;
	}
	
	protected abstract R fallback( Database database );
}
