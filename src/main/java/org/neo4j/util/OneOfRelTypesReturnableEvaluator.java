/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.TraversalPosition;

public class OneOfRelTypesReturnableEvaluator implements ReturnableEvaluator
{
	private final Set<String> types;
	
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
