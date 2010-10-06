/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.neo4j.util.TransactionNodeQueue.TxQueue;

/**
 * Handles entries from a {@link TransactionNodeQueue} using one or more
 * working threads to "eat" the queue items.
 * @author mattias
 */
public abstract class TransactionNodeQueueWorker extends Thread
{
	private GraphDatabaseService graphDb;
	private TransactionNodeQueue workQueue;
	private boolean halted;
	private int maxConsumers;
	private ExecutorService consumers;
	private Set<Integer> consumerTxIds = Collections.synchronizedSet(
		new HashSet<Integer>() );
	private boolean paused;
	private boolean fallThrough;
	private int batchSize;
	
    public TransactionNodeQueueWorker( GraphDatabaseService graphDb, Node rootNode,
        int maxConsumers )
    {
        this( graphDb, rootNode, maxConsumers, 1 );
    }
    
	public TransactionNodeQueueWorker( GraphDatabaseService graphDb, Node rootNode,
		int maxConsumers, int batchSize )
	{
		super( TransactionNodeQueueWorker.class.getSimpleName() );
		this.graphDb = graphDb;
		this.maxConsumers = maxConsumers;
		this.workQueue = createQueue( rootNode );
		this.batchSize = batchSize;
	}
	
	public void add( Map<String, Object> values )
	{
		for ( int i = 0; i < 10; i++ )
		{
			try
			{
				getQueue().add( findTxId(), values );
				return;
			}
			catch ( DeadlockDetectedException e )
			{
				try
				{
					Thread.sleep( 20 );
				}
				catch ( InterruptedException ee )
				{
					Thread.interrupted();
					// It's ok
				}
			}
		}
	}
	
	private int findTxId()
	{
	    return new UserTransactionImpl( graphDb ).getEventIdentifier();
	}

	protected TransactionNodeQueue getQueue()
	{
		return this.workQueue;
	}
	
	protected TransactionNodeQueue createQueue( Node rootNode )
	{
		return new TransactionNodeQueue( rootNode );
	}
	
	public void setPaused( boolean paused )
	{
		this.paused = paused;
	}
	
	public boolean isPaused()
	{
		return this.paused;
	}
	
	public void setFallThrough( boolean fallThrough )
	{
		// If true, it causes the entries not to be passed to its targets
		// but instead just eaten... this is a test thingie.
		this.fallThrough = fallThrough;
	}
	
	public boolean isFallThrough()
	{
		return this.fallThrough;
	}
	
	public void startUp()
	{
		this.consumers = new ThreadPoolExecutor( maxConsumers, maxConsumers, 30,
			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>( maxConsumers ),
			new ThreadFactory()
			{
				private int counter = 1;
			
				public Thread newThread( Runnable runnable )
				{
					return new Thread( runnable,
						"SearchUpdateWorker Consumer[" + ( counter++ ) + "]" );
				}
			} );
		
		start();
	}
	
	public void shutDown()
	{
		this.halted = true;
		wakeUp();
		consumers.shutdown();
		try
		{
			consumers.awaitTermination( 15, TimeUnit.SECONDS );
		}
		catch ( InterruptedException e )
		{
			Thread.interrupted();
			// It is ok
		}
	}
	
	protected void waitBeforeRun()
	{
		// Don't start immediately, wait 10 seconds or something... f.ex.
		// for the search engines to register. Yeah ugly I know
		try
		{
			Thread.sleep( 2000 );
		}
		catch ( InterruptedException e )
		{
			Thread.interrupted();
			// It is ok
		}
	}
	
	@Override
	public void run()
	{
		waitBeforeRun();
		while ( !this.halted )
		{
			try
			{
				if ( !isPaused() )
				{
					balanceQueue();
				}
			}
			catch ( DeadlockDetectedException e )
			{ // It's ok
			}
			catch ( Throwable e )
			{ // It's ok, I guess, but log it please.
				System.out.println( "Error in balance queue:" + e );
			}
			waitForChange();
		}
	}
	
	public boolean isIdle()
	{
		return numberOfConsumers() == 0;
	}
	
	synchronized void wakeUp()
	{
		notify();
	}
	
	private void addConsumer( Consumer consumer )
	{
		this.consumers.submit( consumer );
	}
	
