package org.neo4j.util;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

/**
 * An implementation of {@link Link} which uses {@link NodeWrapper}.
 *
 * @param <T> the type of objects used in this instance.
 */
public class NodeWrapperLink<T extends NodeWrapper> extends AbstractLink<T>
{
	private Class<? extends T> theClass;
	
	/**
	 * @param node the node to act as the link.
	 * @param type the relationship type to be the link relationship.
	 * @param thisIsGenericsFault well, even if we have T we must send the
	 * same class here to make instantiation work.
	 */
	public NodeWrapperLink( GraphDatabaseService neo, Node node, RelationshipType type,
		Class<? extends T> thisIsGenericsFault )
	{
	    super( neo, node, type );
		this.theClass = thisIsGenericsFault;
	}
	
	/**
	 * @param node the node to act as the link.
	 * @param type the relationship type to be the link relationship.
	 * @param thisIsGenericsFault well, even if we have T we must send the
	 * same class here to make instantiation work.
	 * @param direction the direction of the relationship.
	 */
	public NodeWrapperLink( GraphDatabaseService neo, Node node, RelationshipType type,
		Direction direction, Class<T> thisIsGenericsFault )
	{
	    super( neo, node, type, direction );
	    this.theClass = thisIsGenericsFault;
	}
	
	protected Class<? extends T> classType()
	{
		return this.theClass;
	}
	
	@Override
	protected T newObject( Node node )
	{
		return NodeWrapperImpl.newInstance( this.classType(), graphDb(), node );
	}
	
	@Override
	protected Node getNodeFromItem( T item )
	{
		return item.getUnderlyingNode();
	}
}
