package test;

import java.io.File;
import junit.framework.TestCase;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.RelationshipType;

/**
 * Super class of tests which handles neo-specific things.
 * @author mattias
 */
public class NeoTest extends TestCase
{
	private static NeoService neo;
	
	@Override
	public void setUp() throws Exception
	{
		if ( neo == null )
		{
			init();
		}
	}
	
	private void init() throws Exception
	{
		String dbPath = "var/neo";
		File path = new File( dbPath );
		if ( path.exists() )
		{
			for ( File file : path.listFiles() )
			{
				file.delete();
			}
		}
		
		neo = new EmbeddedNeo( dbPath );
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				neo.shutdown();
			}
		} );
	}
	
	protected static NeoService neo()
	{
		return neo;
	}
	
	/**
	 * To avoid a warning about (there's no tests in this class).
	 */
	public void testNothing()
	{
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
}
