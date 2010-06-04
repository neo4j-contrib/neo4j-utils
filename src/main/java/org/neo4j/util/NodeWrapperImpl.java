package org.neo4j.util;

import java.lang.reflect.Constructor;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * Wraps a {@link Node}, also overriding {@link #equals(Object)} and
 * {@link #hashCode()} to make it more useful.
 * @author mattias
 */
public abstract class NodeWrapperImpl implements NodeWrapper
{
	private final Node node;
	
	/**
	 * Utility method for instantiating a new node wrapper instance, using
	 * the class' constructor which takes a {@link Node}.
	 * @param <T> the resulting instance's class type.
	 * @param instanceClass the resulting instance's class type.
	 * @param node the node to wrap, the node returned from
	 * {@link #getUnderlyingNode()}.
	 * @return the new instance wrapping the node.
	 */
	public static <T extends NodeWrapper> T newInstance(
		Class<T> instanceClass, Node node )
	{
		try
		{
			Constructor<T> constructor = null;
			try
			{
                constructor = instanceClass.getConstructor( Node.class );
			}
			catch ( NoSuchMethodException e )
			{
                constructor = instanceClass.getConstructor( GraphDatabaseService.class,
                        Node.class );
			}
			T result = constructor.newInstance( node );
			return result;
		}
		catch ( RuntimeException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
    /**
     * Utility method for instantiating a new node wrapper instance, using
     * the class' constructor which takes a {@link Node}.
     * @param <T> the resulting instance's class type.
     * @param instanceClass the resulting instance's class type.
     * @param graphDb the {@link GraphDatabaseService} used with the node.
     * @param nodeId the id of the node to wrap, the node returned from
     * {@link #getUnderlyingNode()}.
     * @return the new instance wrapping the node (with the given id).
     */
	public static <T extends NodeWrapper> T newInstance(
		Class<T> instanceClass, GraphDatabaseService graphDb, long nodeId )
	{
		return newInstance( instanceClass, graphDb.getNodeById( nodeId ) );
	}

	protected NodeWrapperImpl( Node node )
	{
		this.node = node;
	}
	
	/**
	 * @return the wrapped node, the one received in the constructor.
	 */
	public Node getUnderlyingNode()
	{
		return node;
	}
	
	@Override
	public boolean equals( Object o )
	{
		return o != null && getClass().equals( o.getClass() ) &&
			getUnderlyingNode().equals(
				( ( NodeWrapper ) o ).getUnderlyingNode() );
	}
	
	@Override
	public int hashCode()
	{
		return getUnderlyingNode().hashCode();
	}
}
