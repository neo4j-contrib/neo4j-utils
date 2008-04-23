package org.neo4j.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.transaction.TransactionManager;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import org.neo4j.impl.event.EventListenerAlreadyRegisteredException;
import org.neo4j.impl.event.EventListenerNotRegisteredException;
import org.neo4j.impl.event.EventManager;
import org.neo4j.impl.event.ProActiveEventListener;
import org.neo4j.impl.event.ReActiveEventListener;
import org.neo4j.impl.transaction.LockManager;

/**
 * Contains some convenience methods for f.ex. set/get/remove one property
 * wrapped in a transaction.
 * 
 * Some event helper methods.
 * 
 * Reference nodes helper methods.
 * 
 * EventManager register/unregister methods are a pain in the ass, one throws
 * two exceptions and the other throws one. Added some helper methods here.
 * 
 * @author mathew
 *
 */
public class NeoUtil
{
	/**
	 * The type of event, neo supports pro-active and re-active.
	 */
	public static enum EventType
	{
		/**
		 * A pro-active event, which means that the event reaches its targets
		 * synchronously.
		 */
		PRO_ACTIVE,
		
		/**
		 * A re-active event, which means that the event may reach its targets
		 * at a later time (a separate thread in neo).
		 */
		RE_ACTIVE,
	}
	
	private NeoService neo;
	
	/**
	 * @param neo the {@link NeoService} to use in methods which needs it.
	 */
	public NeoUtil( NeoService neo )
	{
		this.neo = neo;
	}
	
	/**
	 * @return the {@link NeoService} from the constructor.
	 */
	public NeoService neo()
	{
		return this.neo;
	}
	
	private void assertPropertyKeyNotNull( String key )
	{
		if ( key == null )
		{
			throw new IllegalArgumentException( "Property key can't be null" );
		}
	}
	
