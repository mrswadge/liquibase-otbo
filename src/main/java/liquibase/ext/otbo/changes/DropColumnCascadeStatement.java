package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class DropColumnCascadeStatement extends AbstractSqlStatement {
	private String catalogName;
	private String schemaName;
	private String tableName;
	private String columnName;

	public DropColumnCascadeStatement( String catalogName, String schemaName, String tableName, String columnName ) {
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.tableName = tableName;
		this.columnName = columnName;
	}

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

	public String getTableName() {
		return tableName;
	}

	public void setTableName( String tableName ) {
		this.tableName = tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName( String columnName ) {
		this.columnName = columnName;
	}
}
