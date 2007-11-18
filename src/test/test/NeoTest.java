package test;

import java.io.File;
import junit.framework.TestCase;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.RelationshipType;

public class NeoTest extends TestCase
{
	private static boolean initialized;
	
	@Override
	public void setUp() throws Exception
	{
		if ( !initialized )
		{
			init();
			initialized = true;
		}
	}
	
	private void init() throws Exception
	{
		String dbPath = "var/nioneo";
		File path = new File( dbPath );
		if ( path.exists() )
		{
			for ( File file : path.listFiles() )
			{
				file.delete();
			}
		}
		
		final EmbeddedNeo neo = new EmbeddedNeo( Relationships.class, dbPath );
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				neo.shutdown();
			}
		} );
	}
	
	public void testNothing()
	{
	}

	public static enum Relationships implements RelationshipType
	{
		TESTREL
	}
}
