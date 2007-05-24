package com.windh.util.neo.neoshell.apps;

import com.windh.util.neo.neoshell.NeoApp;
import com.windh.util.shell.CommandParser;
import com.windh.util.shell.Output;
import com.windh.util.shell.Session;
import com.windh.util.shell.ShellException;

public class Rm extends NeoApp
{
	@Override
	public String getDescription()
	{
		return "Removes a property";
	}

	@Override
	protected String exec( CommandParser parser, Session session, Output out )
		throws ShellException
	{
		String key = parser.arguments().get( 0 );
		this.getCurrentNode( session ).removeProperty( key );
		return null;
	}
}
