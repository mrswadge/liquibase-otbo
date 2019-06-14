package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.core.AddDefaultValueChange;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.DropDefaultValueStatement;

@DatabaseChange(name = "addOrReplaceDefaultValue",
description = "Adds or replaces a default value to the database definition for the specified column.\n" +
        "One of defaultValue, defaultValueNumeric, defaultValueBoolean or defaultValueDate must be set",
priority = ChangeMetaData.PRIORITY_DEFAULT, appliesTo = "column")
public class AddOrReplaceDefaultValueChange extends AddDefaultValueChange {
	@Override
	public SqlStatement[] generateStatements( Database database ) {
		List<SqlStatement> statements = new ArrayList<SqlStatement>();
		if ( database instanceof MSSQLDatabase ) {
			statements.add( new DropDefaultValueStatement( getCatalogName(), getSchemaName(), getTableName(), getColumnName(), null ) );
		}
		statements.addAll( Arrays.asList( super.generateStatements( database ) ) );
		return statements.toArray( new SqlStatement[0] );
	}
}
