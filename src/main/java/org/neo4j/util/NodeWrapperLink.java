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
	private final Class<? extends T> theClass;
	
	/**
	 * @param node the node to act as the link.
	 * @param type the relationship type to be the link relationship.
	 * @param thisIsGenericsFault well, even if we have T we must send the
	 * same class here to make instantiation work.
	 */
	public NodeWrapperLink( Node node, RelationshipType type,
		Class<? extends T> thisIsGenericsFault )
	{
	    super( node, type );
		this.theClass = thisIsGenericsFault;
	}
	
	/**
     * @param graphDb the {@link GraphDatabaseService}.
	 * @param node the node to act as the link.
	 * @param type the relationship type to be the link relationship.
	 * @param thisIsGenericsFault well, even if we have T we must send the
	 * same class here to make instantiation work.
	 * @param direction the direction of the relationship.
	 */
	public NodeWrapperLink( GraphDatabaseService graphDb, Node node, RelationshipType type,
		Direction direction, Class<T> thisIsGenericsFault )
	{
	    super( node, type, direction );
	    this.theClass = thisIsGenericsFault;
	}
	
	protected Class<? extends T> classType()
	{
		return this.theClass;
	}
	
	@Override
	protected T newObject( Node node )
	{
		return NodeWrapperImpl.newInstance( this.classType(), node );
	}
	
	@Override
	protected Node getNodeFromItem( T item )
	{
		return item.getUnderlyingNode();
	}
}
