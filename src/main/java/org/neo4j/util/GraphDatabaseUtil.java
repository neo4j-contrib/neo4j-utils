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
import org.neo4j.graphdb.RelationshipExpander;
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
	
	private static void assertPropertyKeyNotNull( String key )
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
	public static void setProperty( PropertyContainer container,
		String key, Object value )
	{
		assertPropertyKeyNotNull( key );
		if ( value == null )
		{
			throw new IllegalArgumentException( "Value for property '" +
				key + "' can't be null" );
		}
		
		Transaction tx = container.getGraphDatabase().beginTx();
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
	
	public static List<Object> getPropertyValues( PropertyContainer container,
		String key )
	{
		Object value = container.getProperty( key, null );
		return value == null ?
		    new ArrayList<Object>() : propertyValueAsList( value );
	}	
	
	public static boolean addValueToArray( PropertyContainer container,
		String key, Object value )
	{
		Transaction tx = container.getGraphDatabase().beginTx();
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
	
	public static boolean removeValueFromArray( PropertyContainer container,
		String key, Object value )
	{
		Transaction tx = container.getGraphDatabase().beginTx();
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
	public static Object removeProperty( PropertyContainer container, String key )
	{
		assertPropertyKeyNotNull( key );
		Transaction tx = container.getGraphDatabase().beginTx();
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
	
	public static Node getSingleOtherNode( Node node, RelationshipType type,
		Direction direction )
	{
		Relationship rel = node.getSingleRelationship( type, direction );
		return rel == null ? null : rel.getOtherNode( node );
	}
	
	public static Node getOrCreateSingleOtherNode( Node fromNode, RelationshipType type,
	        Direction direction )
	{
	    GraphDatabaseService graphDb = fromNode.getGraphDatabase();
        Transaction tx = graphDb.beginTx();
        try
        {
            Node otherNode = null;
            Relationship singleRelationship =
                fromNode.getSingleRelationship( type, direction );
            if ( singleRelationship != null )
            {
                otherNode = singleRelationship.getOtherNode( fromNode );
            }
            else
            {
                otherNode = graphDb.createNode();
                fromNode.createRelationshipTo( otherNode, type );
            }
            
            tx.success();
            return otherNode;
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
	    return getOrCreateSingleOtherNode( graphDb().getReferenceNode(),
	            type, direction );
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
	    return getLockManager( graphDb );
	}
	
	public static LockManager getLockManager( GraphDatabaseService graphDb )
	{
        return ( ( EmbeddedGraphDatabase ) graphDb ).getConfig().getLockManager();
	}
	
	public TransactionManager getTransactionManager()
	{
		return ( ( EmbeddedGraphDatabase )
			graphDb() ).getConfig().getTxModule().getTxManager();
	}
	
	public static Object[] propertyValueAsArray( Object propertyValue )
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
	
	public static List<Object> propertyValueAsList( Object propertyValue )
	{
		return new ArrayList<Object>(
			Arrays.asList( propertyValueAsArray( propertyValue ) ) );
	}
	
	public static Object asPropertyValue( Collection<?> values )
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
	
	public static Integer incrementAndGetCounter( Node node, String propertyKey )
	{
		Transaction tx = node.getGraphDatabase().beginTx();
		LockManager lockManager = getLockManager( node.getGraphDatabase() );
		lockManager.getWriteLock( node );
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
			lockManager.releaseWriteLock( node );
			tx.finish();
		}
	}

	public static Integer decrementAndGetCounter( Node node, String propertyKey,
		int notLowerThan )
	{
		Transaction tx = node.getGraphDatabase().beginTx();
		LockManager lockManager = getLockManager( node.getGraphDatabase() );
		lockManager.getWriteLock( node );
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
			lockManager.releaseWriteLock( node );
			tx.finish();
		}
	}
	
	public static String sumNodeContents( Node node )
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
     * @param expander the {@link RelationshipExpander} to use to expand the
     * relationships to iterate over.
     * @return the first relationship, if any, with the given criterias (type,
     * direction, filter) between the two nodes.
     */
    public static Relationship getExistingRelationshipBetween( 
            Node nodeYouThinkHasLeastRelationships, Node secondNode, 
            RelationshipExpander expander )
    {
        return getExistingRelationshipBetween( nodeYouThinkHasLeastRelationships, 
                secondNode, expander, null, 50 );
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
	 * @param expander the {@link RelationshipExpander} to use to expand the
	 * relationships to iterate over.
	 * @param filter a filter for which relationships to take into
	 * consideration. Is allowed to be {@code null}.
	 * @param spawnThreadThreshold the threshold for when to spawn the other
	 * thread which iterates from the second node.
	 * @return the first relationship, if any, with the given criterias (type,
     * direction, filter) between the two nodes.
	 */
	public static Relationship getExistingRelationshipBetween( 
	        Node nodeYouThinkHasLeastRelationships, Node secondNode, 
	        RelationshipExpander expander,
	        ObjectFilter<Relationship> filterOrNull, int spawnThreadThreshold )
	{
	    NodeFinder finderFromTheOtherNode = null;
	    try
	    {
    	    int counter = 0;
    	    for ( Relationship rel : expander.expand( nodeYouThinkHasLeastRelationships ) )
    	    {
    	        if ( finderFromTheOtherNode != null && 
    	                finderFromTheOtherNode.foundRelationship != null )
    	        {
    	            return finderFromTheOtherNode.foundRelationship;
    	        }
    	        
    	        counter++;
    	        if ( filterOrNull != null && !filterOrNull.pass( rel ) )
    	        {
    	            continue;
    	        }
    	        
    	        Node otherNode = rel.getOtherNode( nodeYouThinkHasLeastRelationships );
    	        if ( otherNode.equals( secondNode ) )
    	        {
    	            return rel;
    	        }
    	        
    	        if ( counter == spawnThreadThreshold )
    	        {
    	            finderFromTheOtherNode = new NodeFinder( nodeYouThinkHasLeastRelationships,
    	                    expander.reversed(), secondNode, filterOrNull );
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
	    return null;
	}
	
	private static class NodeFinder extends Thread
	{
	    private final Node nodeWithLeastRels;
	    private final Iterable<Relationship> relationships;
        private final Node secondNode;
        private final ObjectFilter<Relationship> filterOrNull;
        private volatile boolean found;
        private volatile Relationship foundRelationship = null;

        NodeFinder( Node nodeWithLeastRels, RelationshipExpander expander, Node secondNode,
                ObjectFilter<Relationship> filterOrNull )
	    {
            this.nodeWithLeastRels = nodeWithLeastRels;
            this.relationships = expander.expand( secondNode );
            this.secondNode = secondNode;
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
                
                Node otherNode = rel.getOtherNode( secondNode );
                if ( otherNode.equals( nodeWithLeastRels ) )
                {
                    foundRelationship = rel;
                    break;
                }
            }
        }
	}
}
