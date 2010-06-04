package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * Wraps a linked list of nodes in Neo4j.
 * @author mattias
 */
public class NodeQueue
{
	private final Node rootNode;
	private final RelationshipType relType;
	
	public NodeQueue( Node rootNode, RelationshipType relType )
	{
		this.rootNode = rootNode;
		this.relType = relType;
	}
	
	private Relationship getFirstRelationship()
	{
		return rootNode.getSingleRelationship( relType, Direction.OUTGOING );
	}
	
	private Relationship getLastRelationship()
	{
		return rootNode.getSingleRelationship( relType, Direction.INCOMING );
	}
	
	public Node add()
	{
	    GraphDatabaseUtil.acquireWriteLock( rootNode );
		Node node = rootNode.getGraphDatabase().createNode();
		Relationship rel = getLastRelationship();
		if ( rel == null )
		{
			rootNode.createRelationshipTo( node, relType );
		}
		else
		{
			Node lastNode = rel.getStartNode();
			rel.delete();
			lastNode.createRelationshipTo( node, relType );
		}
		node.createRelationshipTo( rootNode, relType );
		return node;
	}
	
    public boolean remove()
	{
	    return remove( 1 ) == 1;
	}
	
	public int remove( int max )
	{
	    GraphDatabaseUtil.acquireWriteLock( rootNode );
        Relationship rel = getFirstRelationship();
        int removed = 0;
        if ( rel != null )
        {
            Node node = rel.getEndNode();
            Node nextNode = null;
            for ( int i = 0; i < max; i++ )
            {
                Relationship relToNext = node.getSingleRelationship(
                    relType, Direction.OUTGOING );
                nextNode = relToNext.getEndNode();
                for ( Relationship relToDel : node.getRelationships(
                    relType ) )
                {
                    relToDel.delete();
                }
                node.delete();
                removed++;
                if ( nextNode.equals( rootNode ) )
                {
                    break;
                }
                node = nextNode;
            }

            if ( nextNode != null && !nextNode.equals( rootNode ) )
            {
                rootNode.createRelationshipTo( nextNode, relType );
            }
        }
        return removed;
	}
	
	public Node peek()
	{
		Relationship rel = getFirstRelationship();
		Node result = null;
		if ( rel != null )
		{
			result = rel.getEndNode();
		}
		return result;
	}
	
	public Node[] peek( int max )
	{
        Collection<Node> result = new ArrayList<Node>( max );
        Node node = rootNode;
        for ( int i = 0; i < max; i++ )
        {
            Relationship rel = node.getSingleRelationship( relType,
                Direction.OUTGOING );
            if ( rel == null )
            {
                break;
            }
            Node otherNode = rel.getEndNode();
            if ( otherNode.equals( rootNode ) )
            {
                break;
            }
            result.add( otherNode );
            node = otherNode;
        }
        return result.toArray( new Node[ 0 ] );
	}
}
