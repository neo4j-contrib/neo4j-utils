package org.neo4j.util;

import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import org.neo4j.impl.event.ProActiveEventListener;

/**
 * Listens to events, receives events when a transaction is commited. Use with
 * {@link TransactionEventManager}.
 * @author mattias
 */
public abstract class TransactionEventListener
	implements ProActiveEventListener
{
	private TransactionEventManager manager;
	
	public TransactionEventListener( TransactionEventManager manager )
	{
		this.manager = manager;
	}
	
	/**
	 * @return a list of events to listen for.
	 */
	protected abstract Event[] getEvents();
	
	/**
	 * Starts listen to events.
	 */
	public void start()
	{
		for ( Event event : getEvents() )
		{
			manager.registerEventListener( this, event );
		}
	}
	
	/**
	 * Stops listen to events.
	 */
	public void stop()
	{
		for ( Event event : getEvents() )
		{
			manager.unregisterEventListener( this, event );
		}
	}
	
	protected EventBufferFilter newFilter()
	{
		return EventBufferFilter.HOLLOW_EVENT_FILTER;
	}

	public boolean proActiveEventReceived( Event event, EventData data )
	{
		if ( event != TransactionEventManager.TX_EVENT_BUFFER )
		{
			return true;
		}
		
		EventContext[] contexts = ( EventContext[] ) data.getData();
		contexts = newFilter().filter( contexts );
		return handleEventBuffer( contexts );
	}

	protected abstract boolean handleEventBuffer( EventContext[] events );
}
