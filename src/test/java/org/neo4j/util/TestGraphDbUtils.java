package org.neo4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.TraversalFactory;

public class TestGraphDbUtils extends TxNeo4jTest
{
	private static GraphDatabaseUtil graphDbUtil;
	
	@BeforeClass
	public static void setUpUtil() throws Exception
	{
		graphDbUtil = new GraphDatabaseUtil( graphDb() );
	}
	
	@Test
    public void testArrays()
	{
		String key = "key_with_array_values";
		Node node = graphDb().createNode();
		int v1 = 10;
		int v2 = 101;
		int v3 = 2002;
		assertTrue( graphDbUtil.addValueToArray( node, key, v1 ) );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1 );
		assertFalse( graphDbUtil.addValueToArray( node, key, v1 ) );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1 );
		assertTrue( graphDbUtil.addValueToArray( node, key, v2 ) );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1, v2 );
		assertTrue( graphDbUtil.addValueToArray( node, key, v3 ) );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1, v2, v3 );
		assertTrue( graphDbUtil.removeValueFromArray( node, key, v2 ) );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1, v3 );
		assertFalse( graphDbUtil.removeValueFromArray( node, key, v2 ) );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1, v3 );
		node.delete();
	}
	
	@Test
    public void testArraySet()
	{
		String key = "key_with_array_values";
		Node node = graphDb().createNode();
		int v1 = 10;
		int v2 = 101;
		int v3 = 2002;
		Collection<Integer> values = new PropertyArraySet<Integer>(
			graphDb(), node, key );
		assertTrue( values.add( v1 ) );
		assertCollection( values, v1 );
		assertFalse( values.add( v1 ) );
		assertCollection( values, v1 );
		assertTrue( values.add( v2 ) );
		assertCollection( values, v1, v2 );
		assertTrue( values.add( v3 ) );
		assertCollection( values, v1, v2, v3 );
		assertCollection( graphDbUtil.propertyValueAsList(
			node.getProperty( key ) ), v1, v2, v3 );
		assertTrue( values.remove( v2 ) );
		assertCollection( values, v1, v3 );
		assertFalse( values.remove( v2 ) );
		assertCollection( values, v1, v3 );
		values.clear();
		assertCollection( values );
		
		values.addAll( Arrays.asList( v1, v2, v3 ) );
		assertCollection( values, v1, v2, v3 );
		values.retainAll( Arrays.asList( v2 ) );
		assertCollection( values, v2 );
		
		node.delete();
	}
	
	@Test
    public void testSumContents() throws Exception
	{
        Node node1 = graphDb().createNode();
        Node node2 = graphDb().createNode();
        Node node3 = graphDb().createNode();
        Node node4 = graphDb().createNode();
        
        Relationship r1 = node1.createRelationshipTo(
            node2, TestRelTypes.TEST_TYPE );
        Relationship r2 = node2.createRelationshipTo(
            node1, TestRelTypes.TEST_OTHER_TYPE );
        Relationship r3 = node3.createRelationshipTo(
            node1, TestRelTypes.TEST_TYPE );
        Relationship r4 = node1.createRelationshipTo(
            node4, TestRelTypes.TEST_YET_ANOTHER_TYPE );
        
        node1.setProperty( "prop1", "Hejsan" );
        node1.setProperty( "prop2", 10 );
        
        new GraphDatabaseUtil( graphDb() ).sumNodeContents( node1 );
        
        r1.delete();
        r2.delete();
        r3.delete();
        r4.delete();
        
        node1.delete();
        node2.delete();
        node3.delete();
        node4.delete();
	}
	
	@Test
	public void testRelationshipExistsBetween()
	{
	    RelationshipType type = TestRelTypes.TEST_TYPE;
	    Node nodeWithFewRels = graphDb().createNode();
	    Node otherNode = createRelationships( nodeWithFewRels, type, 3000 );
	    createRelationships( otherNode, type, 50000 );
	    newTransaction();
	    
	    int count = 100;
	    
	    RelationshipExpander expander = TraversalFactory.expanderForTypes( type, Direction.BOTH );
	    for ( int i = 0; i < 1000; i++ )
	    {
	        graphDbUtil.getExistingRelationshipBetween( nodeWithFewRels, otherNode, expander );
	    }

        long total1 = 0;
        for ( int i = 0; i < count; i++ )
        {
            long t = System.currentTimeMillis();
            assertTrue( graphDbUtil.getExistingRelationshipBetween(
                    nodeWithFewRels, otherNode, expander ) != null );
            if ( i > 0 )
            {
                total1 += ( System.currentTimeMillis() - t );
            }
        }
        
	    long total2 = 0;
        for ( int i = 0; i < count; i++ )
        {
            long t = System.currentTimeMillis();
            assertTrue( graphDbUtil.getExistingRelationshipBetween(
                    otherNode, nodeWithFewRels,expander ) != null );
            if ( i > 0 )
            {
                total2 += ( System.currentTimeMillis() - t );
            }
        }
        
        System.out.println( "total1:" + total1 + ", total2:" + total2 );
	}
	
	private boolean goodOldLook( Node node1, Node node2, RelationshipType type,
	        Direction direction )
	{
	    for ( Relationship rel : node1.getRelationships( type, direction ) )
	    {
	        if ( rel.getOtherNode( node1 ).equals( node2 ) )
	        {
	            return true;
	        }
	    }
	    return false;
	}

    private Node createRelationships( Node node, RelationshipType type, int count )
    {
        Node lastNode = null;
        for ( int i = 0; i < count; i++ )
        {
            lastNode = graphDb().createNode();
            node.createRelationshipTo( lastNode, type );
        }
        return lastNode;
    }
    
    @Test
    public void testGetOrCreateSingleOtherNode()
    {
        Node node = graphDb().createNode();
        assertNull( GraphDatabaseUtil.getSingleOtherNode( node,
                TestRelTypes.TEST_TYPE, Direction.OUTGOING ) );
        Node otherNode = GraphDatabaseUtil.getOrCreateSingleOtherNode( node,
                TestRelTypes.TEST_TYPE, Direction.OUTGOING );
        assertNotNull( otherNode );
        assertEquals( otherNode, GraphDatabaseUtil.getSingleOtherNode(
                node, TestRelTypes.TEST_TYPE, Direction.OUTGOING ) );
        assertNull( GraphDatabaseUtil.getSingleOtherNode( node,
                TestRelTypes.TEST_OTHER_TYPE, Direction.OUTGOING ) );
        assertNull( GraphDatabaseUtil.getSingleOtherNode( node,
                TestRelTypes.TEST_TYPE, Direction.INCOMING ) );
        assertEquals( otherNode, GraphDatabaseUtil.getOrCreateSingleOtherNode(
                node, TestRelTypes.TEST_TYPE, Direction.OUTGOING ) );
        node.getSingleRelationship( TestRelTypes.TEST_TYPE, Direction.OUTGOING ).delete();
        assertNull( GraphDatabaseUtil.getSingleOtherNode( node,
                TestRelTypes.TEST_TYPE, Direction.OUTGOING ) );
    }
}
