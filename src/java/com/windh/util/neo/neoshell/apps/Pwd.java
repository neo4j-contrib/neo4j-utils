package com.windh.util.neo.neoshell.apps;

import java.rmi.RemoteException;
import java.util.List;
import org.neo4j.api.core.Node;
import com.windh.eqn.util.TextUtil;
import com.windh.util.neo.neoshell.NeoApp;
import com.windh.util.shell.CommandParser;
import com.windh.util.shell.Output;
import com.windh.util.shell.Session;
import com.windh.util.shell.ShellException;

public class Pwd extends NeoApp
{
	@Override
	public String getDescription()
	{
		return "Prints path to current node";
	}

	@Override
	protected String exec( CommandParser parser, Session session, Output out )
		throws ShellException, RemoteException
	{
		Node currentNode = this.getCurrentNode( session );
		out.println( "Current node is " + currentNode );
		StringBuffer buffer = new StringBuffer();
		List<Long> paths = Cd.readPaths( session );
		paths.add( currentNode.getId() );
		for ( Long id : paths )
		{
			TextUtil.getInstance().append( buffer, "[" + id + "]", " --> " );
		}
		if ( buffer.length() > 0 )
		{
			out.println( buffer.toString() );
		}
		return null;
	}
}
