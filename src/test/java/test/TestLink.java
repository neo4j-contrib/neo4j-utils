package test;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.core.NodeManager;
import com.windh.util.neo.Link;
import com.windh.util.neo.LinkImpl;
import com.windh.util.neo.NodeWrapper;

public class TestLink extends NeoTest
{
	public void testOne() throws Exception
	{
		this.doSomeTesting( Direction.OUTGOING );
	}
	
	public void testOther() throws Exception
	{
		this.doSomeTesting( Direction.INCOMING );
	}

	private void doSomeTesting( Direction direction ) throws Exception
	{
		Transaction tx = Transaction.begin();
		try
		{
			Node node1 = NodeManager.getManager().createNode();
			Node node2 = NodeManager.getManager().createNode();
			
			Entity entity1 = NodeWrapper.newInstance( Entity.class, node1 );
			Entity entity2 = NodeWrapper.newInstance( Entity.class, node2 );

			Link<Entity> link = new LinkImpl<Entity>(
				entity1.getUnderlyingNode(), Relationships.TESTREL,
				Entity.class, direction );
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
	
	public static class Entity extends NodeWrapper
	{
		public Entity( Node node )
		{
			super( node );
		}
	}
}
