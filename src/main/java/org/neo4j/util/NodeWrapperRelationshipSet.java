package org.neo4j.util;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;

public class NodeWrapperRelationshipSet<T extends NodeWrapper>
	extends NeoRelationshipSet<T>
{
	private Class<T> instanceClass;
	
	public NodeWrapperRelationshipSet( Node node, RelationshipType type,
		Class<T> instanceClass )
	{
		super( node, type );
		this.instanceClass = instanceClass;
	}
	
	public NodeWrapperRelationshipSet( Node node, Direction direction,
		RelationshipType type, Class<T> instanceClass )
	{
		super( node, direction, type );
		this.instanceClass = instanceClass;
	}
	
	protected Class<T> getInstanceClass()
	{
		return this.instanceClass;
	}

	@Override
	protected T newObject( Node node, Relationship relationship )
	{
		return NodeWrapper.newInstance( this.getInstanceClass(), node );
	}
	
	@Override
	protected Node getNodeFromItem( Object item )
	{
		return ( ( NodeWrapper ) item ).getUnderlyingNode();
	}
}
