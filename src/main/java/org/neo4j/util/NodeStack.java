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
        rootNode.removeProperty( "___this is a dummy property for locking___" );
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
        rootNode.removeProperty( "___this is a dummy property for locking___" );
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
        rootNode.removeProperty( "___this is a dummy property for locking___" );
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
