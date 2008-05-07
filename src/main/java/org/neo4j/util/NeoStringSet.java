package org.neo4j.util;

import java.util.Iterator;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;

/**
 * A String collection, persistent in neo.
 */
public class NeoStringSet extends NeoRelationshipSet<String>
{
	private static final String VALUE_KEY = "value";
	
	private NeoService neo;
	
	/**
	 * @param neo the {@link NeoService} to use.
	 * @param node the {@link Node} which is the collection node.
	 * @param type the relationship type to use internally for each element.
	 */
	public NeoStringSet( NeoService neo, Node node, RelationshipType type )
	{
		super( node, type );
		this.neo = neo;
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		String value = ( String ) item;
		Transaction tx = Transaction.begin();
		try
		{
			Node node = neo.createNode();
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
		Transaction tx = Transaction.begin();
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
