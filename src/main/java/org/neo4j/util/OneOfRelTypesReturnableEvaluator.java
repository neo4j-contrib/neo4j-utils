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
	private Set<String> types;
	
	public OneOfRelTypesReturnableEvaluator( RelationshipType... types )
	{
		this.types = new HashSet<String>();
		for ( RelationshipType type : types )
		{
			this.types.add( type.name() );
		}
	}
	
	public OneOfRelTypesReturnableEvaluator( String... names )
	{
		this.types = new HashSet<String>( Arrays.asList( names ) );
	}
	
	public boolean isReturnableNode( TraversalPosition currentPos )
	{
		Relationship rel = currentPos.lastRelationshipTraversed();
		return rel != null && this.types.contains( rel.getType().name() );
	}
}
