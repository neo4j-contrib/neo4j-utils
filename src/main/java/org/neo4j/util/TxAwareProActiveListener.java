package org.neo4j.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.neo4j.api.core.NeoService;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import org.neo4j.impl.event.ProActiveEventListener;

/**
 * This succer keeps some kind of track of transactions and may filter incoming
 * events depending on which events have come previously in the transaction.
 * Optimization issues. F.ex. if ten ISE_PROPERTY_SET events comes in one
 * transaction for the same event then only ony may be nessecary.
 * @author Mattias
 *
 */
public abstract class TxAwareProActiveListener implements ProActiveEventListener
{
	private NeoService neo;
	private NeoUtil neoUtil;
	
	public TxAwareProActiveListener( NeoService neo )
	{
		this.neo = neo;
		this.neoUtil = new NeoUtil( neo );
	}
	
	private ConcurrentMap<Thread, EventFilter> eventLists =
		new ConcurrentHashMap<Thread, EventFilter>();
	
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
			neoUtil.registerProActiveEventListener( this, event );
		}
		neoUtil.registerProActiveEventListener( this,
			Event.TX_IMMEDIATE_BEGIN );
		neoUtil.registerProActiveEventListener( this,
			Event.TX_IMMEDIATE_ROLLBACK );
		neoUtil.registerProActiveEventListener( this,
			Event.TX_IMMEDIATE_COMMIT );
	}
	
	/**
	 * Unregisters this listener so that no more events reaches this listener.
	 */
	public void unregister()
	{
		for ( Event event : getEvents() )
		{
			neoUtil.unregisterProActiveEventListener( this, event );
		}
		neoUtil.unregisterProActiveEventListener( this,
			Event.TX_IMMEDIATE_BEGIN );
		neoUtil.unregisterProActiveEventListener( this,
			Event.TX_IMMEDIATE_ROLLBACK );
		neoUtil.unregisterProActiveEventListener( this,
			Event.TX_IMMEDIATE_COMMIT );
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
		if ( event == Event.TX_IMMEDIATE_BEGIN )
		{
			this.eventLists.putIfAbsent( Thread.currentThread(), newFilter() );
			txStarted();
		}
		else if ( event == Event.TX_IMMEDIATE_ROLLBACK ||
			event == Event.TX_IMMEDIATE_COMMIT )
		{
			this.eventLists.remove( Thread.currentThread() );
			txEnded( event == Event.TX_IMMEDIATE_COMMIT );
		}
		else
		{
			try
			{
				EventContext context = new EventContext( event, data );
				if ( this.eventLists.get( Thread.currentThread() ).pass(
					context.getEvent(), context.getData() ) )
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
		}
		return result;
	}
}
