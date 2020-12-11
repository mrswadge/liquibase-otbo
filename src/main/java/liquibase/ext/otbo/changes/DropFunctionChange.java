package liquibase.ext.otbo.changes;

import liquibase.change.AbstractChange;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.database.Database;
import liquibase.statement.SqlStatement;

@DatabaseChange(
				name = "dropFunction",
				description = "Drops an existing function",
				priority = ChangeMetaData.PRIORITY_DEFAULT )
public class DropFunctionChange extends AbstractChange {
	private String functionName;
	
	@DatabaseChangeProperty(
					description = "Name of the function to drop" )
	public String getFunctionName() {
		return functionName;
	}
	
	public void setFunctionName( String functionName ) {
		this.functionName = functionName;
	}
	
	public String getConfirmationMessage() {
		return "Function " + getFunctionName() + " dropped";
	}
	
	public SqlStatement[] generateStatements( Database database ) {
		return new SqlStatement[] { new DropFunctionStatement( getFunctionName() ) };
	}
}
