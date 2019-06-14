package liquibase.ext.otbo.preconditions;

import liquibase.precondition.core.ColumnExistsPrecondition;

public class OtboColumnExistsPrecondition extends ColumnExistsPrecondition {

	@Override
	public String getName() {
		return "otboColumnExists";
	}
	
}
