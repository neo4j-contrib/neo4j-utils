package org.neo4j.util;

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class TxIterator<T> implements Iterator<T>
{
	private GraphDatabaseService neo;
	private Iterator<T> source;

	public TxIterator( GraphDatabaseService neo, Iterator<T> source )
	{
		this.neo = neo;
		this.source = source;
	}

	public boolean hasNext()
	{
		Transaction tx = neo.beginTx();
		try
		{
			boolean result = source.hasNext();
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	public T next()
	{
		Transaction tx = neo.beginTx();
		try
		{
			T result = source.next();
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	public void remove()
	{
		Transaction tx = neo.beginTx();
		try
		{
			source.remove();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
}
