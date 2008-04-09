package org.neo4j.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.TraversalPosition;

public class OneOfRelTypesReturnableEvaluator implements ReturnableEvaluator
{
	private Set<RelationshipType> types;
	
	public OneOfRelTypesReturnableEvaluator( RelationshipType... types )
	{
		this.types = new HashSet<RelationshipType>( Arrays.asList( types ) );
	}
	
	public boolean isReturnableNode( TraversalPosition currentPos )
	{
		Relationship rel = currentPos.lastRelationshipTraversed();
		return rel != null && this.types.contains( rel.getType() );
	}
}
