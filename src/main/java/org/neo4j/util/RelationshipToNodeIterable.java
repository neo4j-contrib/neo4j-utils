package org.neo4j.util;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.commons.iterator.IterableWrapper;

public class RelationshipToNodeIterable
	extends IterableWrapper<Node, Relationship>
{
	private Node startNode;
	
	public RelationshipToNodeIterable( Node startNode,
		Iterable<Relationship> relationships )
	{
		super( relationships );
		this.startNode = startNode;
	}

	@Override
	protected Node underlyingObjectToObject( Relationship relationship )
	{
		return relationship.getOtherNode( startNode );
	}
}
