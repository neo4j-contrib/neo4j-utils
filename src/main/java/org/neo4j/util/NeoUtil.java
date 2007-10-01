package org.neo4j.util;

import java.util.Collection;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.EmbeddedNeo;
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
	
	private EmbeddedNeo neo;
	
	/**
	 * @param neo the {@link EmbeddedNeo} to use in methods which needs it.
	 */
	public NeoUtil( EmbeddedNeo neo )
	{
		this.neo = neo;
	}
	
	/**
	 * @return the {@link EmbeddedNeo} from the constructor.
	 */
	public EmbeddedNeo neo()
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
	
	/**
	 * Wraps a single {@link Node#removeProperty(String)} in a transaction.
	 * @param node the {@link Node}.
	 * @param key the property key.
	 */
	public void removeProperty( Node node, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = Transaction.begin();
		try
		{
			node.removeProperty( key );
			tx.success();
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
	
	/**
	 * Wraps a {@link EmbeddedNeo#getReferenceNode()} in a transaction.
	 * @return the result from {@link EmbeddedNeo#getReferenceNode()}.
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
	public <T extends NodeWrapper> Collection<T> getSubReferenceNodeCollection(
		RelationshipType type, Class<T> clazz )
	{
		return new NodeWrapperRelationshipSet<T>(
			getOrCreateSubReferenceNode( type ), type, clazz );
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
			EventManager.getManager().registerReActiveEventListener(
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
	public static void unregisterReActiveEventListener(
		ReActiveEventListener listener, Event event )
	{
		try
		{
			EventManager.getManager().unregisterReActiveEventListener(
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
	public static void registerProActiveEventListener(
		ProActiveEventListener listener, Event event )
	{
		try
		{
			EventManager.getManager().registerProActiveEventListener(
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
	public static void unregisterProActiveEventListener(
		ProActiveEventListener listener, Event event )
	{
		try
		{
			EventManager.getManager().unregisterProActiveEventListener(
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
	public static boolean proActiveEvent( Event event, Object data )
	{
		return event( event, data, EventType.PRO_ACTIVE );
	}
	
	/**
	 * Convenience method for generating a re-active event.
	 * @param event the event type.
	 * @param data the event data to send (it gets wrapped in an
	 * {@link EventData}).
	 */
	public static void reActiveEvent( Event event, Object data )
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
	public static boolean event( Event event, Object data, EventType... types )
	{
		boolean result = true;
		EventData eventData = new EventData( data );
		for ( EventType type : types )
		{
			if ( type == EventType.PRO_ACTIVE )
			{
				result = EventManager.getManager().generateProActiveEvent(
					event, eventData );
			}
			else
			{
				EventManager.getManager().generateReActiveEvent(
					event, eventData );
			}
		}
		return result;
	}
}
