package test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.core.NodeManager;
import com.windh.util.neo.NeoRelationshipSet;
import com.windh.util.neo.NodeWrapper;

public class TestNeoRelationshipSet extends NeoTest
{
	public void testSome()
	{
		Transaction tx = Transaction.begin();
		try
		{
			Node node = NodeManager.getManager().createNode();
			SomeContainer container = new SomeContainer( node );
			Set<SomeOtherContainer> set = container.otherContainers();
			Node itemNode = NodeManager.getManager().createNode();
			Node itemNode2 = NodeManager.getManager().createNode();
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
	
	public void testWithoutTx() throws Exception
	{
		Node node = null;
		SomeOtherContainer entity1 = null;
		
		Transaction tx = Transaction.begin();
		try
		{
			node = NodeManager.getManager().createNode();
			entity1 = new SomeOtherContainer(
				NodeManager.getManager().createNode() );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
		
		
		NeoRelationshipSet<SomeOtherContainer> set = new ContainerSet( node );
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
		
		tx = Transaction.begin();
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
	
	private static class SomeContainer extends NodeWrapper
	{
		private SomeContainer( Node node )
		{
			super( node );
		}
		
		public Set<SomeOtherContainer> otherContainers()
		{
			return new ContainerSet( getUnderlyingNode() );
		}
	}
	
	private static class ContainerSet
		extends NeoRelationshipSet<SomeOtherContainer>
	{
		private ContainerSet( Node node )
		{
			super( node, Relationships.TESTREL );
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
			return ( ( NodeWrapper ) item ).getUnderlyingNode();
		}
	}
	
	private static class SomeOtherContainer extends NodeWrapper
	{
		private SomeOtherContainer( Node node )
		{
			super( node );
		}
	}
}
