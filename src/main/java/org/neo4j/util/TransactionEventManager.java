package org.neo4j.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.neo4j.api.core.NeoService;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
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
 */
public class TransactionEventManager
{
	/**
	 * Will be sent to all the listeners with the EventData consisting of
	 * a EventContext[] with all the events in the (successful) transaction
	 */
	public static final Event TX_EVENT_BUFFER =
		new TransactionEvent( "TX_EVENT_BUFFER" );

	private ConcurrentMap<Transaction, TransactionHook> transactions =
		new ConcurrentHashMap<Transaction, TransactionHook>();
	private InternalEventListener internalListener =
		new InternalEventListener();
	private Map<Event, Set<ProActiveEventListener>> listeners =
		new HashMap<Event, Set<ProActiveEventListener>>();
	private Set<Event> registeredEvents = new HashSet<Event>();
	private NeoUtil neoUtil;
	private TransactionHookFactory hookFactory = new TransactionHookFactory()
	{
		public TransactionHook newHook( Transaction tx )
		{
			return new TransactionHook( tx );
		}
	};
	
	public TransactionEventManager( NeoService neo )
	{
		this.neoUtil = new NeoUtil( neo );
	}
	
	/**
	 * Contains an event buffer with all the events generated in this tx.
	 * TransactionHooks are created when the first business logic event gets
	 * generated, not necessarily when the tx begins.
	 * @param id the tx id
	 * @return the TransactionHook with the event buffer, or null if no
	 * TransactionHook was associated with this id
	 */
	private TransactionHook getTransactionHook( Transaction tx )
	{
		return this.transactions.get( tx );
	}
	
	private TransactionHook createTransactionHook( Transaction tx )
	{
		TransactionHook hook = this.hookFactory.newHook( tx );
		TransactionHook previous = this.transactions.putIfAbsent( tx, hook );
		if ( previous != null )
		{
			throw new RuntimeException( "There was a previous tx id " + tx );
		}
		return hook;
	}
	
	/**
	 * Removes a TransactionHook. This is done when a tx is finished and
	 * its events have been sent.
	 * @param id the transaction id.
	 */
	private void removeTransactionHook( TransactionHook hook )
	{
		if ( !this.transactions.remove( hook.tx, hook ) )
		{
			throw new RuntimeException( "Couldn't remove tx hook " +
				hook.tx );
		}
	}
	
	/**
	 * Gets all the listeners which listens to <CODE>event</CODE>
	 * @param event the Event
	 * @return a Set containing the listeners.
	 */
	private Set<ProActiveEventListener> getListenersSet( Event event )
	{
		synchronized ( listeners )
		{
			Set<ProActiveEventListener> listenersForEvent =
				listeners.get( event );
			if ( listenersForEvent == null )
			{
				listenersForEvent = new HashSet<ProActiveEventListener>();
				listeners.put( event, listenersForEvent );
			}
			return listenersForEvent;
		}
	}
	
	/**
	 * Sets the factory instance for creating transaction hooks.
	 * @param factory
	 */
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
			Set<ProActiveEventListener> listenersForEvent =
				getListenersSet( event );
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
		// Only have to register an event for the internal listener once.
		synchronized ( registeredEvents )
		{
			if ( !registeredEvents.contains( event ) )
			{
				neoUtil.registerProActiveEventListener(
					internalListener, event );
				registeredEvents.add( event );
			}
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
		// Only have to unregister an event for the internal listener once.
		synchronized ( registeredEvents )
		{
			if ( registeredEvents.contains( event ) )
			{
				neoUtil.unregisterProActiveEventListener(
					internalListener, event );
				registeredEvents.remove( event );
			}
		}
	}

	/**
	 * General method for sending an event from a TransactionHook.
	 * All the listeners for the given event are looped through and those
	 * who require immediate delivery or not (depending on the
	 * <CODE>immediate</CODE> argument) gets the event delivered.
	 * @param event class containing both Event and EventData
	 * @param immediate which listeners to receive this event, those
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
	
	/**
	 * This method is not for normal use... just for testing purposes.
	 */
	public void flushEvents()
	{
		try
        {
            TransactionHook hook = getTransactionHook(
                neoUtil.getTransactionManager().getTransaction() );
            if ( hook == null )
            {
            	return;
            }
            hook.flushEvents();
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( e );
        }
	}
	
	private class EventList
	{
		private Map<ProActiveEventListener, List<EventContext>>
			eventsPerListener =
				new HashMap<ProActiveEventListener, List<EventContext>>();
		
		void sendEvent( EventContext context )
		{
			TransactionEventManager.this.sendEvent( context );
			bufferEvent( context );
		}
		
		private void bufferEvent( EventContext context )
		{
			for ( ProActiveEventListener listener :
				TransactionEventManager.this.getListenersSet(
				context.getEvent() ) )
			{
				List<EventContext> eventList =
					eventsPerListener.get( listener );
				if ( eventList == null )
				{
					eventList = new ArrayList<EventContext>();
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
				TransactionEventManager.this.sendEventBuffer(
					listener, events ); 
			}
		}
	}
	
	/**
	 * A simple factory interface for creating {@link TransactionHook}
	 * instances.
	 */
	public static interface TransactionHookFactory
	{
		/**
		 * Creates a new transaction hook for a given transaction.
		 * @param tx the transaction to use.
		 * @return a new transaction hook for a given transaction.
		 */
		public TransactionHook newHook( Transaction tx );
	}
	
	/**
	 * A code hook to execute when a transaction is committed.
	 */
	public class TransactionHook implements Synchronization
	{
		private Transaction tx;
		private List < EventContext > events;
		private EventList eventList;
		
		TransactionHook( Transaction tx )
		{
			this.tx = tx;
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
				this.events = new ArrayList<EventContext>();
			}
			this.events.add( new EventContext( event, data ) );
		}

        public void afterCompletion( int status )
        {
            removeTransactionHook( this );
            if ( status != Status.STATUS_COMMITTED )
            {
                return;
            }
            
            Thread thread = new Thread()
            {
                @Override
                public void run()
                {
                    flushEvents();
                }
            };
            thread.start();
            try
            {
                thread.join();
            }
            catch ( InterruptedException e )
            {
                // Ok
            }
        }

        public void beforeCompletion()
        {
        }
	}
	
	private class InternalEventListener
		implements ProActiveEventListener
	{
        public boolean proActiveEventReceived( Event event, EventData data )
        {
            try
            {
                Transaction tx =
                    neoUtil.getTransactionManager().getTransaction();
                TransactionHook hook = getTransactionHook( tx );
                if ( hook == null )
                {
                    hook = createTransactionHook( tx );
                    tx.registerSynchronization( hook );
                }
                hook.queueEvent( event, data );
            }
            catch ( SystemException e )
            {
                throw new RuntimeException( e );
            }
            catch ( RollbackException e )
            {
                throw new RuntimeException( e );
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
