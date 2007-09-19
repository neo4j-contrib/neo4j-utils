package org.neo4j.util;

public abstract class CrudEventData
{
	private AlterationMode mode;
	
	public CrudEventData( AlterationMode mode )
	{
		this.mode = mode;
	}
	
	public AlterationMode getAlterationMode()
	{
		return mode;
	}
	
	public abstract long getNodeId();
	
	public static enum AlterationMode
	{
		CREATED,
		MODIFIED,
		DELETED,
	}
}
