package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.List;

import liquibase.change.AbstractChange;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.database.Database;
import liquibase.statement.SqlStatement;

@DatabaseChange(
		name = "convertViewsIntoMaterializedViews",
		description = "Convert all views into materialized views.",
		priority = ChangeMetaData.PRIORITY_DEFAULT )
public class ConvertViewsIntoMaterializedViewsChange extends AbstractChange {

	public SqlStatement[] generateStatements( Database database ) {
		List<SqlStatement> statements = new ArrayList<SqlStatement>();
		statements.add( new ConvertViewsIntoMaterializedViewsStatement() );
		return statements.toArray( new SqlStatement[0] );
	}

	public String getConfirmationMessage() {
		return String.format( "All views converted to materialized views." );
	}

	protected Change[] createInverses() {
		return null; // We do not support roll back.
	}

}
