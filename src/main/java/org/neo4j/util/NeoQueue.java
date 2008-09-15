package org.neo4j.util;

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
		Transaction tx = neo.beginTx();
		neoUtil.getLockManager().getWriteLock( rootNode );
		try
		{
			Relationship rel = getFirstRelationship();
			Node result = null;
			if ( rel != null )
			{
				result = rel.getEndNode();
				rel.delete();
				Relationship nextRel = result.getSingleRelationship( relType,
					Direction.OUTGOING );
				Node nextNode = nextRel.getEndNode();
				nextRel.delete();
				if ( !nextNode.equals( rootNode ) )
				{
					rootNode.createRelationshipTo( nextNode, relType );
				}
				result.delete();
			}
			tx.success();
			return result != null;
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
}
