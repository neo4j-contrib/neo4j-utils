package org.neo4j.util;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;

/**
 * Migration unit with a simple default implementation using reflection to
 * find the migrators and a normal sub reference node as the config node.
 */
public abstract class SimpleMigration extends Migration
{
	private String versionClassPrefix;
	
	/**
	 * @param neo the {@link EmbeddedNeo} instance to store migration info in.
	 * @param subReferenceType the {@link RelationshipType} to use a sub
	 * reference type.
	 */
	public SimpleMigration( EmbeddedNeo neo, RelationshipType subReferenceType )
	{
		super( getConfigNodeFromType( neo, subReferenceType ) );
		this.versionClassPrefix = this.getMigratorPrefix();
	}
	
	private static Node getConfigNodeFromType( EmbeddedNeo neo,
		RelationshipType type )
	{
		return new NeoUtil( neo ).getOrCreateSubReferenceNode( type );
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
			Class<? extends Migrator> cls = ( Class<? extends Migrator> )
				Class.forName( className );
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
}
