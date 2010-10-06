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

import java.util.NoSuchElementException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class NodeStack
{
    private final Node rootNode;
    private final RelationshipType relType;

    public NodeStack( Node rootNode, RelationshipType relType )
    {
        this.rootNode = rootNode;
        this.relType = relType;
    }
    
    private Relationship nextRel( Node node )
    {
        return node.getSingleRelationship( relType, Direction.OUTGOING );
    }
    
    public Node push()
    {
        GraphDatabaseUtil.acquireWriteLock( rootNode );
        Node node = rootNode.getGraphDatabase().createNode();
        Relationship firstRel = nextRel( rootNode );
        if ( firstRel != null )
        {
            Node firstNode = firstRel.getOtherNode( rootNode );
            firstRel.delete();
            node.createRelationshipTo( firstNode, relType );
        }
        rootNode.createRelationshipTo( node, relType );
        return node;
    }
    
    public Node pop()
    {
        GraphDatabaseUtil.acquireWriteLock( rootNode );
        Relationship firstRel = nextRel( rootNode );
        if ( firstRel == null )
        {
            throw new NoSuchElementException();
        }
        Node firstNode = firstRel.getOtherNode( rootNode );
        Relationship secondRel = nextRel( firstNode );
        if ( secondRel != null )
        {
            Node secondNode = secondRel.getOtherNode( firstNode );
            secondRel.delete();
            rootNode.createRelationshipTo( secondNode, relType );
        }
        firstRel.delete();
        return firstNode;
    }
    
    public Node peek()
    {
        GraphDatabaseUtil.acquireWriteLock( rootNode );
        Relationship firstRel = nextRel( rootNode );
        if ( firstRel == null )
        {
            throw new NoSuchElementException();
        }
        return firstRel.getOtherNode( rootNode );
    }
    
    public boolean empty()
    {
        return nextRel( rootNode ) == null;
    }
}
