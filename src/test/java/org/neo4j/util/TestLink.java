package org.neo4j.util;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import test.NeoTest;

/**
 * Tests the {@link Link} class and its implementation {@link NodeWrapperLink}.
 * @author mattias
 *
 */
public class TestLink extends NeoTest
{
	/**
	 * Tests link functionality with normal outgoing direction.
	 * @throws Exception if something goes wrong.
	 */
	public void testOne() throws Exception
	{
		this.doSomeTesting( Direction.OUTGOING );
	}
	
	/**
	 * Tests link functionality with incoming direction.
	 * @throws Exception if something goes wrong.
	 */
	public void testOther() throws Exception
	{
		this.doSomeTesting( Direction.INCOMING );
	}

	private void doSomeTesting( Direction direction ) throws Exception
	{
		Transaction tx = neo().beginTx();
		try
		{
			Node node1 = neo().createNode();
			Node node2 = neo().createNode();
            Node node3 = neo().createNode();
			
			Entity entity1 = NodeWrapperImpl.newInstance( Entity.class, neo(),
				node1 );
			Entity entity2 = NodeWrapperImpl.newInstance( Entity.class, neo(),
				node2 );
            Entity entity3 = NodeWrapperImpl.newInstance( Entity.class, neo(),
                node3 );

			Link<Entity> link = new NodeWrapperLink<Entity>( neo(),
				entity1.getUnderlyingNode(), Relationships.TESTREL, direction,
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
	
			tx.success();
		}
		finally
		{
			tx.finish();
		}
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
		public Entity( GraphDatabaseService neo, Node node )
		{
			super( neo, node );
		}
	}
}
