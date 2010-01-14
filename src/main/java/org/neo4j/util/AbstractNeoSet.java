package org.neo4j.util;

import java.util.Collection;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * Abstract super class for implementations of neo collections and sets.
 * @author mattias
 *
 * @param <T> The type of objects in this collection.
 */
public abstract class AbstractNeoSet<T> implements Collection<T>
{
	private GraphDatabaseService neo;
	
	public AbstractNeoSet( GraphDatabaseService neo )
	{
		this.neo = neo;
	}
	
	protected GraphDatabaseService neo()
	{
		return this.neo;
	}
	
	public boolean addAll( Collection<? extends T> items )
	{
		Transaction tx = neo().beginTx();
		try
		{
			boolean changed = false;
			for ( T item : items )
			{
				if ( add( item ) )
				{
					changed = true;
				}
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean containsAll( Collection<?> items )
	{
		Transaction tx = neo().beginTx();
		try
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
			tx.success();
			return ok;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean removeAll( Collection<?> items )
	{
		Transaction tx = neo().beginTx();
		try
		{
			boolean changed = false;
			for ( Object item : items )
			{
				if ( remove( item ) )
				{
					changed = true;
				}
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}
}
