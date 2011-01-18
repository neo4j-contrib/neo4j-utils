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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * Tests the relationship sets.
 */
public class TestRelationshipSet extends Neo4jTest
{
	/**
	 * Tests passing of illegal direction arguments.
	 */
    @Test
	public void testIllegalDirection()
	{
		Node node = graphDb().createNode();
		try
		{
			new ContainerSet( node, Direction.BOTH );
			new ContainerSet( node, null );
			fail( "Shouldn't be able to create a relationship set " +
				"with Direction.BOTH or null as direction" );
		}
		catch ( IllegalArgumentException e )
		{
			// Good
		}
	}
	
	/**
	 * Tests some general use of collections.
	 */
    @Test
    public void testSome()
	{
		Node node = graphDb().createNode();
		SomeContainer container = new SomeContainer( node );
		Collection<SomeOtherContainer> set = container.otherContainers();
		Node itemNode = graphDb().createNode();
		Node itemNode2 = graphDb().createNode();
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
	}
	
    @Test
    public void testRetain() throws Exception
	{
        Node node = graphDb().createNode();
        Collection<Node> collection = new NodeRelationshipSet(
            node, TestRelTypes.TEST_TYPE );
        
        Node node1 = graphDb().createNode();
        Node node2 = graphDb().createNode();
        Node node3 = graphDb().createNode();
        Node node4 = graphDb().createNode();
        Node node5 = graphDb().createNode();
        Node node6 = graphDb().createNode();
        
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
	}
	
	/**
	 * Tests so that all collection implementations can manage their own
	 * transactions.
	 * @throws Exception if something goes wrong.
	 */
	@Test
    public void testWithoutTx() throws Exception
	{
		Node node = null;
		SomeOtherContainer entity1 = null;
		
		node = graphDb().createNode();
		entity1 = new SomeOtherContainer( graphDb().createNode() );
		RelationshipSet<SomeOtherContainer> set = new ContainerSet( node );
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
		
		node.delete();
		entity1.getUnderlyingNode().delete();
	}
	
	private class SomeContainer extends NodeWrapperImpl
	{
		private SomeContainer( Node node )
		{
			super( node );
		}
		
		/**
		 * @return a collection of objects contained in this node.
		 */
		public Collection<SomeOtherContainer> otherContainers()
		{
			return new ContainerSet( getUnderlyingNode() );
		}
	}
	
	private class ContainerSet
		extends RelationshipSet<SomeOtherContainer>
	{
		private ContainerSet( Node node )
		{
			this( node, Direction.OUTGOING );
		}
		
		private ContainerSet( Node node, Direction direction )
		{
			super( node, TestRelTypes.TEST_TYPE, direction );
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
			super( node );
		}
	}
}
