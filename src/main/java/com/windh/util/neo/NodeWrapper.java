package com.windh.util.neo;

import java.lang.reflect.Constructor;

import org.neo4j.api.core.Node;

public abstract class NodeWrapper
{
	private Node node;
	
	public static <T extends NodeWrapper> T newInstance(
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
			( ( NodeWrapper ) o ).getUnderlyingNode() );
	}
	
	@Override
	public int hashCode()
	{
		return getUnderlyingNode().hashCode();
	}
}
