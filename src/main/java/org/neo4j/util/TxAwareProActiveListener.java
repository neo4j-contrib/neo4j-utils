package org.neo4j.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.event.Event;
import org.neo4j.kernel.impl.event.EventData;
import org.neo4j.kernel.impl.event.ProActiveEventListener;

/**
 * This succer keeps some kind of track of transactions and may filter incoming
 * events depending on which events have come previously in the transaction.
 * Optimization issues. F.ex. if ten ISE_PROPERTY_SET events comes in one
 * transaction for the same event then only ony may be nessecary.
 */
public abstract class TxAwareProActiveListener implements ProActiveEventListener
{
	private GraphDatabaseUtil graphDbUtil;
	
	public TxAwareProActiveListener( GraphDatabaseService graphDb )
	{
		this.graphDbUtil = new GraphDatabaseUtil( graphDb );
	}
	
	private ConcurrentMap<Transaction, EventFilter> eventLists =
		new ConcurrentHashMap<Transaction, EventFilter>();
	
	protected abstract Event[] getEvents();
	
	/**
	 * Override this please
	 * @return
	 */
	protected EventFilter newFilter()
	{
		return EventFilter.HOLLOW_EVENT_FILTER;
	}
	
	/**
	 * Registers this listener to listen to events.
	 */
	public void register()
	{
		for ( Event event : getEvents() )
		{
			graphDbUtil.registerProActiveEventListener( this, event );
		}
	}
	
	/**
	 * Unregisters this listener so that no more events reaches this listener.
	 */
	public void unregister()
	{
		for ( Event event : getEvents() )
		{
			graphDbUtil.unregisterProActiveEventListener( this, event );
		}
	}
	
	protected abstract boolean handleEvent( Event event, EventData data );
	
	protected void txStarted()
	{
	}
	
	protected void txEnded( boolean committed )
	{
	}
	
	public final boolean proActiveEventReceived( Event event, EventData data )
	{
		boolean result = true;
		try
		{
		    final Transaction tx =
		        graphDbUtil.getTransactionManager().getTransaction();
			EventContext context = new EventContext( event, data );
			EventFilter filter = this.eventLists.get( tx );
			if ( filter == null )
			{
			    filter = this.newFilter();
			    this.eventLists.put( tx, filter );
			    tx.registerSynchronization( new Synchronization()
			    {
                    public void afterCompletion( int status )
                    {
                        eventLists.remove( tx );
                    }

                    public void beforeCompletion()
                    {
                    }
			    } );
			}
			
			if ( filter.pass( context.getEvent(), context.getData() ) )
			{
				result = handleEvent( context.getEvent(),
					context.getData() );
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			result = false;
		}
		return result;
	}
}
