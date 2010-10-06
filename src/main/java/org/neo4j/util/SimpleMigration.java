/**
 * Copyright (c) 2002-2010 "Neo Technology,"
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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

/**
 * Migration unit with a simple default implementation using reflection to
 * find the migrators and a normal sub reference node as the config node.
 */
public abstract class SimpleMigration extends Migration
{
	private String versionClassPrefix;
	
	/**
	 * @param graphDb the {@link GraphDatabaseService} instance to store migration info in.
	 * @param subReferenceType the {@link RelationshipType} to use a sub
	 * reference type.
	 */
	public SimpleMigration( GraphDatabaseService graphDb,
	        RelationshipType subReferenceType )
	{
		super( graphDb, getConfigNodeFromType( graphDb, subReferenceType ) );
		this.versionClassPrefix = this.getMigratorPrefix();
	}
	
	public SimpleMigration( GraphDatabaseService graphDb )
	{
		this( graphDb, MigrationRelationshipTypes.REF_MIGRATION );
	}
	
	private static Node getConfigNodeFromType( GraphDatabaseService graphDb,
		RelationshipType type )
	{
	    Transaction tx = graphDb.beginTx();
	    try
	    {
	        Node result = new GraphDatabaseUtil( graphDb ).getOrCreateSubReferenceNode( type );
	        tx.success();
	        return result;
	    }
	    finally
	    {
	        tx.finish();
	    }
	}
	
	protected String getMigratorPrefix()
	{
		String className = this.getClass().getName();
		int dotIndex = className.lastIndexOf( '.' );
		String result = className.substring( 0, dotIndex + 1 ) + "Migrator";
		return result;
	}
	
	@Override
	protected Migrator findMigrator( int version )
	{
		String className = this.versionClassPrefix + version;
		try
		{
			Class<? extends Migrator> cls =
				Class.forName( className ).asSubclass( Migrator.class );
			return cls.newInstance();
		}
		catch ( RuntimeException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	public static enum MigrationRelationshipTypes implements RelationshipType
	{
		REF_MIGRATION,
	}
}
