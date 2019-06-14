package liquibase.ext.otbo.changes;

import java.util.ArrayList;
import java.util.List;

public enum FlexibleView {
	MATERIALIZED( "MATERIALIZED VIEW" ), VIEW( "VIEW" );
	private String SQLName;

	FlexibleView( String SQLName ) {
		this.SQLName = SQLName;
	}

	public String getSQLName() {
		return SQLName;
	}

	@Override
	public String toString() {
		return getSQLName();
	}
	
	public static final FlexibleView deriveByValue( String value ) {
		for ( FlexibleView v : values() ) {
			if ( v.getSQLName().equals( value ) ) {
				return v;
			}
		}
		return null;
	}
	
	public static final String[] getSQLValues() {
		List<String> values = new ArrayList<String>();
		for ( FlexibleView v : values() ) {
			values.add( v.getSQLName() );
		}
		return values.toArray( new String[0] );
	}
}
