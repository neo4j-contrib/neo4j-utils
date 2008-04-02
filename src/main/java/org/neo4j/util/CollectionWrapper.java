package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public abstract class CollectionWrapper<T, U> implements Collection<T>
{
	private Collection<U> collection;
	
	public CollectionWrapper( Collection<U> underlyingCollection )
	{
		this.collection = underlyingCollection;
	}
	
	protected abstract U objectToUnderlyingObject( T object );
	
	protected abstract T underlyingObjectToObject( U object );

	public boolean add( T o )
	{
		return collection.add( objectToUnderlyingObject( o ) );
	}

	public void clear()
	{
		collection.clear();
	}

	public boolean contains( Object o )
	{
		return collection.contains( objectToUnderlyingObject( ( T ) o ) );
	}

	public boolean isEmpty()
	{
		return collection.isEmpty();
	}

	public Iterator<T> iterator()
	{
		return new WrappingIterator( collection.iterator() );
	}

	public boolean remove( Object o )
	{
		return collection.remove( objectToUnderlyingObject( ( T ) o ) );
	}

	public int size()
	{
		return collection.size();
	}
	
	protected Collection<U> convertCollection( Collection c )
	{
		Collection<U> converted = new HashSet<U>();
		for ( Object item : c )
		{
			converted.add( objectToUnderlyingObject( ( T ) item ) );
		}
		return converted;
	}
	
	public boolean retainAll( Collection c )
	{
		return collection.retainAll( convertCollection( c ) );
	}

	public boolean addAll( Collection c )
	{
		return collection.addAll( convertCollection( c ) );
	}

	public boolean removeAll( Collection c )
	{
		return collection.removeAll( convertCollection( c ) );
	}

	public boolean containsAll( Collection c )
	{
		return collection.containsAll( convertCollection( c ) );
	}

	public Object[] toArray()
	{
		Object[] array = collection.toArray();
		Object[] result = new Object[ array.length ];
		for ( int i = 0; i < array.length; i++ )
		{
			result[ i ] = underlyingObjectToObject( ( U ) array[ i ] );
		}
		return result;
	}

	public <R> R[] toArray( R[] a )
	{
		Object[] array = collection.toArray();
		ArrayList<R> result = new ArrayList<R>();
		for ( int i = 0; i < array.length; i++ )
		{
			result.add( ( R ) underlyingObjectToObject( ( U ) array[ i ] ) );
		}
		return result.toArray( a );
	}
	
	private class WrappingIterator implements Iterator<T>
	{
		private Iterator<U> iterator;
		
		WrappingIterator( Iterator<U> iterator )
		{
			this.iterator = iterator;
		}
		
		public boolean hasNext()
		{
			return iterator.hasNext();
		}
		
		public T next()
		{
			return underlyingObjectToObject( iterator.next() );
		}
		
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
