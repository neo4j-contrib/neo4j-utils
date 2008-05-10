package org.neo4j.util;

import java.util.Iterator;
import java.util.LinkedList;

public class CombiningIterable<T> implements Iterable<T>
{
	private Iterable<Iterable<T>> iterables;
	
	public CombiningIterable( Iterable<Iterable<T>> iterables )
	{
		this.iterables = iterables;
	}
	
	public Iterator<T> iterator()
	{
		LinkedList<Iterator<T>> iterators = new LinkedList<Iterator<T>>();
		for ( Iterable<T> iterable : iterables )
		{
			iterators.add( iterable.iterator() );
		}
		return new CombiningIterator<T>( iterators );
	}
}
