package com.windh.util.neo;

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
	public static enum EventType
	{
		PRO_ACTIVE,
		RE_ACTIVE,
	}
	
	private EmbeddedNeo neo;
	
	public NeoUtil( EmbeddedNeo neo )
	{
		this.neo = neo;
	}
	
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
	 * @param type
	 * @return
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
	
	public Collection<? extends NodeWrapper> getSubReferenceNodeCollection(
		RelationshipType type, Class<? extends NodeWrapper> clazz )
	{
		return new NodeWrapperRelationshipSet(
			getOrCreateSubReferenceNode( type ), type, clazz );
	}
	
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
	 * Generates a proactive event
	 */
	public static boolean proActiveEvent( Event event, Object data )
	{
		return event( event, data, EventType.PRO_ACTIVE );
	}
	
	/**
	 * Generates a reactive event
	 */
	public static void reActiveEvent( Event event, Object data )
	{
		event( event, data, EventType.RE_ACTIVE );
	}
	
	/**
	 * Generates an event either proactively, reactively or both
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
