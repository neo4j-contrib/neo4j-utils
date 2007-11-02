package org.neo4j.util.xaworker;

public class XaWorkerEntry
{
	private Object[] values;
	
	public XaWorkerEntry()
	{
	}
	
	public void setValues( Object... values )
	{
		assertValues( values );
		this.values = values;
	}
	
	private void assertValues( Object[] values )
	{
		for ( Object value : values )
		{
			if ( !( value instanceof Number ) )
			{
				throw new RuntimeException( "Only numbers" );
			}
		}
	}
	
	public Object[] getValues()
	{
		return this.values;
	}
	
	@Override
	public String toString()
	{
		return "SearchXaEntry[" + this.getValues() + "]";
	}
}
