/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
