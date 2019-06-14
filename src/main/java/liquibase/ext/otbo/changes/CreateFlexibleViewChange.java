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
		name = "createFlexibleView",
		description = "Create a new database view or materialized view depending on context.",
		priority = ChangeMetaData.PRIORITY_DEFAULT )
public class CreateFlexibleViewChange extends AbstractChange {

	private String catalogName;
	private String schemaName;
	private String viewName;
	private String selectQuery;

	@DatabaseChangeProperty(
			since = "3.0" )
	public String getCatalogName() {
		return catalogName;
	}

	public void setCatalogName( String catalogName ) {
		this.catalogName = catalogName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName( String schemaName ) {
		this.schemaName = schemaName;
	}

	@DatabaseChangeProperty(
			description = "Name of the view to create" )
	public String getViewName() {
		return viewName;
	}

	public void setViewName( String viewName ) {
		this.viewName = viewName;
	}

	@DatabaseChangeProperty(
			serializationType = SerializationType.DIRECT_VALUE,
			description = "SQL for generating the [materialized] view",
			exampleValue = "select id, name from person where id > 10" )
	public String getSelectQuery() {
		return selectQuery;
	}

	public void setSelectQuery( String selectQuery ) {
		this.selectQuery = selectQuery;
	}

	public SqlStatement[] generateStatements( Database database ) {
		List<SqlStatement> statements = new ArrayList<SqlStatement>();
		statements.add( new CreateFlexibleViewStatement( getViewName(), getSelectQuery() ) );
		return statements.toArray( new SqlStatement[0] );
	}

	public String getConfirmationMessage() {
		return String.format( "Flexible View %s created", getViewName() );
	}

	protected Change[] createInverses() {
		return null; // We do not support roll back.
	}

}
