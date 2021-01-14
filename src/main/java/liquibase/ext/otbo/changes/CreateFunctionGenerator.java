package liquibase.ext.otbo.changes;

import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.util.SqlParser;
import liquibase.util.StringClauses;
import liquibase.util.StringClauses.ClauseIterator;

public class CreateFunctionGenerator extends AbstractSqlGenerator<CreateFunctionStatement> {
	public boolean supports( CreateFunctionStatement statement, Database database ) {
		return database instanceof OracleDatabase || database instanceof MSSQLDatabase;
	}
	
	public ValidationErrors validate( CreateFunctionStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "functionName", statement.getFunctionName() );
		validationErrors.checkRequiredField( "functionBody", statement.getFunctionBody() );
		return validationErrors;
	}
	
	public Sql[] generateSql( CreateFunctionStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		String sql = statement.getFunctionBody();
		
		if ( database instanceof MSSQLDatabase ) {
			String objectName = "[" + statement.getFunctionName() + "]";
			
			if ( statement.getSchemaName() != null ) {
				objectName = "[" + statement.getSchemaName() + "]." + objectName;
			}
			
			StringClauses parsedSql = SqlParser.parse( sql, true, true );
			ClauseIterator clauseIter = parsedSql.getClauseIterator();
			Object next = "START";
			
			while ( next != null && !next.toString().equalsIgnoreCase( "CREATE" ) && clauseIter.hasNext() ) {
				next = clauseIter.nextNonWhitespace();
			}
			
			clauseIter.replace( "ALTER" );
			String createSql = sql.replace( "'", "''" );
			String alterSql = parsedSql.toString().replace( "'", "''" );
			sql = "IF OBJECT_ID('" + objectName + "') IS NULL EXEC ('" + createSql + "') ELSE EXEC ('" + alterSql + "')";
		}
		
		return new Sql[] { new UnparsedSql( sql ) };
	}
}
