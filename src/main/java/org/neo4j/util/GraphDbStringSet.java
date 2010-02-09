package org.neo4j.util;

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

/**
 * A String collection, persistent in neo.
 */
public class GraphDbStringSet extends RelationshipSet<String>
{
	private static final String VALUE_KEY = "value";
	
	/**
	 * @param neo the {@link GraphDatabaseService} to use.
	 * @param node the {@link Node} which is the collection node.
	 * @param type the relationship type to use internally for each element.
	 */
	public GraphDbStringSet( GraphDatabaseService neo, Node node, RelationshipType type )
	{
		super( neo, node, type );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		String value = ( String ) item;
		Transaction tx = graphDb().beginTx();
		try
		{
			Node node = graphDb().createNode();
			node.setProperty( VALUE_KEY, value );
			tx.success();
			return node;
		}
		finally
		{
			tx.finish();
		}
	}

	@Override
	protected String newObject( Node node, Relationship relationship )
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			return ( String ) node.getProperty( VALUE_KEY );
		}
		finally
		{
			tx.finish();
		}
	}

	@Override
	protected Relationship findRelationship( Object item )
	{
		String value = ( String ) item;
		Relationship result = null;
		Iterator<Relationship> itr = this.getAllRelationships();
		while ( itr.hasNext() )
		{
			Relationship rel = itr.next();
			Node node = rel.getOtherNode( this.getUnderlyingNode() );
			String nodeValue = newObject( node, rel );
			if ( value.equals( nodeValue ) )
			{
				result = rel;
				break;
			}
		}
		return result;
	}
	
	@Override
	protected void removeItem( Relationship rel )
	{
		Node node = this.getOtherNode( rel );
		super.removeItem( rel );
		node.delete();
	}
}
