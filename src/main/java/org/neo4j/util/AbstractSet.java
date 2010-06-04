package org.neo4j.util;

import java.util.Collection;

/**
 * Abstract super class for implementations of Neo4j collections and sets.
 * @author mattias
 *
 * @param <T> The type of objects in this collection.
 */
public abstract class AbstractSet<T> implements Collection<T>
{
	public boolean addAll( Collection<? extends T> items )
	{
		boolean changed = false;
		for ( T item : items )
		{
			if ( add( item ) )
			{
				changed = true;
			}
		}
		return changed;
	}

	public boolean containsAll( Collection<?> items )
	{
		boolean ok = true;
		for ( Object item : items )
		{
			if ( !contains( item ) )
			{
				ok = false;
				break;
			}
		}
		return ok;
	}

	public boolean removeAll( Collection<?> items )
	{
		boolean changed = false;
		for ( Object item : items )
		{
			if ( remove( item ) )
			{
				changed = true;
			}
		}
		return changed;
	}
}
