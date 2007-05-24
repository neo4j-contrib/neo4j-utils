package com.windh.util.neo;

import java.util.Collection;
import java.util.Iterator;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.core.NodeManager;
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
	private static final NeoUtil INSTANCE = new NeoUtil();
	
	public static NeoUtil getInstance()
	{
		return INSTANCE;
	}
	
	public static enum EventType
	{
		PRO_ACTIVE,
		RE_ACTIVE,
	}
	
	private NeoUtil()
	{
	}
	
	public boolean hasProperty( Node node, String key )
	{
		Transaction tx = Transaction.begin();
		try
		{
			return node.hasProperty( key );
		}
		finally
		{
			tx.finish();
		}
	}

	public Object getProperty( Node node, String key )
	{
		Transaction tx = Transaction.begin();
		try
		{
			return node.getProperty( key );
		}
		finally
		{
			tx.finish();
		}
	}

	public Object getProperty( Node node, String key, Object defaultValue )
	{
		Transaction tx = Transaction.begin();
		try
		{
			return node.getProperty( key, defaultValue );
		}
		finally
		{
			tx.finish();
		}
	}

	public void setProperty( Node node, String key, Object value )
	{
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
		Transaction tx = Transaction.begin();
		try
		{
			node.removeProperty( key );
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
			return node.getSingleRelationship( type, direction );
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
			return NodeManager.getManager().getReferenceNode();
		}
		finally
		{
			tx.finish();
		}
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
	public Node getOrCreateSubReferenceNode( RelationshipType type )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Node refNode = getReferenceNode();
			Node node = null;
			Iterator<Relationship> rels =
				refNode.getRelationships( type ).iterator();
			if ( rels.hasNext() )
			{
				Relationship rel = rels.next();
				if ( rels.hasNext() )
				{
					throw new RuntimeException( "Reference area corrupted" +
						" there are more than one" +
						" reference relationships of type " + type );
				}
				node = rel.getEndNode();
			}
			else
			{
				node = NodeManager.getManager().createNode();
				refNode.createRelationshipTo( node, type );
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
	
	public void unregisterReActiveEventListener( ReActiveEventListener listener,
		Event event )
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

	public void registerProActiveEventListener( ProActiveEventListener listener,
		Event event )
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
	
	public void unregisterProActiveEventListener(
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
	public boolean proActiveEvent( Event event, Object data )
	{
		return event( event, data, EventType.PRO_ACTIVE );
	}
	
	/**
	 * Generates a reactive event
	 */
	public void reActiveEvent( Event event, Object data )
	{
		event( event, data, EventType.RE_ACTIVE );
	}
	
	/**
	 * Generates an event either proactively, reactively or both
	 */
	public boolean event( Event event, Object data, EventType... types )
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
