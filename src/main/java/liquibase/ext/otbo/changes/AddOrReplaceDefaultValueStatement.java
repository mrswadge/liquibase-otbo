package liquibase.ext.otbo.changes;

import liquibase.statement.core.AddDefaultValueStatement;

public class AddOrReplaceDefaultValueStatement extends AddDefaultValueStatement {
	public AddOrReplaceDefaultValueStatement( String catalogName, String schemaName, String tableName, String columnName, String columnDataType ) {
		super( catalogName, schemaName, tableName, columnName, columnDataType );
	}

	public AddOrReplaceDefaultValueStatement( String catalogName, String schemaName, String tableName, String columnName, String columnDataType, Object defaultValue ) {
		super( catalogName, schemaName, tableName, columnName, columnDataType, defaultValue );
	}
}
