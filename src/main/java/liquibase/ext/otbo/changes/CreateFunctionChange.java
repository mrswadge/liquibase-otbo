package liquibase.ext.otbo.changes;

import liquibase.change.AbstractChange;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.database.Database;
import liquibase.statement.SqlStatement;

@DatabaseChange(
				name = "createFunction",
				description = "Creates a new function",
				priority = ChangeMetaData.PRIORITY_DEFAULT )
public class CreateFunctionChange extends AbstractChange {
	private String functionName;
	private String functionBody;
	
	@DatabaseChangeProperty(
					description = "Name of the function to create" )
	public String getFunctionName() {
		return functionName;
	}
	
	public void setFunctionName( String functionName ) {
		this.functionName = functionName;
	}
	
	@DatabaseChangeProperty(
					serializationType = SerializationType.DIRECT_VALUE )
	public String getFunctionBody() {
		return functionBody;
	}
	
	public void setFunctionBody( String functionBody ) {
		this.functionBody = functionBody;
	}
	
	public String getConfirmationMessage() {
		return "Function " + getFunctionName() + " created";
	}
	
	public SqlStatement[] generateStatements( Database database ) {
		return new SqlStatement[] { new CreateFunctionStatement( getFunctionName(), getFunctionBody() ) };
	}
}
