/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.neo4j.graphdb.Node;

public class TestIndexedSet extends TxNeo4jTest
{
    @Test
    public void testIndexedSet() throws Exception
	{
		Node rootNode = graphDb().createNode();
		Collection<AnItem> collection = new SortedNodeCollection<AnItem>(
			rootNode, new AnItemComparator(), AnItem.class );
		
		List<String> strings = new ArrayList<String>( Arrays.asList(
			"Mattias",
			"Persson",
			"Went",
			"And",
			"Implemented",
			"An",
			"Indexed",
			"GraphDb",
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
		
		// Compare the indexed node collection against a natural sorting order
		Collections.sort( strings );
		assertCollectionSame( strings, collection );
		
		String toRemove = "Persson";
		AnItem toRemoveItem = findItem( collection, toRemove );
		assertTrue( collection.remove( toRemoveItem ) );
		assertTrue( strings.remove( toRemove ) );
		assertCollectionSame( strings, collection );
		
		collection.clear();
		( ( SortedNodeCollection<AnItem> ) collection ).delete();
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
			String indexed = item.getName();
			assertEquals( naturalOrder, indexed );
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
			Node node = graphDb().createNode();
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
