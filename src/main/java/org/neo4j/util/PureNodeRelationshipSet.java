package org.neo4j.util;

import java.util.Set;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;

/**
 * Convenience {@link Set} for when dealing with basic nodes and relationships
 * in contrast to {@link NodeWrapperRelationshipSet} where you're dealing with
 * wrappers around nodes.
 */
public class PureNodeRelationshipSet extends NeoRelationshipSet<Node>
{
	public PureNodeRelationshipSet( NeoService neo, Node node,
		RelationshipType type )
	{
		super( neo, node, type );
	}
	
	public PureNodeRelationshipSet( NeoService neo, Node node,
		RelationshipType type, Direction direction )
	{
		super( neo, node, type, direction );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( Node ) item;
	}
	
	@Override
	protected Node newObject( Node node, Relationship relationship )
	{
		return node;
	}
}
