package org.neo4j.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.transaction.DeadlockDetectedException;
import org.neo4j.impl.transaction.UserTransactionImpl;
import org.neo4j.util.NeoTransactionQueue.TxQueue;

/**
 * Handles entries from a {@link NeoTransactionQueue} using one or more
 * working threads to "eat" the queue items.
 * @author mattias
 */
public abstract class NeoTransactionQueueWorker extends Thread
{
	private NeoService neo;
	private NeoTransactionQueue workQueue;
	private boolean halted;
	private int maxConsumers;
	private ExecutorService consumers;
	private Set<Integer> consumerTxIds = Collections.synchronizedSet(
		new HashSet<Integer>() );
	private boolean paused;
	private boolean fallThrough;
	
	public NeoTransactionQueueWorker( NeoService neo, Node rootNode,
		int maxConsumers )
	{
		super( NeoTransactionQueueWorker.class.getSimpleName() );
		this.neo = neo;
		this.maxConsumers = maxConsumers;
		this.workQueue = createQueue( rootNode );
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
					// It's ok
				}
			}
		}
	}
	
	private int findTxId()
	{
		return UserTransactionImpl.getInstance().getEventIdentifier();
	}

	protected NeoTransactionQueue getQueue()
	{
		return this.workQueue;
	}
	
	protected NeoTransactionQueue createQueue( Node rootNode )
	{
		return new NeoTransactionQueue( neo, rootNode );
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
		private EntryGetter entryGetter;
		
		Consumer( TxQueue updateQueue )
		{
			this.updateQueue = updateQueue;
			this.txId = updateQueue.getTxId();
			this.entryGetter = new EntryGetter( updateQueue );
		}
		
		public void run()
		{
			try
			{
				Map<String, Object> entry = updateQueue.peek();
				while ( !halted && entry != null )
				{
					if ( isPaused() )
					{
						sleepSomeTime( 1000 );
					}
					else
					{
						doOne( entry );
						entry = getNextEntry();
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
		
		private Map<String, Object> getNextEntry()
		{
			return entryGetter.run();
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
			Transaction tx = Transaction.begin();
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
	
	private static class EntryGetter
		extends DeadlockCapsule<Map<String, Object>>
	{
		private TxQueue queue;
		
		EntryGetter( TxQueue queue )
		{
			super( "EntryGetter" );
			this.queue = queue;
		}
		
		@Override
		public Map<String, Object> tryOnce()
		{
			Transaction tx = Transaction.begin();
			try
			{
				queue.remove();
				Map<String, Object> result = queue.peek();
				tx.success();
				return result;
			}
			finally
			{
				tx.finish();
			}
		}
	}
}
