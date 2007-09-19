package com.windh.util.neo;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import com.windh.util.neo.CrudEventData.AlterationMode;

public class CrudEventFilter implements EventFilter
{
	private Set<Long> modified = new HashSet<Long>();
	
	public boolean pass( Event event, EventData data )
	{
		CrudEventData d = ( CrudEventData ) data.getData();
		boolean result = this.modified.add( d.getNodeId() );
		return result || d.getAlterationMode() == AlterationMode.DELETED;
	}
}
