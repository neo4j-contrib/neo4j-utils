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

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * Convenience {@link Set} for when dealing with basic nodes and relationships
 * in contrast to {@link NodeWrapperRelationshipSet} where you're dealing with
 * wrappers around nodes.
 */
public class NodeRelationshipSet extends RelationshipSet<Node>
{
	public NodeRelationshipSet( Node node,
		RelationshipType type )
	{
		super( node, type );
	}
	
	public NodeRelationshipSet( Node node,
		RelationshipType type, Direction direction )
	{
		super( node, type, direction );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( Node ) item;
	}
	
	@Override
	protected Node newObject( Node node, Relationship relationship )
	{
		return node;
	}
}
