package com.windh.util.neo;

import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import org.neo4j.impl.event.ProActiveEventListener;

public abstract class TransactionEventListener
	implements ProActiveEventListener
{
	protected abstract Event[] getEvents();
	
	public void start()
	{
		for ( Event event : getEvents() )
		{
			TransactionEventManager.getManager().registerEventListener(
				this, event );
		}
	}
	
	public void stop()
	{
		for ( Event event : getEvents() )
		{
			TransactionEventManager.getManager().unregisterEventListener(
				this, event );
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
