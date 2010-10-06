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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * Super class of tests which handles Neo4j-specific things.
 * @author mattias
 */
public abstract class Neo4jTest
{
	protected static enum TestRelTypes implements RelationshipType
	{
		TEST_TYPE,
		TEST_OTHER_TYPE,
		TEST_YET_ANOTHER_TYPE,
	}
	
	private static GraphDatabaseService graphDb;
	private Transaction tx;
	
	@BeforeClass
	public static void setUpDb()
	{
        String dbPath = "target/var/neo4j";
        File path = new File( dbPath );
        if ( path.exists() )
        {
            for ( File file : path.listFiles() )
            {
                file.delete();
            }
        }
        graphDb = new EmbeddedGraphDatabase( dbPath );
	}
	
	@AfterClass
	public static void tearDownDb()
	{
	    graphDb.shutdown();
	}
	
	protected static GraphDatabaseService graphDb()
	{
		return graphDb;
	}
	
	@Before
	public void beginTx()
	{
	    if ( tx == null )
	    {
	        tx = graphDb.beginTx();
	    }
	}
	
	@After
	public void commitTx()
	{
	    if ( tx != null )
	    {
    	    tx.success();
    	    tx.finish();
    	    tx = null;
	    }
	}
	
	protected static <T> void assertCollection( Collection<T> collection, T... items )
	{
		String collectionString = join( ", ", collection.toArray() );
		assertEquals( collectionString, items.length, collection.size() );
		for ( T item : items )
		{
			assertTrue( collection.contains( item ) );
		}
	}

	protected static <T> String join( String delimiter, T... items )
	{
		StringBuffer buffer = new StringBuffer();
		for ( T item : items )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( delimiter );
			}
			buffer.append( item.toString() );
		}
		return buffer.toString();
	}
}
