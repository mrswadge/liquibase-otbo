package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class CreateFunctionStatement extends AbstractSqlStatement {
	private String schemaName;
	private String functionName;
	private String functionBody;
	
	public CreateFunctionStatement( String schemaName, String functionName, String functionBody ) {
		this.schemaName = schemaName;
		this.functionName = functionName;
		this.functionBody = functionBody;
	}
	
	public String getSchemaName() {
		return schemaName;
	}
	
	public String getFunctionName() {
		return functionName;
	}
	
	public String getFunctionBody() {
		return functionBody;
	}
}
