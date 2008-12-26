package org.neo4j.util;

import java.util.Arrays;
import java.util.Collection;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.NeoPropertyArraySet;
import org.neo4j.util.NeoUtil;

import test.NeoTest;

public class TestNeoUtils extends NeoTest
{
	private NeoUtil neoUtil;
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		this.neoUtil = new NeoUtil( neo() );
	}
	
	public void testArrays()
	{
		Transaction tx = neo().beginTx();
		try
		{
			String key = "key_with_array_values";
			Node node = neo().createNode();
			int v1 = 10;
			int v2 = 101;
			int v3 = 2002;
			assertTrue( neoUtil.addValueToArray( node, key, v1 ) );
			assertCollection( neoUtil.neoPropertyAsList(
				node.getProperty( key ) ), v1 );
			assertFalse( neoUtil.addValueToArray( node, key, v1 ) );
			assertCollection( neoUtil.neoPropertyAsList(
				node.getProperty( key ) ), v1 );
			assertTrue( neoUtil.addValueToArray( node, key, v2 ) );
			assertCollection( neoUtil.neoPropertyAsList(
				node.getProperty( key ) ), v1, v2 );
			assertTrue( neoUtil.addValueToArray( node, key, v3 ) );
			assertCollection( neoUtil.neoPropertyAsList(
				node.getProperty( key ) ), v1, v2, v3 );
			assertTrue( neoUtil.removeValueFromArray( node, key, v2 ) );
			assertCollection( neoUtil.neoPropertyAsList(
				node.getProperty( key ) ), v1, v3 );
			assertFalse( neoUtil.removeValueFromArray( node, key, v2 ) );
			assertCollection( neoUtil.neoPropertyAsList(
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
		Transaction tx = neo().beginTx();
		try
		{
			String key = "key_with_array_values";
			Node node = neo().createNode();
			int v1 = 10;
			int v2 = 101;
			int v3 = 2002;
			Collection<Integer> values = new NeoPropertyArraySet<Integer>(
				neo(), node, key );
			assertTrue( values.add( v1 ) );
			assertCollection( values, v1 );
			assertFalse( values.add( v1 ) );
			assertCollection( values, v1 );
			assertTrue( values.add( v2 ) );
			assertCollection( values, v1, v2 );
			assertTrue( values.add( v3 ) );
			assertCollection( values, v1, v2, v3 );
			assertCollection( neoUtil.neoPropertyAsList(
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
	    Transaction tx = neo().beginTx();
	    try
	    {
	        Node node1 = neo().createNode();
	        Node node2 = neo().createNode();
	        Node node3 = neo().createNode();
	        Node node4 = neo().createNode();
	        
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
	        
	        new NeoUtil( neo() ).sumNodeContents( node1 );
	        
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
