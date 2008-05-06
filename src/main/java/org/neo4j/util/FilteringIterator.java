package org.neo4j.util;

import java.util.Iterator;

public abstract class FilteringIterator<T> extends PrefetchingIterator<T>
{
	private Iterator<T> source;
	
	public FilteringIterator( Iterator<T> source )
	{
		this.source = source;
	}
	
	@Override
	protected T fetchNextOrNull()
	{
		while ( source.hasNext() )
		{
			T testItem = source.next();
			if ( passes( testItem ) )
			{
				return testItem;
			}
		}
		return null;
	}
	
	protected abstract boolean passes( T item );
}
