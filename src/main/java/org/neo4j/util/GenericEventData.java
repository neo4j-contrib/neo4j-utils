package org.neo4j.util;

import java.util.HashMap;
import java.util.Map;

public abstract class GenericEventData<T> extends CrudEventData
{
	private T object;
	private Map<String, Object> attributes;
	
	public GenericEventData( AlterationMode mode, T object )
	{
		super( mode );
		this.object = object;
	}
	
	public T getObject()
	{
		return object;
	}
	
	public void set( String key, Object value )
	{
		if ( attributes == null )
		{
			attributes = new HashMap<String, Object>();
		}
		attributes.put( key, value );
	}
	
	public Object get( String key )
	{
		return attributes == null ? null : attributes.get( key );
	}
}
