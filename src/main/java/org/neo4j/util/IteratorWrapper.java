package org.neo4j.util;

import java.util.Iterator;

public abstract class IteratorWrapper<T, U> implements Iterator<T>
{
	private Iterator<U> source;
	
	public IteratorWrapper( Iterator<U> iteratorToWrap )
	{
		this.source = iteratorToWrap;
	}
	
	public boolean hasNext()
	{
		return this.source.hasNext();
	}
	
	public T next()
	{
		return underlyingObjectToObject( this.source.next() );
	}
	
	public void remove()
	{
		this.source.remove();
	}
	
	protected abstract T underlyingObjectToObject( U object );
}
