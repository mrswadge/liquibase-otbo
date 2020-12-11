package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class CreateFunctionStatement extends AbstractSqlStatement {
	private String functionName;
	private String functionBody;
	
	public CreateFunctionStatement( String functionName, String functionBody ) {
		this.functionName = functionName;
		this.functionBody = functionBody;
	}
	
	public String getFunctionName() {
		return functionName;
	}
	
	public String getFunctionBody() {
		return functionBody;
	}
}
