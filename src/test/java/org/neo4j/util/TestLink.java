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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

/**
 * Tests the {@link Link} class and its implementation {@link NodeWrapperLink}.
 * @author mattias
 *
 */
public class TestLink extends Neo4jTest
{
	/**
	 * Tests link functionality with normal outgoing direction.
	 * @throws Exception if something goes wrong.
	 */
    @Test
    public void testOne() throws Exception
	{
		this.doSomeTesting( Direction.OUTGOING );
	}
	
	/**
	 * Tests link functionality with incoming direction.
	 * @throws Exception if something goes wrong.
	 */
    @Test
    public void testOther() throws Exception
	{
		this.doSomeTesting( Direction.INCOMING );
	}

    private void doSomeTesting( Direction direction ) throws Exception
	{
		Node node1 = graphDb().createNode();
		Node node2 = graphDb().createNode();
        Node node3 = graphDb().createNode();
		
		Entity entity1 = NodeWrapperImpl.newInstance( Entity.class, node1 );
		Entity entity2 = NodeWrapperImpl.newInstance( Entity.class, node2 );
        Entity entity3 = NodeWrapperImpl.newInstance( Entity.class, node3 );

		Link<Entity> link = new NodeWrapperLink<Entity>( graphDb(),
			entity1.getUnderlyingNode(), TestRelTypes.TEST_TYPE, direction,
			Entity.class );
		assertTrue( !link.has() );
		assertNull( link.get() );
		assertNull( link.remove() );
		assertNull( link.set( entity2 ) );
		assertTrue( link.has() );
		assertEquals( entity2, link.get() );
		assertEquals( entity2, link.remove() );
		assertTrue( !link.has() );
		assertNull( link.set( entity2 ) );
		assertEquals( entity2, link.set( entity3 ) );
		link.remove();
		
		node1.delete();
		node2.delete();
		node3.delete();
	}
	
	/**
	 * Simple node wrapper class for testing.
	 * @author mattias
	 */
	public static class Entity extends NodeWrapperImpl
	{
		/**
		 * @param node the underlying node.
		 */
		public Entity( Node node )
		{
			super( node );
		}
	}
}
