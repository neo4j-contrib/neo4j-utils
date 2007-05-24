package com.windh.util.neo;

import java.util.Collection;
import java.util.Set;
import org.neo4j.api.core.Transaction;

public abstract class AbstractNeoSet<T> implements Set<T>
{
	public boolean addAll( Collection items )
	{
		Transaction tx = Transaction.begin();
		try
		{
			boolean changed = false;
			for ( Object item : items )
			{
				if ( add( ( T ) item ) )
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

	public boolean containsAll( Collection items )
	{
		Transaction tx = Transaction.begin();
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

	public boolean removeAll( Collection items )
	{
		Transaction tx = Transaction.begin();
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
