package org.neo4j.util;

import java.util.Iterator;

public abstract class IterableWrapper<T, U> implements Iterable<T>
{
	private Iterable<U> source;
	
	public IterableWrapper( Iterable<U> iterableToWrap )
	{
		this.source = iterableToWrap;
	}
	
	protected abstract T underlyingObjectToObject( U object );
	
	public Iterator<T> iterator()
	{
		return new MyIteratorWrapper( source.iterator() );
	}
	
	private class MyIteratorWrapper extends IteratorWrapper<T, U>
	{
		public MyIteratorWrapper( Iterator<U> iteratorToWrap )
        {
	        super( iteratorToWrap );
        }

		@Override
        protected T underlyingObjectToObject( U object )
        {
	        return IterableWrapper.this.underlyingObjectToObject( object );
        }
	}
}
