package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.List;

import liquibase.change.AbstractChange;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.database.Database;
import liquibase.statement.SqlStatement;

@DatabaseChange(
		name = "dropFlexibleView",
		description = "Drop an flexible database view or materialized view.",
		priority = ChangeMetaData.PRIORITY_DEFAULT )
public class DropFlexibleViewChange extends AbstractChange {

	private String viewName;

	@DatabaseChangeProperty( description = "Name of the view to drop" )
	public String getViewName() {
		return viewName;
	}

	public void setViewName( String viewName ) {
		this.viewName = viewName;
	}

	public SqlStatement[] generateStatements( Database database ) {
		List<SqlStatement> statements = new ArrayList<SqlStatement>();
		statements.add( new DropFlexibleViewStatement( getViewName() ) );
		return statements.toArray( new SqlStatement[0] );
	}

	public String getConfirmationMessage() {
		return String.format( "Flexible View %s dropped", getViewName() );
	}

	protected Change[] createInverses() {
		return null; // We do not support roll back.
	}

}
