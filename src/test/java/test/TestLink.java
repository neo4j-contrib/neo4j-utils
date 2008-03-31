package test;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.Link;
import org.neo4j.util.LinkImpl;
import org.neo4j.util.NodeWrapperImpl;

/**
 * Tests the {@link Link} class and its implementation {@link LinkImpl}.
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
			
			Entity entity1 = NodeWrapperImpl.newInstance( Entity.class, node1 );
			Entity entity2 = NodeWrapperImpl.newInstance( Entity.class, node2 );

			Link<Entity> link = new LinkImpl<Entity>( neo(),
				entity1.getUnderlyingNode(), Relationships.TESTREL, direction,
				Entity.class );
			assertTrue( !link.has() );
			try
			{
				link.get();
				fail( "ERROR, has got" );
			}
			catch ( Exception e )
			{ // Good
			}
	
			try
			{
				link.remove();
				fail( "ERROR, has got remove" );
			}
			catch ( Exception e )
			{ // Good
			}
	
			link.set( entity2 );
			assertTrue( link.has() );
			link.get();
			link.remove();
			assertTrue( !link.has() );
			link.set( entity2 );
			link.set( entity2 );
	
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
		public Entity( Node node )
		{
			super( node );
		}
	}
}
