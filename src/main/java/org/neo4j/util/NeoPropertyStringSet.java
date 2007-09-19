package org.neo4j.util;

import org.neo4j.api.core.Node;

public class NeoPropertyStringSet extends NeoPropertySet<String>
{
	public NeoPropertyStringSet( Node node, String propertyKey,
		String delimiter )
	{
		super( node, propertyKey, delimiter );
	}

	public NeoPropertyStringSet( Node node, String propertyKey )
	{
		super( node, propertyKey );
	}

	@Override
	protected String itemToString( Object item )
	{
		return item.toString();
	}

	@Override
	protected String stringToItem( String string )
	{
		return string;
	}
}
