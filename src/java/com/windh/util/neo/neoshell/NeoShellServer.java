package com.windh.util.neo.neoshell;

import java.rmi.RemoteException;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.RelationshipType;
import com.windh.util.neo.neoshell.apps.Ls;
import com.windh.util.shell.AbstractClient;
import com.windh.util.shell.SimpleServer;

public class NeoShellServer extends SimpleServer
{
	private EmbeddedNeo neo;
	private Class<? extends RelationshipType> relTypeClass;
	
	public NeoShellServer( EmbeddedNeo neo,
		Class<? extends RelationshipType> relTypeClass ) throws RemoteException
	{
		super();
		this.addPackage( Ls.class.getPackage().getName() );
		this.neo = neo;
		this.relTypeClass = relTypeClass;
		this.setProperty( AbstractClient.PROMPT_KEY, "neo-sh$ " );
	}
	
	public NeoShellServer( EmbeddedNeo neo ) throws RemoteException
	{
		this( neo, neo.getRelationshipTypes() );
	}

	@Override
	public String welcome()
	{
		return "Welcome to NeoShell";
	}
	
	public EmbeddedNeo getNeo()
	{
		return this.neo;
	}
	
	public Class<? extends RelationshipType> getRelationshipTypeClass()
	{
		return this.relTypeClass;
	}
}
