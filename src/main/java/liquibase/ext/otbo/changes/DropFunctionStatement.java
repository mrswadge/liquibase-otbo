package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class DropFunctionStatement extends AbstractSqlStatement {
	private String functionName;
	
	public DropFunctionStatement( String functionName ) {
		this.functionName = functionName;
	}
	
	public String getFunctionName() {
		return functionName;
	}
}
