package org.neo4j.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;

import test.NeoTest;

/**
 * Tests the neo relationship sets.
 * @author mattias
 *
 */
public class TestNeoRelationshipSet extends NeoTest
{
	/**
	 * Tests passing of illegal direction arguments.
	 */
	public void testIllegalDirection()
	{
		Transaction tx = neo().beginTx();
		try
		{
			Node node = neo().createNode();
			try
			{
				new ContainerSet( neo(), node, Direction.BOTH );
				new ContainerSet( neo(), node, null );
				fail( "Shouldn't be able to create a neo relationship set " +
					"with Direction.BOTH or null as direction" );
			}
			catch ( IllegalArgumentException e )
			{
				// Good
			}
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Tests some general use of collections.
	 */
	public void testSome()
	{
		Transaction tx = neo().beginTx();
		try
		{
			Node node = neo().createNode();
			SomeContainer container = new SomeContainer( node );
			Collection<SomeOtherContainer> set = container.otherContainers();
			Node itemNode = neo().createNode();
			Node itemNode2 = neo().createNode();
			SomeOtherContainer item = new SomeOtherContainer( itemNode );
			SomeOtherContainer item2 = new SomeOtherContainer( itemNode2 );
			
			assertTrue( set.isEmpty() );
			assertEquals( 0, set.size() );
			set.clear();
			assertTrue( set.isEmpty() );
			assertEquals( 0, set.size() );
			assertTrue( !set.contains( item ) );
			assertTrue( !set.contains( item2 ) );
			assertEquals( 0, set.toArray().length );

			assertTrue( set.add( item ) );
			assertTrue( !set.isEmpty() );
			assertEquals( 1, set.size() );
			assertTrue( set.contains( item ) );
			assertTrue( !set.contains( item2 ) );
			Iterator<SomeOtherContainer> iterator = set.iterator();
			assertTrue( iterator.hasNext() );
			assertEquals( iterator.next().getUnderlyingNode(),
				item.getUnderlyingNode() );
			assertTrue( !iterator.hasNext() );
			assertTrue( !set.add( item ) );
			assertTrue( !set.remove( item2 ) );
			assertEquals( 1, set.toArray().length );
			SomeOtherContainer[] array = set.toArray(
				new SomeOtherContainer[ set.size() ] );
			assertEquals( item.getUnderlyingNode(),
				array[ 0 ].getUnderlyingNode() );
			assertEquals( 1, array.length );
			
			assertTrue( set.remove( item ) );
			assertTrue( set.isEmpty() );
			assertEquals( 0, set.size() );
			set.clear();
			assertEquals( 0, set.size() );
			
			assertTrue( set.isEmpty() );
			set.add( item );
			assertTrue( !set.isEmpty() );
			set.add( item2 );
			assertTrue( !set.isEmpty() );
			assertTrue( set.contains( item ) );
			assertTrue( set.contains( item2 ) );
			assertEquals( 2, set.size() );
			set.add( item );
			assertEquals( 2, set.size() );
			set.remove( item );
			assertTrue( !set.contains( item ) );
			assertEquals( 1, set.size() );
			set.remove( item );
			assertEquals( 1, set.size() );
			set.remove( item2 );
			assertTrue( !set.contains( item2 ) );
			assertEquals( 0, set.size() );

			set.add( item );
			set.add( item2 );
			iterator = set.iterator();
			assertTrue( iterator.hasNext() );
			iterator.next();
			assertTrue( iterator.hasNext() );
			iterator.next();
			assertTrue( !iterator.hasNext() );
			set.clear();
			itemNode.delete();
			itemNode2.delete();
			node.delete();
			
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public void testRetain() throws Exception
	{
        Transaction tx = neo().beginTx();
        try
        {
            Node node = neo().createNode();
            Collection<Node> collection = new PureNodeRelationshipSet(
                neo(), node, Relationships.TESTREL );
            
            Node node1 = neo().createNode();
            Node node2 = neo().createNode();
            Node node3 = neo().createNode();
            Node node4 = neo().createNode();
            Node node5 = neo().createNode();
            Node node6 = neo().createNode();
            
            collection.add( node1 );
            collection.add( node2 );
            collection.add( node3 );
            collection.add( node4 );
            
            Collection<Node> newCollection = new HashSet<Node>();
            newCollection.add( node3 );
            newCollection.add( node4 );
            newCollection.add( node5 );
            newCollection.add( node6 );
            
            collection.addAll( newCollection );
            assertEquals( 6, collection.size() );
            collection.retainAll( newCollection );
            
            assertEquals( newCollection.size(), collection.size() );
            for ( Node shouldContain : newCollection )
            {
                assertTrue( collection.contains( shouldContain ) );
            }
            
            collection.clear();
            
            node.delete();
            node1.delete();
            node2.delete();
            node3.delete();
            node4.delete();
            node5.delete();
            node6.delete();
            
            tx.success();
        }
        finally
        {
            tx.finish();
        }
	}
	
	/**
	 * Tests so that all collection implementations can manage their own
	 * transactions.
	 * @throws Exception if something goes wrong.
	 */
	public void testWithoutTx() throws Exception
	{
		Node node = null;
		SomeOtherContainer entity1 = null;
		
		Transaction tx = neo().beginTx();
		try
		{
			node = neo().createNode();
			entity1 = new SomeOtherContainer( neo().createNode() );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
		
		
		NeoRelationshipSet<SomeOtherContainer> set =
			new ContainerSet( neo(), node );
		Collection<SomeOtherContainer> collection =
			Arrays.asList( new SomeOtherContainer[] { entity1 } );
		assertTrue( set.isEmpty() );
		assertTrue( set.add( entity1 ) );
		assertTrue( set.contains( entity1 ) );
		assertTrue( set.containsAll( collection ) );
		set.clear();
		assertTrue( set.addAll( collection ) );
		assertTrue( set.remove( entity1 ) );
		assertTrue( set.add( entity1 ) );
		assertTrue( set.removeAll( collection ) );
		assertTrue( set.add( entity1 ) );
		for ( SomeOtherContainer c : set )
		{
			assertNotNull( c );
		}
		assertTrue( !set.equals( new Object() ) );
		set.hashCode();
		assertEquals( 1, set.size() );
		assertEquals( entity1, set.toArray()[ 0 ] );
		assertEquals( 1, set.toArray().length );
		assertEquals( entity1,
			set.toArray( new SomeOtherContainer[ set.size() ] )[ 0 ] );
		assertEquals( 1,
			set.toArray( new SomeOtherContainer[ set.size() ] ).length );
		set.toString();
		set.clear();
		
		tx = neo().beginTx();
		try
		{
			node.delete();
			entity1.getUnderlyingNode().delete();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	private class SomeContainer extends NodeWrapperImpl
	{
		private SomeContainer( Node node )
		{
			super( neo(), node );
		}
		
		/**
		 * @return a collection of objects contained in this node.
		 */
		public Collection<SomeOtherContainer> otherContainers()
		{
			return new ContainerSet( neo(), getUnderlyingNode() );
		}
	}
	
	private class ContainerSet
		extends NeoRelationshipSet<SomeOtherContainer>
	{
		private ContainerSet( NeoService neo, Node node )
		{
			this( neo, node, Direction.OUTGOING );
		}
		
		private ContainerSet( NeoService neo, Node node, Direction direction )
		{
			super( neo, node, Relationships.TESTREL, direction );
		}

		@Override
		protected SomeOtherContainer newObject( Node node,
			Relationship relationship )
		{
			return new SomeOtherContainer( node );
		}

		@Override
		protected Node getNodeFromItem( Object item )
		{
			return ( ( NodeWrapperImpl ) item ).getUnderlyingNode();
		}
	}
	
	private static class SomeOtherContainer extends NodeWrapperImpl
	{
		private SomeOtherContainer( Node node )
		{
			super( neo(), node );
		}
	}
}
