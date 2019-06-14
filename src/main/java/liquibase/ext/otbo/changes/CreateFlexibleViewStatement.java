package liquibase.ext.otbo.changes;

import liquibase.statement.AbstractSqlStatement;

public class CreateFlexibleViewStatement extends AbstractSqlStatement {
	private String viewName;
	private String selectQuery;

	public CreateFlexibleViewStatement( String viewName, String selectQuery ) {
		this.viewName = viewName;
		this.selectQuery = selectQuery;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName( String viewName ) {
		this.viewName = viewName;
	}

	public String getSelectQuery() {
		return selectQuery;
	}

	public void setSelectQuery( String selectQuery ) {
		this.selectQuery = selectQuery;
	}

}
