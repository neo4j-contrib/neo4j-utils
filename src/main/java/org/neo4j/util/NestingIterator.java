package org.neo4j.util;

import java.util.Iterator;

/**
 * For each item in the supplied iterator (called surface item) there's
 * instantiated an iterator from that item which is iterated before moving
 * on to the next surface item.
 *
 * @param <T> the type of items.
 */
public abstract class NestingIterator<T> extends PrefetchingIterator<T>
{
	private Iterator<T> source;
	private Iterator<T> currentNestedIterator;
	private T currentSurfaceItem;
	
	public NestingIterator( Iterator<T> source )
	{
		this.source = source;
	}
	
	protected abstract Iterator<T> createNestedIterator( T item );
	
	public T getCurrentSurfaceItem()
	{
		if ( this.currentSurfaceItem == null )
		{
			throw new IllegalStateException( "Has no surface item right now," +
				" you must to at least one next() first" );
		}
		return this.currentSurfaceItem;
	}
	
	@Override
	protected T fetchNextOrNull()
	{
		if ( currentNestedIterator == null ||
			!currentNestedIterator.hasNext() )
		{
			while ( source.hasNext() )
			{
				currentSurfaceItem = source.next();
				currentNestedIterator =
					createNestedIterator( currentSurfaceItem );
				if ( currentNestedIterator.hasNext() )
				{
					break;
				}
			}
		}
		return currentNestedIterator != null &&
			currentNestedIterator.hasNext() ?
			currentNestedIterator.next() : null;
	}
}
