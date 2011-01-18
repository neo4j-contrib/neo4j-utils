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
import org.neo4j.kernel.impl.transaction.IllegalResourceException;

public abstract class AbstractLink<T> implements Link<T>
{
    private final Node node;
    private final RelationshipType type;
    private final Direction direction;
    
    public AbstractLink( Node node, RelationshipType type,
        Direction direction )
    {
        this.node = node;
        this.type = type;
        this.direction = direction;
    }
    
    public AbstractLink( Node node, RelationshipType type )
    {
        this( node, type, Direction.OUTGOING );
    }
    
    protected Node node()
    {
        return this.node;
    }
    
    protected Direction direction()
    {
        return this.direction;
    }
    
    protected RelationshipType type()
    {
        return this.type;
    }
    
    protected abstract T newObject( Node node );
    
    private T newObject( Relationship relationship )
    {
        return newObject( relationship.getOtherNode( this.node ) );
    }
    
    protected abstract Node getNodeFromItem( T item );
    
    protected Relationship getLinkRelationshipOrNull()
    {
        return this.node.getSingleRelationship( this.type(),
            this.direction() );
    }
    
    public T get()
    {
        Relationship relationship = getLinkRelationshipOrNull();
        return relationship == null ? null : newObject( relationship );
    }

    public boolean has()
    {
        return getLinkRelationshipOrNull() != null;
    }

    public T remove()
    {
        Relationship relationship = getLinkRelationshipOrNull();
        T result = null;
        if ( relationship != null )
        {
            result = newObject( relationship );
            entityRemoved( result, relationship );
            relationship.delete();
        }
        return result;
    }

    public T set( T object )
    {
        GraphDatabaseUtil.acquireWriteLock( node );
        try
        {
            Relationship existingRelationship = getLinkRelationshipOrNull();
            T existingObject = null;
            if ( existingRelationship != null )
            {
                existingObject = newObject( existingRelationship );
                entityRemoved( existingObject, existingRelationship );
                existingRelationship.delete();
            }
            
            Node entityNode = getNodeFromItem( object );
            Node startNode = this.direction() == Direction.OUTGOING ?
                this.node() : entityNode;
            Node endNode = this.direction() == Direction.OUTGOING ?
                entityNode : this.node();
            Relationship createdRelationship =
                startNode.createRelationshipTo( endNode, this.type() );
            entitySet( object, createdRelationship );
            return existingObject;
        }
        catch ( IllegalResourceException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    protected void entitySet( T entity, Relationship createdRelationship )
    {
    }
    
    protected void entityRemoved( T entity, Relationship removedRelationship )
    {
    }
}
