package liquibase.ext.otbo.changes;

import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;

public class DropFunctionGenerator extends AbstractSqlGenerator<DropFunctionStatement> {
	public boolean supports( DropFunctionStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}
	
	public ValidationErrors validate( DropFunctionStatement statement, Database database, SqlGeneratorChain<DropFunctionStatement> sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "functionName", statement.getFunctionName() );
		return validationErrors;
	}
	
	public Sql[] generateSql( DropFunctionStatement statement, Database database, SqlGeneratorChain<DropFunctionStatement> sqlGeneratorChain ) {
		String objectName = statement.getFunctionName();
		
		if ( database instanceof MSSQLDatabase ) {
			objectName = "[" + objectName + "]";
			
			if ( statement.getSchemaName() != null ) {
				objectName = "[" + statement.getSchemaName() + "]." + objectName;
			}
		}
		
		return new Sql[] { new UnparsedSql( "DROP FUNCTION " + objectName ) };
	}
}
