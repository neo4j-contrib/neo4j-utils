package org.neo4j.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	 * @return the {@link GraphDatabaseService} from the constructor.
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
		Object value = container.getProperty( key, null );
		return value == null ?
		    new ArrayList<Object>() : propertyValueAsList( value );
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
	
	public Node getSingleOtherNode( Node node, RelationshipType type,
		Direction direction )
	{
		Relationship rel = node.getSingleRelationship( type, direction );
		return rel == null ? null : rel.getOtherNode( node );
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
			Node referenceNode = graphDb.getReferenceNode();
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
	
	public LockManager getLockManager()
	{
		return ( ( EmbeddedGraphDatabase ) graphDb() ).getConfig().getLockManager();
	}
	
	public TransactionManager getTransactionManager()
	{
		return ( ( EmbeddedGraphDatabase )
			graphDb() ).getConfig().getTxModule().getTxManager();
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
	
    /**
     * TODO Use RelationshipExpander later on
     * 
     * Looks to see if there exists a relationship between two given nodes.
     * It starts iterating over relationships from the node which you think
     * has the least amount of relationships of the two nodes. If it has
     * iterated over a specified amount of relationships without finding
     * a match it spawns a new thread which will iterate over relationships
     * from the other node in the reverse direction. The loop will exit
     * when/if any thread finds a match or when there are no more relationships
     * left to iterate over.
     * 
     * @param nodeYouThinkHasLeastRelationships the node of the two which you
     * think/know has the least amount of relationship (that will be
     * iterated over).
     * @param secondNode the node of the two you suspect has more relationships
     * than the first node.
     * @param type the type of relationships to iterate over.
     * @param direction the direction from the first node that you expect for
     * your relationships.
     * @return if there's any relationship with the given criterias (type,
     * direction, filter) from the first node to the second node.
     */
    public boolean relationshipExistsBetween( Node nodeYouThinkHasLeastRelationships,
            Node secondNode, RelationshipType type, Direction direction )
    {
        return relationshipExistsBetween( nodeYouThinkHasLeastRelationships, secondNode, type,
                direction, null, 50 );
    }
    
	/**
	 * TODO Use RelationshipExpander later on
	 * 
	 * Looks to see if there exists a relationship between two given nodes.
	 * It starts iterating over relationships from the node which you think
	 * has the least amount of relationships of the two nodes. If it has
	 * iterated over a specified amount of relationships without finding
	 * a match it spawns a new thread which will iterate over relationships
	 * from the other node in the reverse direction. The loop will exit
	 * when/if any thread finds a match or when there are no more relationships
	 * left to iterate over.
	 * 
	 * @param nodeYouThinkHasLeastRelationships the node of the two which you
	 * think/know has the least amount of relationship (that will be
	 * iterated over).
	 * @param secondNode the node of the two you suspect has more relationships
	 * than the first node.
	 * @param type the type of relationships to iterate over.
	 * @param direction the direction from the first node that you expect for
	 * your relationships.
	 * @param filter a filter for which relationships to take into
	 * consideration. Is allowed to be {@code null}.
	 * @param spawnThreadThreshold the threshold for when to spawn the other
	 * thread which iterates from the second node.
	 * @return if there's any relationship with the given criterias (type,
	 * direction, filter) from the first node to the second node.
	 */
	public boolean relationshipExistsBetween( Node nodeYouThinkHasLeastRelationships,
	        Node secondNode, RelationshipType type, Direction direction,
	        ObjectFilter<Relationship> filterOrNull, int spawnThreadThreshold )
	{
	    NodeFinder finderFromTheOtherNode = null;
	    try
	    {
    	    int counter = 0;
    	    for ( Relationship rel :
    	            nodeYouThinkHasLeastRelationships.getRelationships( type, direction ) )
    	    {
    	        if ( finderFromTheOtherNode != null && finderFromTheOtherNode.found )
    	        {
    	            return true;
    	        }
    	        
    	        counter++;
    	        if ( filterOrNull != null && !filterOrNull.pass( rel ) )
    	        {
    	            continue;
    	        }
    	        
    	        Node otherNode = rel.getOtherNode( nodeYouThinkHasLeastRelationships );
    	        if ( otherNode.equals( secondNode ) )
    	        {
    	            return true;
    	        }
    	        
    	        if ( counter == spawnThreadThreshold )
    	        {
    	            finderFromTheOtherNode = new NodeFinder( nodeYouThinkHasLeastRelationships,
    	                    nodeYouThinkHasLeastRelationships.getRelationships( type,
    	                            direction.reverse() ), secondNode, filterOrNull );
    	            finderFromTheOtherNode.start();
    	        }
    	    }
	    }
	    finally
	    {
	        if ( finderFromTheOtherNode != null )
	        {
	            // To make it exit
	            finderFromTheOtherNode.found = true;
	        }
	    }
	    return false;
	}
	
	private static class NodeFinder extends Thread
	{
	    private final Node node;
	    private final Iterable<Relationship> relationships;
        private final Node nodeToFind;
        private final ObjectFilter<Relationship> filterOrNull;
        private boolean found;

        NodeFinder( Node node, Iterable<Relationship> relationships, Node nodeToFind,
                ObjectFilter<Relationship> filterOrNull )
	    {
            this.node = node;
            this.relationships = relationships;
            this.nodeToFind = nodeToFind;
            this.filterOrNull = filterOrNull;
	    }
        
        @Override
        public void run()
        {
            for ( Relationship rel : relationships )
            {
                // So that found can be set from outside to make it exit
                // the loop
                if ( found )
                {
                    break;
                }
                
                if ( filterOrNull != null && !filterOrNull.pass( rel ) )
                {
                    continue;
                }
                
                Node otherNode = rel.getOtherNode( node );
                if ( otherNode.equals( nodeToFind ) )
                {
                    found = true;
                    break;
                }
            }
        }
	}
}
