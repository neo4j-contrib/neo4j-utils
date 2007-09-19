package org.neo4j.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import org.neo4j.impl.event.EventManager;
import org.neo4j.impl.event.ProActiveEventListener;

/**
 * A layer on top of EventManager. EventManager delegates the events on to its
 * listeners immediately when generated (or rather immediately if they are
 * reactive). That doesn't hold water when transactions comes into the picture.
 * Events mustn't reach its listeners of the transaction fails, therefore
 * all events will be sent after a successful transaction. That is what
 * TransactionEventManager does. So instead of registering an event listener
 * at EventManager, register it at TransactionEventManager instead to gain this
 * functionality.
 * 
 * @author Mattias
 *
 */
public class TransactionEventManager
{
	/**
	 * Will be sent to all the listeners with the EventData consisting of
	 * a EventContext[] with all the events in the (successful) transaction
	 */
	public static final Event TX_EVENT_BUFFER =
		new TransactionEvent( "TX_EVENT_BUFFER" );

	private static TransactionEventManager instance =
		new TransactionEventManager();
		
	public static TransactionEventManager getManager()
	{
		return instance;
	}
	
	private ConcurrentMap < Integer, TransactionHook > transactions =
		new ConcurrentHashMap();
	private InternalEventListener internalListener =
		new InternalEventListener();
	private Map < Event, Set < ProActiveEventListener > > listeners =
		new HashMap();
	private Set < Event > registeredEvents = new HashSet();
	private ConcurrentMap < Thread, Integer > threadToTxId =
		new ConcurrentHashMap();
	private TransactionHookFactory hookFactory = new TransactionHookFactory()
	{
		public TransactionHook newHook( int txId )
		{
			return new TransactionHook( txId );
		}
	};
	
	private TransactionEventManager()
	{
		NeoUtil.registerProActiveEventListener(
			internalListener, Event.TX_IMMEDIATE_BEGIN );
		NeoUtil.registerProActiveEventListener(
			internalListener, Event.TX_IMMEDIATE_COMMIT );
		NeoUtil.registerProActiveEventListener(
			internalListener, Event.TX_IMMEDIATE_ROLLBACK );
	}
	
	/**
	 * Contains an event buffer with all the events generated in this tx.
	 * TransactionHooks are created when the first business logic event gets
	 * generated, not nessecarily when the tx begins.
	 * @param id the tx id
	 * @return the TransactionHook with the event buffer, or null if no
	 * TransactionHook was associated with this id
	 */
	private TransactionHook getTransactionHook( int id )
	{
		return this.transactions.get( id );
	}
	
	private TransactionHook createTransactionHook( int id )
	{
		TransactionHook hook = this.hookFactory.newHook( id );
		TransactionHook previous = this.transactions.putIfAbsent( id, hook );
		if ( previous != null )
		{
			throw new RuntimeException( "There was a previous tx id " + id );
		}
		return hook;
	}
	
	/**
	 * Removes a TransactionHook. This is done when a tx is finished and
	 * its events have been sent.
	 * @param id the tx id
	 * @return the removed TransactionHook
	 */
	private void removeTransactionHook( TransactionHook hook )
	{
		if ( !this.transactions.remove( hook.getId(), hook ) )
		{
			throw new RuntimeException( "Couldn't remove tx hook " +
				hook.getId() );
		}
	}
	
	/**
	 * Gets all the listeners which listens to <CODE>event</CODE>
	 * @param event the Event
	 * @return a Set containing the listeners.
	 */
	private Set < ProActiveEventListener > getListenersSet( Event event )
	{
		synchronized ( listeners )
		{
			Set < ProActiveEventListener > listenersForEvent =
				listeners.get( event );
			if ( listenersForEvent == null )
			{
				listenersForEvent = new HashSet();
				listeners.put( event, listenersForEvent );
			}
			return listenersForEvent;
		}
	}
	
	public void setTransactionHookFactory( TransactionHookFactory factory )
	{
		this.hookFactory = factory;
	}
	
	/**
	 * Registers a listener to listen for <CODE>event</CODE> events.
	 * <CODE>listener</CODE> will receive the events after a successfull
	 * transaction commit.
	 *
	 * @param listener the listener to listen for <CODE>event</CODE>.
	 * @param event the type of event to listen for.
	 */
	public void registerEventListener( ProActiveEventListener listener,
		Event event )
	{
		synchronized ( listeners )
		{
			Set listenersForEvent = getListenersSet( event );
			if ( listenersForEvent.contains( listener ) )
			{
				throw new RuntimeException( "Listener " + listener +
					" already registered for " + event );
			}
			listenersForEvent.add( listener );
		}
		registerInternalEventListener( event );
	}
	
