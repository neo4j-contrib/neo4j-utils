package org.neo4j.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * A {@link NeoPropertySet} where the objects are strings.
 */
public class NeoPropertyStringSet extends NeoPropertySet<String>
{
	/**
	 * @param node the collection node.
	 * @param propertyKey the property key for storing values.
	 * @param delimiter custom delimiter between values.
	 */
	public NeoPropertyStringSet( GraphDatabaseService neo, Node node, String propertyKey,
		String delimiter )
	{
		super( neo, node, propertyKey, delimiter );
	}

	/**
	 * @param node the collection node.
	 * @param propertyKey the property key for storing values.
	 */
	public NeoPropertyStringSet( GraphDatabaseService neo, Node node, String propertyKey )
	{
		super( neo, node, propertyKey );
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
