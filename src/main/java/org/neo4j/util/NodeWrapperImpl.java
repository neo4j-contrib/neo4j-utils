package org.neo4j.util;

import java.lang.reflect.Constructor;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;

/**
 * Wraps a {@link Node}, also overriding {@link #equals(Object)} and
 * {@link #hashCode()} to make it more useful.
 * @author mattias
 */
public abstract class NodeWrapperImpl implements NodeWrapper
{
	private NeoService neo;
	private Node node;
	
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
		Class<T> instanceClass, NeoService neo, Node node )
	{
		try
		{
			Constructor<T> constructor =
				instanceClass.getConstructor( NeoService.class, Node.class );
			T result = constructor.newInstance( neo, node );
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
	
	public static <T extends NodeWrapper> T newInstance(
		Class<T> instanceClass, NeoService neo, long nodeId )
	{
		Transaction tx = neo.beginTx();
		try
		{
			Node node = neo.getNodeById( nodeId );
			T result = newInstance( instanceClass, neo, node );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	protected NodeWrapperImpl( NeoService neo, Node node )
	{
		this.neo = neo;
		this.node = node;
	}
	
	/**
	 * @return the wrapped node, the one received in the constructor.
	 */
	public Node getUnderlyingNode()
	{
		return node;
	}
	
	protected NeoService getNeo()
	{
		return this.neo;
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
