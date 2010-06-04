package org.neo4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

public class TestNodeQueue extends Neo4jTest
{
	private static enum RelTypes implements RelationshipType
	{
		TEST_QUEUE,
	}
	
	@Ignore
	@Test
    public void testSome() throws Exception
	{
		// TODO Test diabled for now since it always fails.
		Collection<Node> rootNodes = new ArrayList<Node>();
		for ( int i = 0; i < 10; i++ )
		{
        	Node rootNode = graphDb().createNode();
        	rootNodes.add( rootNode );
	        NodeQueue q = new NodeQueue( rootNode, RelTypes.TEST_QUEUE );
	        for ( int ii = 0; ii < 10000; ii++ )
	        {
	        	q.add();
	        }
		}
		
        for ( Node node : rootNodes )
        {
        	NodeQueue q = new NodeQueue( node, RelTypes.TEST_QUEUE );
        	while ( q.peek() != null )
        	{
        		q.remove();
        	}
        	node.delete();
        }
	}
	
	@Test
    public void testMany() throws Exception
	{
	    Transaction tx = graphDb().beginTx();
	    
	    Node rootNode = graphDb().createNode();
	    NodeQueue q = new NodeQueue( rootNode, RelTypes.TEST_QUEUE );
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
	
	@Test
    public void testFixedLengthList() throws Exception
	{
	    Transaction tx = graphDb().beginTx();
	    
	    Node rootNode = graphDb().createNode();
	    FixedLengthNodeList list = new FixedLengthNodeList( rootNode,
	        DynamicRelationshipType.withName( "LIST_TEST" ), 5 );
	    assertNull( list.peek() );
	    Node a = list.add();
	    assertEquals( a, list.peek() );
	    assertEquals( a, list.peek( 10 )[ 0 ] );
	    Node b = list.add();
	    assertEquals( b, list.peek() );
	    assertEquals( b, list.peek( 10 )[ 0 ] );
        assertEquals( a, list.peek( 10 )[ 1 ] );
        Node c = list.add();
        Node d = list.add();
        Node e = list.add();
        assertEquals( 5, list.peek( 10 ).length );
        assertEquals( e, list.peek() );
        assertEquals( a, list.peek( 10 )[ 4 ] );
        Node f = list.add();
        assertEquals( 5, list.peek( 10 ).length );
        assertEquals( f, list.peek() );
        assertEquals( b, list.peek( 10 )[ 4 ] );
        Node g = list.add();
        assertEquals( 5, list.peek( 10 ).length );
        assertEquals( g, list.peek() );
        assertEquals( c, list.peek( 10 )[ 4 ] );
        
        list.remove( 10 );
        rootNode.delete();
	    
	    tx.success();
	    tx.finish();
	}
	
	@Test
	public void testNodeStack()
	{
	    Transaction tx = graphDb().beginTx();
	    Node rootNode = graphDb().createNode();
	    RelationshipType type = DynamicRelationshipType.withName( "stack" );
	    NodeStack stack = new NodeStack( rootNode, type );
	    assertStackEmpty( stack );
	    Node node = stack.push();
	    node.setProperty( "name", "first" );
	    assertFalse( stack.empty() );
	    assertEquals( node, stack.peek() );
	    assertEquals( node, stack.pop() );
	    assertStackEmpty( stack );
	    
        node = stack.push();
        node.setProperty( "name", "first" );
        Node node2 = stack.push();
        node.setProperty( "name", "second" );
        assertFalse( stack.empty() );
        assertEquals( node2, stack.peek() );
        assertEquals( node2, stack.pop() );
        assertEquals( node, stack.peek() );
        assertEquals( node, stack.peek() );
        assertEquals( node, stack.pop() );
        assertStackEmpty( stack );
        rootNode.delete();
        tx.success();
        tx.finish();
	}

    private void assertStackEmpty( NodeStack stack )
    {
        assertTrue( stack.empty() );
	    try
	    {
	        stack.peek();
	        fail( "Shouldn't be able to peek here" );
	    }
	    catch ( NoSuchElementException e ) {}
        try
        {
            stack.pop();
            fail( "Shouldn't be able to peek here" );
        }
        catch ( NoSuchElementException e ) {}
    }
}
