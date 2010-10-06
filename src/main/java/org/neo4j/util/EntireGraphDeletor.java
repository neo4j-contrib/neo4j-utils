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

import java.util.Collection;
import java.util.HashSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * Deletes an entire graph, that is all connected nodes (and their connected nodes)
 * from a start node (any start node in that graph). It is mostly used in
 * some test cases.
 */
public class EntireGraphDeletor implements GraphDeletor
{
	public void delete( Node startNode )
	{
		removeNodeAndThoseConnectedWith( new HashSet<Node>(),
			new HashSet<Relationship>(), startNode );
	}

	private void removeNodeAndThoseConnectedWith( Collection<Node> deletedNodes,
		Collection<Relationship> deletedRels, Node node )
	{
		for ( Relationship rel : node.getRelationships() )
		{
			Node sub = rel.getOtherNode( node );
			if ( deletedRels.add( rel ) )
			{
				aboutToDeleteRelationship( rel );
				rel.delete();
			}
			removeNodeAndThoseConnectedWith( deletedNodes, deletedRels, sub );
		}
		if ( deletedNodes.add( node ) )
		{
			aboutToDeleteNode( node );
			node.delete();
		}
	}
	
	protected void aboutToDeleteRelationship( Relationship relationship )
	{
	}
	
	protected void aboutToDeleteNode( Node node )
	{
	}
}
