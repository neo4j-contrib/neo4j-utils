package com.windh.util.neo;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.impl.core.NodeManager;

public class NeoStringSet extends NeoRelationshipSet<String>
{
	private static final String VALUE_KEY = "value";
	
	public NeoStringSet( Node node, RelationshipType type )
	{
		super( node, type );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		String value = ( String ) item;
		Node node = NodeManager.getManager().createNode();
		NeoUtil.getInstance().setProperty( node, VALUE_KEY, value );
		return node;
	}

	@Override
	protected String newObject( Node node, Relationship relationship )
	{
		return ( String ) NeoUtil.getInstance().getProperty(
			node, VALUE_KEY );
	}

	@Override
    protected Relationship findRelationship( Object item )
    {
		String value = ( String ) item;
		Relationship result = null;
		for ( Relationship rel : this.getAllRelationships() )
		{
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
