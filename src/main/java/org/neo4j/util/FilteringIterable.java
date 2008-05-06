package org.neo4j.util;

import java.util.Iterator;

public abstract class FilteringIterable<T> implements Iterable<T>
{
	private Iterable<T> source;
	
	public FilteringIterable( Iterable<T> source )
	{
		this.source = source;
	}
	
	public Iterator<T> iterator()
	{
		return new FilteringIterator<T>( source.iterator() )
		{
			@Override
            protected boolean passes( T item )
            {
	            return FilteringIterable.this.passes( item );
            }
		};
	}
	
	protected abstract boolean passes( T item );
}
