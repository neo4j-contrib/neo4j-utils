package org.neo4j.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.neo4j.api.core.Node;

import test.TxNeoTest;

public class TestIndexedSet extends TxNeoTest
{
	public void testIndexedSet() throws Exception
	{
		Node rootNode = neo().createNode();
		Collection<AnItem> collection = new IndexedNeoCollection<AnItem>( neo(),
			rootNode, new AnItemComparator(), AnItem.class );
		
		List<String> strings = new ArrayList<String>( Arrays.asList(
			"Mattias",
			"Persson",
			"Went",
			"And",
			"Implemented",
			"An",
			"Indexed",
			"Neo",
			"Set",
			"And",
			"How",
			"About",
			"That"
		) );
		
		for ( String string : strings )
		{
			assertTrue( collection.add( new AnItem( string ) ) );
		}
		
		// Compare the indexed neo collection against a natural sorting order
		Collections.sort( strings );
		assertCollectionSame( strings, collection );
		
		String toRemove = "Persson";
		AnItem toRemoveItem = findItem( collection, toRemove );
		assertTrue( collection.remove( toRemoveItem ) );
		assertTrue( strings.remove( toRemove ) );
		assertCollectionSame( strings, collection );
		
		collection.clear();
		( ( IndexedNeoCollection<AnItem> ) collection ).delete();
		rootNode.delete();
		for ( Node node : AnItem.createdNodes )
		{
			node.delete();
		}
	}
	
	private AnItem findItem( Collection<AnItem> collection, String name )
	{
		for ( AnItem item : collection )
		{
			if ( item.getName().equals( name ) )
			{
				return item;
			}
		}
		fail( "'" + name + "' not found" );
		return null;
	}
	
	private void assertCollectionSame( List<String> strings,
		Collection<AnItem> collection )
	{
		assertEquals( strings.size(), collection.size() );
		int i = 0;
		for ( AnItem item : collection ) 
		{
			String naturalOrder = strings.get( i++ );
			String neoIndexed = item.getName();
			assertEquals( naturalOrder, neoIndexed );
		}
	}
	
	private static class AnItem extends NodeWrapperImpl
	{
		static Collection<Node> createdNodes = new ArrayList<Node>();
		
		public AnItem( Node node )
		{
			super( node );
		}
		
		public AnItem( String name )
		{
			this( newNode(), name );
		}
		
		private static Node newNode()
		{
			Node node = neo().createNode();
			createdNodes.add( node );
			return node;
		}
		
		public AnItem( Node node, String name )
		{
			this( node );
			node.setProperty( "name", name );
		}
		
		public String getName()
		{
			return ( String ) getUnderlyingNode().getProperty( "name" );
		}
	}
	
	private static class AnItemComparator implements Comparator<AnItem>
	{
		public int compare( AnItem o1, AnItem o2 )
		{
			return o1.getName().compareTo( o2.getName() );
		}
	}
}
