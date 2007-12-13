package org.neo4j.util;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;

/**
 * A neo relationship set where each element wraps a neo {@link Node},
 * using {@link NodeWrapper}.
 * @author mattias
 *
 * @param <T> the class type of each object in the collection.
 */
public class NodeWrapperRelationshipSet<T extends NodeWrappable>
	extends NeoRelationshipSet<T>
{
	private Class<T> instanceClass;
	
	/**
	 * @param node the node with its relationships acting as a collection.
	 * @param type the type of relationships to read/write. 
	 * @param instanceClass the exact class of instances in the collection.
	 */
	public NodeWrapperRelationshipSet( Node node, RelationshipType type,
		Class<T> instanceClass )
	{
		super( node, type );
		this.instanceClass = instanceClass;
	}
	
	/**
	 * @param node the node with its relationships acting as a collection.
	 * @param direction the direction of relationships.
	 * @param type the type of relationships to read/write. 
	 * @param instanceClass the exact class of instances in the collection.
	 */
	public NodeWrapperRelationshipSet( Node node, Direction direction,
		RelationshipType type, Class<T> instanceClass )
	{
		super( node, direction, type );
		this.instanceClass = instanceClass;
	}
	
	protected Class<T> getInstanceClass()
	{
		return this.instanceClass;
	}

	@Override
	protected T newObject( Node node, Relationship relationship )
	{
		return NodeWrapper.newInstance( this.getInstanceClass(), node );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( ( NodeWrappable ) item ).getUnderlyingNode();
	}
}
