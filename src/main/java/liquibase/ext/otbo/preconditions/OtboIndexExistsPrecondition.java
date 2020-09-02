package liquibase.ext.otbo.preconditions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import liquibase.precondition.core.IndexExistsPrecondition;
import liquibase.resource.ResourceAccessor;

public class OtboIndexExistsPrecondition extends OtboPrecondition<IndexExistsPrecondition> {

	private String indexName;
	private String tableName;
	private String columnNames;

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName( String indexName ) {
		this.indexName = indexName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName( String tableName ) {
		this.tableName = tableName;
	}

	public String getColumnNames() {
		return columnNames;
	}

	public void setColumnNames( String columnNames ) {
		this.columnNames = columnNames;
	}

	public String getName() {
		return "otboIndexExists";
	}

	@Override
	protected IndexExistsPrecondition fallback( Database database ) {
		IndexExistsPrecondition fallback = new IndexExistsPrecondition();
		fallback.setCatalogName( database.getLiquibaseCatalogName() );
		fallback.setSchemaName( database.getLiquibaseSchemaName() );
		fallback.setIndexName( getIndexName() );
		fallback.setTableName( getTableName() );
		fallback.setColumnNames( getColumnNames() );
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
			ValidationErrors validationErrors = new ValidationErrors();
			if ( getIndexName() == null && getTableName() == null && getColumnNames() == null ) {
				validationErrors.addError( "indexName OR tableName and columnNames is required" );
			}
			return validationErrors;
		} else {
			return redirect.validate( database );
		}
	}

	public void check( Database database, DatabaseChangeLog changeLog, ChangeSet changeSet, ChangeExecListener changeExecListener ) throws PreconditionFailedException, PreconditionErrorException {
		if ( database.getConnection() instanceof OfflineConnection ) {
			throw new PreconditionFailedException( String.format( "No index was found on table '%s.%s' with columns '%s'.", database.getLiquibaseSchemaName(), getTableName(), getColumnNames() ), changeLog, this );
		}
		
		Precondition redirect = redirected( database );
		if ( redirect == null ) {
			JdbcConnection connection = (JdbcConnection) database.getConnection();
	
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				if ( getIndexName() != null ) {
					final String sql = "select count(*) from all_indexes i join dual d on upper(i.index_name) = upper(?) and upper(i.owner) = upper(?) left join all_constraints c on i.index_name = c.constraint_name where c.constraint_name is null";
					ps = connection.prepareStatement( sql );
					ps.setString( 1, getIndexName() );
					ps.setString( 2, database.getLiquibaseSchemaName() );
					rs = ps.executeQuery();
					if ( !rs.next() || rs.getInt( 1 ) <= 0 ) {
						throw new PreconditionFailedException( String.format( "The index '%s.%s' was not found.", database.getLiquibaseSchemaName(), getIndexName() ), changeLog, this );
					}
				} else {
					final String sql = "select i.index_name, i.column_name from ( select * from all_ind_columns where upper(table_name) = upper (?) and upper(index_owner) = upper(?) ) i left join all_constraints c on i.index_name = c.constraint_name where c.constraint_name is null";
					ps = connection.prepareStatement( sql );
					ps.setString( 1, getTableName() );
					ps.setString( 2, database.getLiquibaseSchemaName() );
					rs = ps.executeQuery();
	
					Map<String, List<String>> columnsMap = new HashMap<String, List<String>>();
					while ( rs.next() ) {
						String indexName = rs.getString( 1 );
						String columnName = rs.getString( 2 );
						List<String> cols = columnsMap.get( indexName );
						if ( cols == null ) {
							cols = new ArrayList<String>();
							columnsMap.put( indexName, cols );
						}
						cols.add( columnName.toUpperCase() );
					}
	
					String[] expectedColumns = getColumnNames().toUpperCase().split( "\\s*,\\s*" );
	
					// check for an index with all columns listed.
					for ( String index : columnsMap.keySet() ) {
						List<String> columnNames = columnsMap.get( index );
						if ( columnNames.size() == expectedColumns.length ) {
							if ( columnNames.containsAll( Arrays.asList( expectedColumns ) ) ) {
								return;
							}
						}
					}
					throw new PreconditionFailedException( String.format( "No index was found on table '%s.%s' with columns '%s'.", database.getLiquibaseSchemaName(), getTableName(), getColumnNames() ), changeLog, this );
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
    this.columnNames = parsedNode.getChildValue(null, "columnNames", String.class);
    this.indexName = parsedNode.getChildValue(null, "indexName", String.class);
    this.tableName = parsedNode.getChildValue(null, "tableName", String.class);
	}
}