package liquibase.ext.otbo.changes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewGenerator;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.ext.otbo.preconditions.OtboMaterializedViewExistsPrecondition;
import liquibase.logging.LogService;
import liquibase.logging.Logger;
import liquibase.precondition.core.ViewExistsPrecondition;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.DropViewGenerator;
import liquibase.statement.core.DropViewStatement;

public class DropFlexibleViewGenerator extends AbstractSqlGenerator<DropFlexibleViewStatement> {
	
	private static final Logger log = LogService.getLog(DropFlexibleViewGenerator.class);
	
	@Override
	public boolean supports( DropFlexibleViewStatement statement, Database database ) {
		return database instanceof OracleDatabase;
	}

	public ValidationErrors validate( DropFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "viewName", statement.getViewName() );
		return validationErrors;
	}

	public Sql[] generateSql( DropFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		JdbcConnection connection = (JdbcConnection) database.getConnection();
		// check if the view already exists, then formulate a plan from there!
		
		OtboMaterializedViewExistsPrecondition mviewCheck = new OtboMaterializedViewExistsPrecondition();
		mviewCheck.setViewName( statement.getViewName() );
		boolean mviewExists = mviewCheck.check( database );

		boolean viewExists = false;
		try {
			ViewExistsPrecondition viewCheck = new ViewExistsPrecondition();
			viewCheck.setViewName( statement.getViewName() );
			viewCheck.check( database, null, null, null );
			viewExists = true;
		} catch ( PreconditionFailedException e ) {
			// ignore
		} catch ( PreconditionErrorException e ) {
			throw new RuntimeException( e );
		}
		
		List<Sql> sequel = new ArrayList<Sql>();
		
		if ( mviewExists ) {
			DropMaterializedViewStatement dropMViewStmt = new DropMaterializedViewStatement( statement.getViewName() );
			dropMViewStmt.setSchemaName( database.getLiquibaseSchemaName() );
			DropMaterializedViewGenerator dropMViewGen = new DropMaterializedViewGenerator();
			sequel.addAll( Arrays.asList( dropMViewGen.generateSql( dropMViewStmt, database, null ) ) );
		} else if ( viewExists ) {
			DropViewStatement dropViewStmt = new DropViewStatement( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), statement.getViewName() );
			DropViewGenerator dropViewGen = new DropViewGenerator();
			sequel.addAll( Arrays.asList( dropViewGen.generateSql( dropViewStmt, database, null ) ) );
		} else {
			log.warning( String.format( "The [materialized] view named %s was not found when it was attempted to be dropped from the database.", statement.getViewName() ) );
		}

		log.debug( sequel.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} }  ).collect( Collectors.joining( "\n" ) ) );
		
		return sequel.toArray( new Sql[0] );
	}
}
