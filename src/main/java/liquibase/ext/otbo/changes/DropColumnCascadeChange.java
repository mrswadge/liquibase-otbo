package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.List;

import liquibase.change.AbstractChange;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.database.Database;
import liquibase.statement.SqlStatement;

@DatabaseChange(
		name = "dropColumnCascade",
		description = "Remove a column from the database and any constraints tied to them too.",
		priority = ChangeMetaData.PRIORITY_DEFAULT )
public class DropColumnCascadeChange extends AbstractChange {

	private String catalogName;
	private String schemaName;
	private String tableName;
	private String columnName;

	@DatabaseChangeProperty(since = "3.0")
	public String getCatalogName() {
		return catalogName;
	}

	public void setCatalogName( String catalogName ) {
		this.catalogName = catalogName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName( String schemaName ) {
		this.schemaName = schemaName;
	}

	@DatabaseChangeProperty(description = "Name of the table" )
	public String getTableName() {
		return tableName;
	}

	public void setTableName( String tableName ) {
		this.tableName = tableName;
	}

	@DatabaseChangeProperty(description = "Name of the column" )
	public String getColumnName() {
		return columnName;
	}

	public void setColumnName( String columnName ) {
		this.columnName = columnName;
	}

	public SqlStatement[] generateStatements( Database database ) {
		List<SqlStatement> statements = new ArrayList<SqlStatement>();
		statements.add( new DropColumnCascadeStatement( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), getTableName(), getColumnName() ) );
		return statements.toArray( new SqlStatement[0] );
	}

	public String getConfirmationMessage() {
		return String.format( "Column %s.%s has been removed", getTableName(), getColumnName() );
	}

	protected Change[] createInverses() {
		return null; // We do not support roll back.
	}

}
