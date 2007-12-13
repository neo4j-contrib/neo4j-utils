package org.neo4j.util;

import java.lang.reflect.Constructor;

import org.neo4j.api.core.Node;

/**
 * Wraps a {@link Node}, also overriding {@link #equals(Object)} and
 * {@link #hashCode()} to make it more useful.
 * @author mattias
 */
public abstract class NodeWrapper implements NodeWrappable
{
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
	public static <T extends NodeWrappable> T newInstance(
		Class<T> instanceClass, Node node )
	{
		try
		{
			Constructor<T> constructor =
				instanceClass.getConstructor( Node.class );
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

	protected NodeWrapper( Node node )
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
		if ( o == null || !getClass().equals( o.getClass() ) )
		{
			return false;
		}
		return getUnderlyingNode().equals(
			( ( NodeWrappable ) o ).getUnderlyingNode() );
	}
	
	@Override
	public int hashCode()
	{
		return getUnderlyingNode().hashCode();
	}
}