	/**
	 * Wraps a single {@link Node#hasProperty(String)} in a transaction.
	 * @param node the {@link Node}.
	 * @param key the property key.
	 * @return the result from {@link Node#hasProperty(String)}.
	 */
	public boolean hasProperty( Node node, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = Transaction.begin();
		try
		{
			boolean result = node.hasProperty( key );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a single {@link Node#getProperty(String)} in a transaction.
	 * @param node the {@link Node}.
	 * @param key the property key.
	 * @return the result from {@link Node#getProperty(String)}.
	 */
	public Object getProperty( Node node, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = Transaction.begin();
		try
		{
			Object result = node.getProperty( key );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a single {@link Node#getProperty(String, Object)} in a transaction.
	 * @param node the {@link Node}.
	 * @param key the property key.
	 * @param defaultValue the value to return if the property doesn't exist.
	 * @return the result from {@link Node#getProperty(String, Object)}.
	 */
	public Object getProperty( Node node, String key, Object defaultValue )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = Transaction.begin();
		try
		{
			Object result = node.getProperty( key, defaultValue );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a single {@link Node#setProperty(String, Object)} in a transaction.
	 * @param node the {@link Node}.
	 * @param key the property key.
	 * @param value the property value.
	 */
	public void setProperty( Node node, String key, Object value )
	{
		assertPropertyKeyNotNull( key );
		if ( value == null )
		{
			throw new IllegalArgumentException( "Value for property '" +
				key + "' can't be null" );
		}
		
		Transaction tx = Transaction.begin();
		try
		{
			node.setProperty( key, value );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public List<Object> getPropertyValues( Node node, String key )
	{
		Transaction tx = neo.beginTx();
		try
		{
			Object value = node.getProperty( key, null );
			List<Object> result = value == null ?
			    new ArrayList<Object>() : neoPropertyAsList( value );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}	
	
	public boolean addValueToArray( Node node, String key, Object value )
	{
		Transaction tx = neo.beginTx();
		try
		{
			Collection<Object> values = getPropertyValues( node, key );
			boolean result = values.add( value );
			node.setProperty( key, asNeoProperty( values ) );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	public boolean removeValueFromArray( Node node, String key, Object value )
	{
		Transaction tx = neo.beginTx();
		try
		{
			Collection<Object> values = getPropertyValues( node, key );
			boolean result = values.remove( value );
			if ( values.isEmpty() )
			{
				node.removeProperty( key );
			}
			else
			{
				node.setProperty( key, asNeoProperty( values ) );
			}
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Wraps a single {@link Node#removeProperty(String)} in a transaction.
	 * @param node the {@link Node}.
	 * @param key the property key.
	 * @return the old value of the property or null if the property didn't
	 * exist
	 */
	public Object removeProperty( Node node, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = Transaction.begin();
		try
		{
			Object oldValue = node.removeProperty( key );
			tx.success();
			return oldValue;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Wraps a {@link Node#getSingleRelationship(RelationshipType, Direction)}
	 * in a transaction.
	 * @param node the {@link Node}.
	 * @param type the {@link RelationshipType}
	 * @param direction the {@link Direction}.
	 * @return the result from
	 * {@link Node#getSingleRelationship(RelationshipType, Direction)}.
	 */
	public Relationship getSingleRelationship( Node node, RelationshipType type,
		Direction direction )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship singleRelationship =
				node.getSingleRelationship( type, direction );
			tx.success();
			return singleRelationship;
		}
		finally
		{
			tx.finish();
		}
	}
	
	public Node getSingleOtherNode( Node node, RelationshipType type,
		Direction direction )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship rel = getSingleRelationship( node, type, direction );
			Node result = rel == null ? null : rel.getOtherNode( node );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	public Relationship getSingleRelationship( Node node,
		RelationshipType type )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Iterator<Relationship> itr =
				node.getRelationships( type ).iterator();
			Relationship rel = null;
			if ( itr.hasNext() )
			{
				rel = itr.next();
				if ( itr.hasNext() )
				{
					throw new RuntimeException( node + " has more than one " +
						"relationship of type '" + type.name() + "'" ); 
				}
			}
			tx.success();
			return rel;
		}
		finally
		{
			tx.finish();
		}
	}
	
	public Node getSingleOtherNode( Node node, RelationshipType type )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship rel = getSingleRelationship( node, type );
			Node result = rel == null ? null : rel.getOtherNode( node );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a {@link NeoService#getReferenceNode()} in a transaction.
	 * @return the result from {@link NeoService#getReferenceNode()}.
	 */
	public Node getReferenceNode()
	{
		Transaction tx = Transaction.begin();
		try
		{
			Node referenceNode = neo().getReferenceNode();
			tx.success();
			return referenceNode;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * @see #getOrCreateSubReferenceNode(RelationshipType, Direction) .
	 * @param type the relationship type.
	 * @return the sub-reference node for {@code type}.
	 */
	public Node getOrCreateSubReferenceNode( RelationshipType type )
	{
		return this.getOrCreateSubReferenceNode( type, Direction.OUTGOING );
	}

	/**
	 * Tries to get a sub reference node with relationship type
	 * <code>type</code>. If it doesn't exist, it is created. There can be
	 * only one of any given type.
	 * 
	 * [NodeSpaceReferenceNode] -- type --> [SubReferenceNode]
	 * 
	 * @param type the relationship type.
	 * @param direction the direction of the relationship.
	 * @return the sub-reference node.
	 */
	public Node getOrCreateSubReferenceNode( RelationshipType type,
		Direction direction )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Node referenceNode = getReferenceNode();
			Node node = null;
			Relationship singleRelationship =
				referenceNode.getSingleRelationship( type, direction );
			if ( singleRelationship != null )
			{
				node = singleRelationship.getOtherNode( referenceNode );
			}
			else
			{
				node = neo().createNode();
				referenceNode.createRelationshipTo( node, type );
			}
			
			tx.success();
			return node;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Returns the sub-reference node for a relationship type as a collection.
	 * @param <T> the instance class for objects in the result collection.
	 * @param type the relationship type for the sub-reference node.
	 * @param clazz the instance class for objects in the result collection.
	 * @return the sub-reference node for a relationship type as a collection.
	 */
	public <T extends NodeWrapper> Collection<T>
		getSubReferenceNodeCollection( RelationshipType type, Class<T> clazz )
	{
		return new NodeWrapperRelationshipSet<T>(
			getOrCreateSubReferenceNode( type ), type, clazz );
	}
	
	public EventManager getEventManager()
	{
		return ( ( EmbeddedNeo )
			neo() ).getConfig().getEventModule().getEventManager();
	}
	
	public LockManager getLockManager()
	{
		return ( ( EmbeddedNeo ) neo() ).getConfig().getLockManager();
	}
	
	public TransactionManager getTransactionManager()
	{
		return ( ( EmbeddedNeo )
			neo() ).getConfig().getTxModule().getTxManager();
	}
	
	/**
	 * Convenience method for registering a re-active event listener.
	 * Instead of throwing declared exceptions it throws runtime exceptions.
	 * @param listener the listener to register with the event.
	 * @param event the event to register the listener with.
	 */
	public void registerReActiveEventListener( ReActiveEventListener listener,
		Event event )
	{
		try
		{
			getEventManager().registerReActiveEventListener( listener, event );
		}
		catch ( EventListenerAlreadyRegisteredException e )
		{
			throw new RuntimeException( e );
		}
		catch ( EventListenerNotRegisteredException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Convenience method for unregistering a re-active event listener.
	 * Instead of throwing declared exceptions it throws runtime exceptions.
	 * @param listener the listener to unregister from the event.
	 * @param event the event to unregister the listener from.
	 */
	public void unregisterReActiveEventListener(
		ReActiveEventListener listener, Event event )
	{
		try
		{
			getEventManager().unregisterReActiveEventListener(
				listener, event );
		}
		catch ( EventListenerNotRegisteredException e )
		{
			throw new RuntimeException( e );
		}
	}

	/**
	 * Convenience method for registering a pro-active event listener.
	 * Instead of throwing declared exceptions it throws runtime exceptions.
	 * @param listener the listener to register with the event.
	 * @param event the event to register the listener with.
	 */
	public void registerProActiveEventListener(
		ProActiveEventListener listener, Event event )
	{
		try
		{
			getEventManager().registerProActiveEventListener(
				listener, event );
		}
		catch ( EventListenerAlreadyRegisteredException e )
		{
			throw new RuntimeException( e );
		}
		catch ( EventListenerNotRegisteredException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Convenience method for unregistering a re-active event listener.
	 * Instead of throwing declared exceptions it throws runtime exceptions.
	 * @param listener the listener to unregister from the event.
	 * @param event the event to unregister the listener from.
	 */
	public void unregisterProActiveEventListener(
		ProActiveEventListener listener, Event event )
	{
		try
		{
			getEventManager().unregisterProActiveEventListener(
				listener, event );
		}
		catch ( EventListenerNotRegisteredException e )
		{
			throw new RuntimeException( e );
		}
	}

	/**
	 * Convenience method for generating a pro-active event.
	 * @param event the event type.
	 * @param data the event data to send (it gets wrapped in an
	 * {@link EventData}).
	 * @return the result of
	 * {@link EventManager#generateProActiveEvent(Event, EventData)}.
	 */
	public boolean proActiveEvent( Event event, Object data )
	{
		return event( event, data, EventType.PRO_ACTIVE );
	}
	
	/**
	 * Convenience method for generating a re-active event.
	 * @param event the event type.
	 * @param data the event data to send (it gets wrapped in an
	 * {@link EventData}).
	 */
	public void reActiveEvent( Event event, Object data )
	{
		event( event, data, EventType.RE_ACTIVE );
	}
	
	/**
	 * Convenience method for generating an event (pro-active, re-active or
	 * both)
	 * @param event the event type.
	 * @param data the event data to send (it gets wrapped in an
	 * {@link EventData}).
	 * @param types the types of event to send.
	 * @return the result of
	 * {@link EventManager#generateProActiveEvent(Event, EventData)} if any
	 * of the types is {@link EventType#PRO_ACTIVE}, else {@code true}.
	 */
	public boolean event( Event event, Object data, EventType... types )
	{
		boolean result = true;
		EventData eventData = new EventData( data );
		EventManager eventManager = getEventManager();
		for ( EventType type : types )
		{
			if ( type == EventType.PRO_ACTIVE )
			{
				result = eventManager.generateProActiveEvent(
					event, eventData );
			}
			else
			{
				eventManager.generateReActiveEvent( event, eventData );
			}
		}
		return result;
	}
	
	public Object[] neoPropertyAsArray( Object neoPropertyValue )
	{
		if ( neoPropertyValue.getClass().isArray() )
		{
			int length = Array.getLength( neoPropertyValue );
			Object[] result = new Object[ length ];
			for ( int i = 0; i < length; i++ )
			{
				result[ i ] = Array.get( neoPropertyValue, i );
			}
			return result;
		}
		else
		{
			return new Object[] { neoPropertyValue };
		}
	}
	
	public List<Object> neoPropertyAsList( Object neoPropertyValue )
	{
		return new ArrayList<Object>(
			Arrays.asList( neoPropertyAsArray( neoPropertyValue ) ) );
	}
	
	public Object asNeoProperty( Collection<?> values )
	{
		if ( values.isEmpty() )
		{
			return null;
		}
		if ( values.size() == 1 )
		{
			return values.iterator().next();
		}
		
		Object array = Array.newInstance( values.iterator().next().getClass(),
			values.size() );
		int index = 0;
		for ( Object value : values )
		{
			Array.set( array, index++, value );
		}
		return array;
	}
}
