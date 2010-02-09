package org.neo4j.util;

import java.io.File;
import java.util.Collection;

import junit.framework.TestCase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

/**
 * Super class of tests which handles Neo4j-specific things.
 * @author mattias
 */
public abstract class Neo4jTest extends TestCase
{
	protected static enum TestRelTypes implements RelationshipType
	{
		TEST_TYPE,
		TEST_OTHER_TYPE,
		TEST_YET_ANOTHER_TYPE,
	}
	
	private static GraphDatabaseService graphDb;
	
	@Override
	protected void setUp() throws Exception
	{
		if ( graphDb() == null )
		{
			init();
		}
	}
	
	private void init() throws Exception
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
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		} );
	}
	
	protected static GraphDatabaseService graphDb()
	{
		return graphDb;
	}
	
	/**
	 * Relationships used in this test suite.
	 * @author mattias
	 */
	public static enum Relationships implements RelationshipType
	{
		/**
		 * A relationship type to use in tests.
		 */
		TESTREL
	}
	
	protected <T> void assertCollection( Collection<T> collection, T... items )
	{
		String collectionString = join( ", ", collection.toArray() );
		assertEquals( collectionString, items.length, collection.size() );
		for ( T item : items )
		{
			assertTrue( collection.contains( item ) );
		}
	}

	protected <T> String join( String delimiter, T... items )
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
