package com.windh.util.neo.neoshell.apps;

import java.rmi.RemoteException;
import com.windh.util.neo.neoshell.NeoApp;
import com.windh.util.shell.CommandParser;
import com.windh.util.shell.Output;
import com.windh.util.shell.Session;
import com.windh.util.shell.ShellException;

public class Env extends NeoApp
{
	@Override
	public String getDescription()
	{
		return "Lists all environment variables";
	}
	
	@Override
	protected String exec( CommandParser parser, Session session, Output out )
		throws ShellException, RemoteException
	{
		for ( String key : session.keys() )
		{
			out.println( key + "=" + session.get( key ) );
		}
		return null;
	}
}
