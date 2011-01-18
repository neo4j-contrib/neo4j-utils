/**
 * Copyright (c) 2002-2011 "Neo Technology,"
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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * A relationship set where each element wraps a {@link Node},
 * using {@link NodeWrapperImpl}.
 * @author mattias
 *
 * @param <T> the class type of each object in the collection.
 */
public class NodeWrapperRelationshipSet<T extends NodeWrapper>
	extends RelationshipSet<T>
{
	private final Class<? extends T> instanceClass;
	
	/**
	 * @param node the node with its relationships acting as a collection.
	 * @param type the type of relationships to read/write. 
	 * @param instanceClass the exact class of instances in the collection.
	 */
	public NodeWrapperRelationshipSet( Node node,
		RelationshipType type, Class<? extends T> instanceClass )
	{
		super( node, type );
		this.instanceClass = instanceClass;
	}
	
	/**
	 * @param node the node with its relationships acting as a collection.
	 * @param type the type of relationships to read/write. 
	 * @param direction the direction of relationships.
	 * @param instanceClass the exact class of instances in the collection.
	 */
	public NodeWrapperRelationshipSet( Node node,
		RelationshipType type, Direction direction,
		Class<? extends T> instanceClass )
	{
		super( node, type, direction );
		this.instanceClass = instanceClass;
	}
	
	protected Class<? extends T> getInstanceClass()
	{
		return this.instanceClass;
	}

	@Override
	protected T newObject( Node node, Relationship relationship )
	{
		return NodeWrapperImpl.newInstance( this.getInstanceClass(), node );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( ( NodeWrapper ) item ).getUnderlyingNode();
	}
}