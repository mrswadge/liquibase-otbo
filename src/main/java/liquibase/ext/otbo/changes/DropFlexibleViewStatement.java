package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class DropFlexibleViewStatement extends AbstractSqlStatement {
	private String viewName;

	public DropFlexibleViewStatement( String viewName ) {
		this.viewName = viewName;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName( String viewName ) {
		this.viewName = viewName;
	}

}
