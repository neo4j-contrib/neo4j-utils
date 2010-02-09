package org.neo4j.util;

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class TxIterator<T> implements Iterator<T>
{
	private GraphDatabaseService graphDb;
	private Iterator<T> source;

	public TxIterator( GraphDatabaseService graphDb, Iterator<T> source )
	{
		this.graphDb = graphDb;
		this.source = source;
	}

	public boolean hasNext()
	{
		Transaction tx = graphDb.beginTx();
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
		Transaction tx = graphDb.beginTx();
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
		Transaction tx = graphDb.beginTx();
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
