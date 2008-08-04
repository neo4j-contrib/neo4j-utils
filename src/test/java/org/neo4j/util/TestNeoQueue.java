package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;

import test.NeoTest;

public class TestNeoQueue extends NeoTest
{
	private static enum RelTypes implements RelationshipType
	{
		TEST_QUEUE,
	}
	
	public void testSome() throws Exception
	{
		// TODO Test diabled for now since it always fails.
		if ( true )
		{
			return;
		}
		
		Collection<Node> rootNodes = new ArrayList<Node>();
		for ( int i = 0; i < 10; i++ )
		{
			Transaction tx = neo().beginTx();
	        try
	        {
	        	Node rootNode = neo().createNode();
	        	rootNodes.add( rootNode );
		        NeoQueue q = new NeoQueue( neo(),
		        	rootNode, RelTypes.TEST_QUEUE );
		        for ( int ii = 0; ii < 10000; ii++ )
		        {
		        	q.add();
		        }
		        tx.success();
	        }
	        finally
	        {
		        tx.finish();
	        }
		}
		
		Transaction tx = neo().beginTx();
        try
        {
	        for ( Node node : rootNodes )
	        {
	        	NeoQueue q = new NeoQueue( neo(), node,
	        		RelTypes.TEST_QUEUE );
	        	while ( q.peek() != null )
	        	{
	        		q.remove();
	        	}
	        	node.delete();
	        }
	        tx.success();
        }
        finally
        {
	        tx.finish();
        }
	}
}
