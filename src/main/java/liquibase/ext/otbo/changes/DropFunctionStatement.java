package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class DropFunctionStatement extends AbstractSqlStatement {
	private String schemaName;
	private String functionName;
	
	public DropFunctionStatement( String schemaName, String functionName ) {
		this.schemaName = schemaName;
		this.functionName = functionName;
	}
	
	public String getSchemaName() {
		return schemaName;
	}
	
	public String getFunctionName() {
		return functionName;
	}
}
