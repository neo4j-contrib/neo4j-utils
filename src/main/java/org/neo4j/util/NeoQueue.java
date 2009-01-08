package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;

/**
 * Wraps a linked list of nodes in neo.
 * @author mattias
 */
public class NeoQueue
{
	private NeoService neo;
	private Node rootNode;
	private RelationshipType relType;
	private NeoUtil neoUtil;
	
	public NeoQueue( NeoService neo, Node rootNode, RelationshipType relType )
	{
		this.neo = neo;
		this.rootNode = rootNode;
		this.relType = relType;
		this.neoUtil = new NeoUtil( neo );
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
		Transaction tx = neo.beginTx();
		neoUtil.getLockManager().getWriteLock( rootNode );
		try
		{
			Node node = neo.createNode();
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
			tx.success();
			return node;
		}
		finally
		{
			neoUtil.getLockManager().releaseWriteLock( rootNode );
			tx.finish();
		}
	}
	
	public boolean remove()
	{
	    return remove( 1 ) == 1;
	}
	
	public int remove( int max )
	{
        Transaction tx = neo.beginTx();
        neoUtil.getLockManager().getWriteLock( rootNode );
        try
        {
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
            
            tx.success();
            return removed;
        }
        finally
        {
            neoUtil.getLockManager().releaseWriteLock( rootNode );
            tx.finish();
        }
	}
	
	public Node peek()
	{
		Transaction tx = neo.beginTx();
		try
		{
			Relationship rel = getFirstRelationship();
			Node result = null;
			if ( rel != null )
			{
				result = rel.getEndNode();
			}
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	public Node[] peek( int max )
	{
	    Transaction tx = neo.beginTx();
	    try
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
            tx.success();
            return result.toArray( new Node[ 0 ] );
	    }
	    finally
	    {
	        tx.finish();
	    }
	}
}