	private void registerInternalEventListener( Event event )
	{
		try
		{
			// Only have to register an event for the internal listener once.
			synchronized ( registeredEvents )
			{
				if ( !registeredEvents.contains( event ) )
				{
					EventManager.getManager().registerProActiveEventListener(
						internalListener, event );
					registeredEvents.add( event );
				}
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Unregisters a listener from <CODE>event</CODE> events.
	 *
	 * @param listener the listener to stop listen to <CODE>event</CODE>.
	 * @param event the type of event to stop listen to.
	 */
	public void unregisterEventListener( ProActiveEventListener listener,
		Event event )
	{
		synchronized ( listeners )
		{
			Set < ProActiveEventListener > listenersForEvent =
				getListenersSet( event );
			if ( !listenersForEvent.contains( listener ) )
			{
				throw new RuntimeException( "Listener " + listener +
					" not registered for " + event );
			}
			listenersForEvent.remove( listener );
		}
		unregisterInternalEventListener( event );
	}
	
	private void unregisterInternalEventListener( Event event )
	{
		try
		{
			// Only have to unregister an event for the internal listener once.
			synchronized ( registeredEvents )
			{
				if ( registeredEvents.contains( event ) )
				{
					EventManager.getManager().unregisterProActiveEventListener(
						internalListener, event );
					registeredEvents.remove( event );
				}
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	/**
	 * General method for sending an event from a TransactionHook.
	 * All the listeners for the given event are looped through and those
	 * who require immediate delivery or not (depending on the
	 * <CODE>immediate</CODE> argument) gets the event delivered.
	 * @param event class containing both Event and EventData
	 * @param immediate which listeners to resceive this event, those
	 * who require immediate delivery or not.
	 */
	private void sendEvent( EventContext event/*, boolean immediate*/ )
	{
		synchronized ( listeners )
		{
			for ( ProActiveEventListener listener :
				getListenersSet( event.getEvent() ) )
			{
				safeSendEvent( listener, event.getEvent(),
					event.getData() );
			}
		}
	}
	
	/**
	 * Sends all the events in a tx for a specific listener as one chunk.
	 * This is performed after all the events have been sent individually.
	 * @param listener the listenter to receive the chunk
	 * @param events the events this listener will receive
	 */
	private void sendEventBuffer( ProActiveEventListener listener,
		EventContext[] events )
	{
		safeSendEvent( listener, TX_EVENT_BUFFER, new EventData( events ) );
	}
	
	private void safeSendEvent( ProActiveEventListener listener,
		Event event, EventData data )
	{
		try
		{
			listener.proActiveEventReceived( event, data );
		}
		catch ( Exception e )
		{
			//TODO Log
		}
	}
	
	private void mapThreadToTxId( Thread thread, int id )
	{
		if ( this.threadToTxId.putIfAbsent( thread, id ) != null )
		{
			throw new RuntimeException( "Already in tx " + id );
		}
	}
	
	private int getTxIdForThread( Thread thread )
	{
		Integer result = this.threadToTxId.get( thread );
		if ( result == null )
		{
			throw new RuntimeException( "No tx id found for " + thread );
		}
		return result;
	}
	
	private int unmapThreadFromTxId( Thread thread )
	{
		Integer id = this.threadToTxId.remove( thread );
		if ( id == null )
		{
			throw new RuntimeException( "No tx id found for " + thread );
		}
		return id;
	}
	
	/**
	 * This method is not for normal use... just for testing purposes.
	 */
	public void flushEvents()
	{
		int txId = getTxIdForThread( Thread.currentThread() );
		TransactionHook hook = TransactionEventManager.getManager(
			).getTransactionHook( txId );
		if ( hook == null )
		{
			return;
		}
		hook.flushEvents();
	}
	
	private static class EventList
	{
		private Map < ProActiveEventListener, List < EventContext > >
			eventsPerListener = new HashMap();
		
		void sendEvent( EventContext context )
		{
			TransactionEventManager.getManager().sendEvent( context );
			bufferEvent( context );
		}
		
		private void bufferEvent( EventContext context )
		{
			for ( ProActiveEventListener listener :
				TransactionEventManager.getManager().getListenersSet(
				context.getEvent() ) )
			{
				List < EventContext > eventList =
					eventsPerListener.get( listener );
				if ( eventList == null )
				{
					eventList = new ArrayList();
					eventsPerListener.put( listener, eventList );
				}
				eventList.add( context );
			}
		}
		
		void sendEventBuffer()
		{
			for ( ProActiveEventListener listener : eventsPerListener.keySet() )
			{
				List < EventContext > list = eventsPerListener.get( listener );
				EventContext[] events = list.toArray(
					new EventContext[ list.size() ] );
				TransactionEventManager.getManager().sendEventBuffer(
					listener, events ); 
			}
		}
	}
	
	public static interface TransactionHookFactory
	{
		public TransactionHook newHook( int txId );
	}
	
	public static class TransactionHook
	{
		private int txId;
		private List < EventContext > events;
		private EventList eventList;
		
		TransactionHook( int txId )
		{
			this.txId = txId;
		}
		
		int getId()
		{
			return this.txId;
		}
		
		protected void sendEvents()
		{
			if ( this.events == null )
			{
				// No events in the queue
				return;
			}
			
			try
			{
				beforeSendingEvents();
				this.eventList = new EventList();
				for ( EventContext context : events )
				{
					this.eventList.sendEvent( context );
				}
				this.eventList.sendEventBuffer();
			}
			finally
			{
				afterSendingEvents();
			}
		}
		
		/**
		 * Used in tests only
		 */
		private void flushEvents()
		{
			sendEvents();
			this.events = null;
		}
		
		protected void beforeSendingEvents()
		{
		}
		
		protected void afterSendingEvents()
		{
		}
		
		protected void queueEvent( Event event, EventData data )
		{
			if ( this.events == null )
			{
				this.events = new ArrayList();			
			}
			this.events.add( new EventContext( event, data ) );
		}
	}
	
	/**
	 * Ok this internal listener listens to reactive and proactive
	 * events. Reactive are the kernels events: TX_ROLLBACK and TX_COMMIT.
	 * Proactive are TX_IMMEDIATE_BEGIN and all else which gets registered via
	 * TransactionManager.registerEventListener.
	 * 
	 * The life cycle of a TransactionHook goes like this:
	 * 
	 *  1. TX_IMMEDIATE_BEGIN gets received and the tx id gets mapped to the
	 * 		current thread for future use.
	 *  2. Business logic events (proactive) gets received
	 *  3. The transaction is successfully committed
	 * 	4. The reactive (TX_COMMIT from the kernel) reaches the
	 * 		transaction hook and all the events registered during the
	 * 		transaction will be sent out to its listeners.
	 */
	private class InternalEventListener
		implements /*ReActiveEventListener, */ProActiveEventListener
	{
		/*public void reActiveEventReceived( Event event, EventData data )
		{
			int txId = ( Integer ) data.getData();
			TransactionHook hook = getTransactionHook( txId );
			if ( hook == null )
			{
				return;
			}
			
			try
			{
				if ( event == Event.TX_COMMIT )
				{
					hook.sendEvents();
				}
				// If it is the TX_ROLLBACK event, then don't bother sending
			}
			finally
			{
				removeTransactionHook( hook );
			}
		}*/
		
		public boolean proActiveEventReceived( Event event, EventData data )
		{
			if ( event == Event.TX_IMMEDIATE_BEGIN )
			{
				// A transaction was started NOW
				int txId = ( Integer ) data.getData();
				TransactionEventManager.getManager().createTransactionHook(
					txId );
				TransactionEventManager.getManager().mapThreadToTxId(
					Thread.currentThread(), txId );
			}
			else if ( event == Event.TX_IMMEDIATE_ROLLBACK ||
				event == Event.TX_IMMEDIATE_COMMIT )
			{
				int txId = unmapThreadFromTxId( Thread.currentThread() );
				TransactionHook hook = TransactionEventManager.getManager(
					).getTransactionHook( txId );
				if ( hook != null )
				{
					removeTransactionHook( hook );
					if ( event == Event.TX_IMMEDIATE_COMMIT )
					{
						hook.sendEvents();
					}
				}
			}
			else
			{
				// Queue an event from the BL layer
				int txId = TransactionEventManager.getManager(
					).getTxIdForThread( Thread.currentThread() );
				TransactionHook hook = TransactionEventManager.getManager(
					).getTransactionHook( txId );
				if ( hook == null )
				{
					throw new RuntimeException( "No tx hook " + txId );
				}
				hook.queueEvent( event, data );
			}
			return true;
		}
	}
	
	protected static class TransactionEvent extends Event
	{
		TransactionEvent( String name )
		{
			super( name );
		}
	}
}
