package org.neo4j.util;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.transaction.IllegalResourceException;
import org.neo4j.impl.transaction.LockManager;

public abstract class AbstractLink<T> implements Link<T>
{
    private final NeoService neo;
    private final Node node;
    private final RelationshipType type;
    private final Direction direction;
    
    public AbstractLink( NeoService neo, Node node, RelationshipType type,
        Direction direction )
    {
        this.neo = neo;
        this.node = node;
        this.type = type;
        this.direction = direction;
    }
    
    public AbstractLink( NeoService neo, Node node, RelationshipType type )
    {
        this( neo, node, type, Direction.OUTGOING );
    }
    
    protected NeoService neo()
    {
        return this.neo;
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
    
    private Relationship getLinkRelationshipOrNull()
    {
        return this.node.getSingleRelationship( this.type(),
            this.direction() );
    }
    
    public T get()
    {
        Transaction tx = neo.beginTx();
        try
        {
            Relationship relationship = getLinkRelationshipOrNull();
            T result = relationship == null ? null : newObject( relationship );
            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    public boolean has()
    {
        Transaction tx = neo.beginTx();
        try
        {
            boolean result = getLinkRelationshipOrNull() != null;
            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    public T remove()
    {
        Transaction tx = neo.beginTx();
        try
        {
            Relationship relationship = getLinkRelationshipOrNull();
            T result = null;
            if ( relationship != null )
            {
                result = newObject( relationship );
                entityRemoved( result, relationship );
                relationship.delete();
            }
            tx.success();
            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    public T set( T object )
    {
        Transaction tx = neo.beginTx();
        LockManager lockManager =
            ( ( EmbeddedNeo ) neo ).getConfig().getLockManager();
        try
        {
            lockManager.getWriteLock( this.node() );
            Relationship existingRelationship = getLinkRelationshipOrNull();
            T existingObject = null;
            if ( existingRelationship != null )
            {
                existingObject = newObject( existingRelationship );
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
            tx.success();
            return existingObject;
        }
        catch ( IllegalResourceException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            try
            {
                lockManager.releaseWriteLock( this.node() );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
            tx.finish();
        }
    }
    
    protected void entitySet( T entity, Relationship createdRelationship )
    {
    }
    
    protected void entityRemoved( T entity, Relationship removedRelationship )
    {
    }
}
