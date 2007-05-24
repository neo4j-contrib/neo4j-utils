package com.windh.util.neo;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;

/**
 * Migration unit with a simple default implementation using reflection to
 * find the migrators and a normal sub reference node as the config node.
 */
public abstract class SimpleMigration extends Migration
{
	private String versionClassPrefix;
	
	public SimpleMigration( RelationshipType subReferenceType )
	{
		super( getConfigNodeFromType( subReferenceType ) );
		this.versionClassPrefix = this.getMigratorPrefix();
	}
	
	private static Node getConfigNodeFromType( RelationshipType type )
	{
		return NeoUtil.getInstance().getOrCreateSubReferenceNode( type );
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
