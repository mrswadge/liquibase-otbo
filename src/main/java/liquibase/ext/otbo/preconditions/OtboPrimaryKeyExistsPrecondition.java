package liquibase.ext.otbo.preconditions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import liquibase.Liquibase;
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
import liquibase.logging.LogService;
import liquibase.logging.Logger;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.precondition.Precondition;
import liquibase.precondition.core.PrimaryKeyExistsPrecondition;
import liquibase.resource.ResourceAccessor;

public class OtboPrimaryKeyExistsPrecondition extends OtboPrecondition<PrimaryKeyExistsPrecondition> {

  private static final Logger LOG = LogService.getLog(OtboPrimaryKeyExistsPrecondition.class);

	private String primaryKeyName;
	private String tableName;

	public String getTableName() {
		return tableName;
	}

	public void setTableName( String tableName ) {
		this.tableName = tableName;
	}

	public String getName() {
		return "otboPrimaryKeyExists";
	}

	public String getPrimaryKeyName() {
		return primaryKeyName;
	}

	public void setPrimaryKeyName( String constraintName ) {
		this.primaryKeyName = constraintName;
	}

	@Override
	protected PrimaryKeyExistsPrecondition fallback( Database database ) {
		PrimaryKeyExistsPrecondition fallback = new PrimaryKeyExistsPrecondition();
		fallback.setCatalogName( database.getLiquibaseCatalogName() );
		fallback.setSchemaName( database.getLiquibaseSchemaName() );
		fallback.setTableName( getTableName() );
		fallback.setPrimaryKeyName( getPrimaryKeyName() );
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
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			JdbcConnection connection = (JdbcConnection) database.getConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				/*
				THE CONSTRAINT_TYPE will tell you what type of constraint it is
				
				R - Referential key ( foreign key)
				U - Unique key
				P - Primary key
				C - Check constraint
				 */
				final String sql = "select constraint_name from all_constraints where table_name = upper(?) and upper(owner) = upper(?) and constraint_type = 'P'";
				ps = connection.prepareStatement( sql );
				ps.setString( 1, getTableName() );
				ps.setString( 2, database.getLiquibaseSchemaName() );
				rs = ps.executeQuery();
				
				if ( !rs.next() ) {
					throw new PreconditionFailedException( String.format( "The primary key '%s' was not found on the table '%s.%s'.", getPrimaryKeyName(), database.getLiquibaseSchemaName(), getTableName() ), changeLog, this );
				} else {
					String name = rs.getString( 1 );
					if ( getPrimaryKeyName() != null && getPrimaryKeyName().length() > 0 ) {
						// In Liquibase 3.6.3 the requirement is that a primary key is present. It does not have to share the same name as the names are generated and provided by the database.
						if ( ! name.equalsIgnoreCase( getPrimaryKeyName() ) ) {
							LOG.warning( String.format( "The primary key '%s' was not found on the table '%s.%s', but instead a primary key '%s' was found.", getPrimaryKeyName(), database.getLiquibaseSchemaName(), getTableName(), name ) );
						}
					}
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
    this.tableName = parsedNode.getChildValue(null, "tableName", String.class);
    this.primaryKeyName = parsedNode.getChildValue(null, "primaryKeyName", String.class);
	}
}