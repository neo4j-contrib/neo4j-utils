package com.windh.util.neo.neoshell.apps;

import java.rmi.RemoteException;
import com.windh.util.neo.neoshell.NeoApp;
import com.windh.util.shell.App;
import com.windh.util.shell.CommandParser;
import com.windh.util.shell.Output;
import com.windh.util.shell.Session;
import com.windh.util.shell.ShellException;

public class Gsh extends NeoApp
{
	private App sh;
	
	public Gsh()
	{
		this.sh = new com.windh.util.shell.apps.extra.Gsh();
	}
	
	@Override
	public String getDescription()
	{
		return this.sh.getDescription();
	}

	@Override
	public String getDescription( String option )
	{
		return this.sh.getDescription( option );
	}

	@Override
	protected String exec( CommandParser parser, Session session, Output out )
		throws ShellException, RemoteException
	{
		return sh.execute( parser, session, out );
	}
}
