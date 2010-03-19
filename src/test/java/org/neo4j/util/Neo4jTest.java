package org.neo4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
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
