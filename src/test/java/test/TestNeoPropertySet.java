package test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.core.NodeManager;
import org.neo4j.util.NeoPropertySet;

public class TestNeoPropertySet extends NeoTest
{
	public void testSome()
	{
		Node node = null;
		Transaction tx = Transaction.begin();
		try
		{
			node = NodeManager.getManager().createNode();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
		
		Collection<Integer> set = new TestSet( node, "ids" );
		Integer item = 10;
		Integer item2 = 21211;
		
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
		Iterator<Integer> iterator = set.iterator();
		assertTrue( iterator.hasNext() );
		assertEquals( item, iterator.next() );
		assertTrue( !iterator.hasNext() );
		assertTrue( !set.add( item ) );
		assertTrue( !set.remove( item2 ) );
		assertEquals( 1, set.toArray().length );
		Integer[] array = set.toArray( new Integer[ set.size() ] );
		assertEquals( item, array[ 0 ] );
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

		Collection<Object> items = new ArrayList<Object>();
		assertTrue( !set.retainAll( items ) );
		set.add( item );
		set.add( item2 );
		items.add( item );
		items.add( item2 );
		assertTrue( !set.retainAll( items ) );
		items.add( "kdjfgdkjk" );
		assertTrue( !set.retainAll( items ) );
		
		items.clear();
		items.add( item2 );
		assertTrue( set.retainAll( items ) );
		assertEquals( 1, set.size() );
		assertEquals( 1, set.toArray().length );
		assertEquals( item2, set.toArray( new Integer[ 1 ] )[ 0 ] );
		
		tx = Transaction.begin();
		try
		{
			node.delete();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	private static class TestSet extends NeoPropertySet<Integer>
	{
		TestSet( Node node, String key )
		{
			super( node, key, "," );
		}

		@Override
		protected String itemToString( Object item )
		{
			return item.toString();
		}

		@Override
		protected Integer stringToItem( String string )
		{
			return new Integer( string );
		}
	}
}
