package org.neo4j.util;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * A neo relationship set where each element wraps a neo {@link Node},
 * using {@link NodeWrapperImpl}.
 * @author mattias
 *
 * @param <T> the class type of each object in the collection.
 */
public class NodeWrapperRelationshipSet<T extends NodeWrapper>
	extends RelationshipSet<T>
{
	private Class<? extends T> instanceClass;
	
	/**
	 * @param node the node with its relationships acting as a collection.
	 * @param type the type of relationships to read/write. 
	 * @param instanceClass the exact class of instances in the collection.
	 */
	public NodeWrapperRelationshipSet( GraphDatabaseService neo, Node node,
		RelationshipType type, Class<? extends T> instanceClass )
	{
		super( neo, node, type );
		this.instanceClass = instanceClass;
	}
	
	/**
	 * @param node the node with its relationships acting as a collection.
	 * @param type the type of relationships to read/write. 
	 * @param direction the direction of relationships.
	 * @param instanceClass the exact class of instances in the collection.
	 */
	public NodeWrapperRelationshipSet( GraphDatabaseService neo, Node node,
		RelationshipType type, Direction direction,
		Class<? extends T> instanceClass )
	{
		super( neo, node, type, direction );
		this.instanceClass = instanceClass;
	}
	
	protected Class<? extends T> getInstanceClass()
	{
		return this.instanceClass;
	}

	@Override
	protected T newObject( Node node, Relationship relationship )
	{
		return NodeWrapperImpl.newInstance( this.getInstanceClass(), graphDb(),
			node );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( ( NodeWrapper ) item ).getUnderlyingNode();
	}
}