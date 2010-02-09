package org.neo4j.util;

import java.util.Arrays;
import java.util.Collection;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.util.PropertyArraySet;
import org.neo4j.util.GraphDatabaseUtil;

public class TestGraphDbUtils extends Neo4jTest
{
	private GraphDatabaseUtil graphDbUtil;
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		this.graphDbUtil = new GraphDatabaseUtil( graphDb() );
	}
	
	public void testArrays()
	{
		Transaction tx = graphDb().beginTx();
		try
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
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public void testArraySet()
	{
		Transaction tx = graphDb().beginTx();
		try
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
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public void testSumContents() throws Exception
	{
	    Transaction tx = graphDb().beginTx();
	    try
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
	        
	        tx.success();
	    }
	    finally
	    {
	        tx.finish();
	    }
	}
}
