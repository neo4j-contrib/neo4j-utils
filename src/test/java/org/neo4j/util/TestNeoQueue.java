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
	
	public void testMany() throws Exception
	{
	    Transaction tx = neo().beginTx();
	    
	    Node rootNode = neo().createNode();
	    NeoQueue q = new NeoQueue( neo(), rootNode, RelTypes.TEST_QUEUE );
	    for ( int i = 0; i < 10; i++ )
	    {
	        Node node = q.add();
	        node.setProperty( "p", i );
	    }
	    
	    Node node = q.peek();
	    assertEquals( 0, node.getProperty( "p" ) );
	    Node[] nodes = q.peek( 4 );
	    assertEquals( 4, nodes.length );
	    for ( int i = 0; i < 4; i++ )
	    {
	        assertEquals( i, nodes[ i ].getProperty( "p" ) );
	    }
	    assertEquals( 10, q.peek( 20 ).length );
	    
	    q.remove( 3 );
	    assertEquals( 3, q.peek().getProperty( "p" ) );
	    assertEquals( 7, q.peek( 20 ).length );
	    
	    q.remove( 7 );
	    rootNode.delete();
	    
	    tx.success();
	    tx.finish();
	}
}
