package org.neo4j.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.transaction.TransactionManager;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.event.Event;
import org.neo4j.kernel.impl.event.EventData;
import org.neo4j.kernel.impl.event.EventListenerAlreadyRegisteredException;
import org.neo4j.kernel.impl.event.EventListenerNotRegisteredException;
import org.neo4j.kernel.impl.event.EventManager;
import org.neo4j.kernel.impl.event.ProActiveEventListener;
import org.neo4j.kernel.impl.event.ReActiveEventListener;
import org.neo4j.kernel.impl.transaction.LockManager;

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
public class GraphDatabaseUtil
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
	
	private GraphDatabaseService graphDb;
	
	/**
	 * @param graphDb the {@link GraphDatabaseService} to use in methods
	 * which needs it.
	 */
	public GraphDatabaseUtil( GraphDatabaseService graphDb )
	{
		this.graphDb = graphDb;
	}
	
	/**
	 * @return the {@link NeoService} from the constructor.
	 */
	public GraphDatabaseService graphDb()
	{
		return this.graphDb;
	}
	
	private void assertPropertyKeyNotNull( String key )
	{
		if ( key == null )
		{
			throw new IllegalArgumentException( "Property key can't be null" );
		}
	}
	
	/**
	 * Wraps a single {@link PropertyContainer#hasProperty(String)}
	 * in a transaction.
	 * @param container the {@link PropertyContainer}.
	 * @param key the property key.
	 * @return the result from {@link PropertyContainer#hasProperty(String)}.
	 */
	public boolean hasProperty( PropertyContainer container, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = graphDb().beginTx();
		try
		{
			boolean result = container.hasProperty( key );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a single {@link PropertyContainer#getProperty(String)}
	 * in a transaction.
	 * @param container the {@link PropertyContainer}.
	 * @param key the property key.
	 * @return the result from {@link PropertyContainer#getProperty(String)}.
	 */
	public Object getProperty( PropertyContainer container, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = graphDb().beginTx();
		try
		{
			Object result = container.getProperty( key );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a single {@link PropertyContainer#getProperty(String, Object)}
	 * in a transaction.
	 * @param container the {@link PropertyContainer}.
	 * @param key the property key.
	 * @param defaultValue the value to return if the property doesn't exist.
	 * @return the result from
	 * {@link PropertyContainer#getProperty(String, Object)}.
	 */
	public Object getProperty( PropertyContainer container,
		String key, Object defaultValue )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = graphDb().beginTx();
		try
		{
			Object result = container.getProperty( key, defaultValue );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	/**
	 * Wraps a single {@link PropertyContainer#setProperty(String, Object)}
	 * in a transaction.
	 * @param container the {@link PropertyContainer}.
	 * @param key the property key.
	 * @param value the property value.
	 */
	public void setProperty( PropertyContainer container,
		String key, Object value )
	{
		assertPropertyKeyNotNull( key );
		if ( value == null )
		{
			throw new IllegalArgumentException( "Value for property '" +
				key + "' can't be null" );
		}
		
		Transaction tx = graphDb().beginTx();
		try
		{
			container.setProperty( key, value );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public List<Object> getPropertyValues( PropertyContainer container,
		String key )
	{
		Transaction tx = graphDb.beginTx();
		try
		{
			Object value = container.getProperty( key, null );
			List<Object> result = value == null ?
			    new ArrayList<Object>() : propertyValueAsList( value );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}	
	
	public boolean addValueToArray( PropertyContainer container,
		String key, Object value )
	{
		Transaction tx = graphDb.beginTx();
		try
		{
			Collection<Object> values = getPropertyValues( container, key );
			boolean changed = values.contains( value ) ? false :
			    values.add( value );
			if ( changed )
			{
				container.setProperty( key, asPropertyValue( values ) );
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}
	
	public boolean removeValueFromArray( PropertyContainer container,
		String key, Object value )
	{
		Transaction tx = graphDb.beginTx();
		try
		{
			Collection<Object> values = getPropertyValues( container, key );
			boolean changed = values.remove( value );
			if ( changed )
			{
				if ( values.isEmpty() )
				{
					container.removeProperty( key );
				}
				else
				{
					container.setProperty( key, asPropertyValue( values ) );
				}
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Wraps a single {@link PropertyContainer#removeProperty(String)}
	 * in a transaction.
	 * @param container the {@link PropertyContainer}.
	 * @param key the property key.
	 * @return the old value of the property or null if the property didn't
	 * exist
	 */
	public Object removeProperty( PropertyContainer container, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = graphDb().beginTx();
		try
		{
			Object oldValue = container.removeProperty( key );
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
		Transaction tx = graphDb().beginTx();
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
		Transaction tx = graphDb().beginTx();
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
		Transaction tx = graphDb().beginTx();
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
		Transaction tx = graphDb().beginTx();
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
	 * Wraps a {@link GraphDatabaseService#getReferenceNode()} in a transaction.
	 * @return the result from {@link GraphDatabaseService#getReferenceNode()}.
	 */
	public Node getReferenceNode()
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			Node referenceNode = graphDb().getReferenceNode();
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
		Transaction tx = graphDb().beginTx();
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
				node = graphDb().createNode();
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
		return new NodeWrapperRelationshipSet<T>( graphDb(),
			getOrCreateSubReferenceNode( type ), type, clazz );
	}
	
	public EventManager getEventManager()
	{
		return ( ( EmbeddedGraphDatabase )
			graphDb() ).getConfig().getEventModule().getEventManager();
	}
	
	public LockManager getLockManager()
	{
		return ( ( EmbeddedGraphDatabase ) graphDb() ).getConfig().getLockManager();
	}
	
	public TransactionManager getTransactionManager()
	{
		return ( ( EmbeddedGraphDatabase )
			graphDb() ).getConfig().getTxModule().getTxManager();
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
	
	public Object[] propertyValueAsArray( Object propertyValue )
	{
		if ( propertyValue.getClass().isArray() )
		{
			int length = Array.getLength( propertyValue );
			Object[] result = new Object[ length ];
			for ( int i = 0; i < length; i++ )
			{
				result[ i ] = Array.get( propertyValue, i );
			}
			return result;
		}
		else
		{
			return new Object[] { propertyValue };
		}
	}
	
	public List<Object> propertyValueAsList( Object propertyValue )
	{
		return new ArrayList<Object>(
			Arrays.asList( propertyValueAsArray( propertyValue ) ) );
	}
	
	public Object asPropertyValue( Collection<?> values )
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
	
	public Integer incrementAndGetCounter( Node node, String propertyKey )
	{
		Transaction tx = graphDb.beginTx();
		getLockManager().getWriteLock( node );
		try
		{
			int value = ( Integer ) node.getProperty( propertyKey, 0 );
			value++;
			node.setProperty( propertyKey, value );
			tx.success();
			return value;
		}
		finally
		{
			getLockManager().releaseWriteLock( node );
			tx.finish();
		}
	}

	public Integer decrementAndGetCounter( Node node, String propertyKey,
		int notLowerThan )
	{
		Transaction tx = graphDb.beginTx();
		getLockManager().getWriteLock( node );
		try
		{
			int value = ( Integer ) node.getProperty( propertyKey, 0 );
			value--;
			value = value < notLowerThan ? notLowerThan : value;
			node.setProperty( propertyKey, value );
			tx.success();
			return value;
		}
		finally
		{
			getLockManager().releaseWriteLock( node );
			tx.finish();
		}
	}
	
	public String sumNodeContents( Node node )
	{
        StringBuffer result = new StringBuffer();
        for ( Relationship rel : node.getRelationships() )
        {
            if ( rel.getStartNode().equals( node ) )
            {
                result.append( rel.getStartNode() + " ---[" +
                    rel.getType().name() + "]--> " + rel.getEndNode() );
            }
            else
            {
                result.append( rel.getStartNode() + " <--[" +
                    rel.getType().name() + "]--- " + rel.getEndNode() );
            }
            result.append( "\n" );
        }
        for ( String key : node.getPropertyKeys() )
        {
            for ( Object value : propertyValueAsArray(
                node.getProperty( key ) ) )
            {
                result.append( "*" + key + "=[" + value + "]" );
                result.append( "\n" );
            }
        }
        return result.toString();
	}
}
