package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

/**
 * Wraps a linked list of nodes in Neo4j.
 * @author mattias
 */
public class NodeQueue
{
	private GraphDatabaseService graphDB;
	private Node rootNode;
	private RelationshipType relType;
	private GraphDatabaseUtil graphDbUtil;
	
	public NodeQueue( GraphDatabaseService graphDb, Node rootNode,
	        RelationshipType relType )
	{
		this.graphDB = graphDb;
		this.rootNode = rootNode;
		this.relType = relType;
		this.graphDbUtil = new GraphDatabaseUtil( graphDb );
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
		Transaction tx = graphDB.beginTx();
		graphDbUtil.getLockManager().getWriteLock( rootNode );
		try
		{
			Node node = graphDB.createNode();
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
			graphDbUtil.getLockManager().releaseWriteLock( rootNode );
			tx.finish();
		}
	}
	
	public boolean remove()
	{
	    return remove( 1 ) == 1;
	}
	
	public int remove( int max )
	{
        Transaction tx = graphDB.beginTx();
        graphDbUtil.getLockManager().getWriteLock( rootNode );
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
            graphDbUtil.getLockManager().releaseWriteLock( rootNode );
            tx.finish();
        }
	}
	
	public Node peek()
	{
		Transaction tx = graphDB.beginTx();
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
	    Transaction tx = graphDB.beginTx();
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