	private void consumerDone( int txId )
	{
		consumerTxIds.remove( txId );
		wakeUp();
	}
	
	/**
	 * Throw runtime exception if it fails.
	 */
	private void doHandleEntry( Map<String, Object> entry )
	{
		if ( this.isFallThrough() )
		{
			return;
		}
		handleEntry( entry );
	}
	
	protected void beforeBatch()
	{
	}
	
	protected void afterBatch()
	{
	}
	
	protected abstract void handleEntry( Map<String, Object> entry );
	
	protected long getWaitTimeoutBetweenBalancing()
	{
		return 2000;
	}
	
	private synchronized void waitForChange()
	{
		try
		{
			wait( getWaitTimeoutBetweenBalancing() );
		}
		catch ( InterruptedException e )
		{ // Ok, but log?
			Thread.interrupted();
		}
	}
	
	private int numberOfConsumers()
	{
		return consumerTxIds.size();
	}
	
	private void balanceQueue()
	{
		Map<Integer, TxQueue> queues = getQueue().getQueues();
		Set<Integer> queueIds = new HashSet<Integer>( queues.keySet() );
		while ( !halted && numberOfConsumers() < maxConsumers &&
			!queueIds.isEmpty() )
		{
			Integer txId = queueIds.iterator().next();
			synchronized ( consumerTxIds )
			{
				if ( !consumerTxIds.contains( txId ) )
				{
					addConsumer( new Consumer( queues.get( txId ) ) );
					consumerTxIds.add( txId );
				}
			}
			queueIds.remove( txId );
		}
	}
	
	private class Consumer implements Runnable
	{
		private TxQueue updateQueue;
		private int txId;
		
		Consumer( TxQueue updateQueue )
		{
			this.updateQueue = updateQueue;
			this.txId = updateQueue.getTxId();
		}
		
		public void run()
		{
			try
			{
				while ( !halted )
				{
                    if ( isPaused() )
                    {
                        sleepSomeTime( 1000 );
                    }
                    else
                    {
                        Collection<Map<String, Object>> entries =
                            updateQueue.peek( batchSize );
                        beforeBatch();
    				    for ( Map<String, Object> entry : entries )
    				    {
       						doOne( entry );
    				    }
    				    afterBatch();
                        new EntryRemover( graphDb, updateQueue,
                            entries.size() ).run();
                    }
				}
			}
			catch ( Throwable e )
			{
//				log.error( "Caught throwable", e );
			}
			finally
			{
				consumerDone( txId );
			}
		}
		
		private void sleepSomeTime( long millis )
		{
			try
			{
				Thread.sleep( millis );
			}
			catch ( InterruptedException e )
			{
				// Ok
				Thread.interrupted();
			}
		}
		
		private void doOne( Map<String, Object> entry )
		{
			// Try a max of ten times if it fails.
			Exception exception = null;
			for ( int i = 0; !halted && i < 10; i++ )
			{
				try
				{
					doHandleEntry( entry );
					return;
				}
				catch ( Exception e )
				{
					exception = e;
				}
				sleepSomeTime( 500 );
			}
			handleEntryError( entry, exception );
		}
		
		private void handleEntryError( Map<String, Object> entry,
			Exception exception )
		{
//			log.info( entry + " re-added last in the queue, que to " +
//				( exception == null ? "" : exception.toString() ) );
			
			// Add it to the end of the queue
			Transaction tx = graphDb.beginTx();
			try
			{
				add( entry );
				tx.success();
			}
			finally
			{
				tx.finish();
			}
		}
	}
	
	private static class EntryRemover
		extends DeadlockCapsule<Object>
	{
		private GraphDatabaseService graphDb;
		private TxQueue queue;
		private int size;
		
		EntryRemover( GraphDatabaseService graphDb, TxQueue queue, int size )
		{
			super( "EntryRemover" );
			this.graphDb = graphDb;
			this.queue = queue;
			this.size = size;
		}
		
		@Override
		public Object tryOnce()
		{
			Transaction tx = graphDb.beginTx();
			try
			{
				queue.remove( size );
				tx.success();
				return null;
			}
			finally
			{
				tx.finish();
			}
		}
	}
}
