package org.neo4j.util;

import java.util.Iterator;

public abstract class NestingIterable<T> implements Iterable<T>
{
	private Iterable<T> source;
	
	public NestingIterable( Iterable<T> source )
	{
		this.source = source;
	}
	
	public Iterator<T> iterator()
	{
		return new NestingIterator<T>( source.iterator() )
		{
			@Override
			protected Iterator<T> createNestedIterator( T item )
			{
				return NestingIterable.this.createNestedIterator( item );
			}
		};
	}
	
	protected abstract Iterator<T> createNestedIterator( T item );
}
