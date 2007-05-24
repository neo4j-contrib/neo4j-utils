package com.windh.util.neo.neoshell.apps;

import com.windh.util.neo.neoshell.NeoApp;
import com.windh.util.shell.CommandParser;
import com.windh.util.shell.Output;
import com.windh.util.shell.Session;
import com.windh.util.shell.ShellException;

public class Export extends NeoApp
{
	@Override
	public String getDescription()
	{
		return "Sets an environmentt variable. Usage: export <key>=<value>";
	}

	@Override
	protected String exec( CommandParser parser, Session session, Output out )
		throws ShellException
	{
		StringBuffer buffer = new StringBuffer();
		for ( String string : parser.arguments() )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( " " );
			}
			buffer.append( string );
		}
		
		String string = buffer.toString();
		int index = string.indexOf( '=' );
		if ( index == -1 )
		{
			throw new ShellException( "Invalid format <key>=<value>" );
		}
		
		String key = string.substring( 0, index );
		String value = string.substring( index + 1 );
		if ( value == null || value.trim().length() == 0 )
		{
			this.safeRemove( session, key );
		}
		else
		{
			this.safeSet( session, key, value );
		}
		return null;
	}
}
