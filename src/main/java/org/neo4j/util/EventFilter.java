package org.neo4j.util;

import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;

public interface EventFilter
{
	public boolean pass( Event event, EventData data );

	public static final EventFilter HOLLOW_EVENT_FILTER = new EventFilter()
	{
		public boolean pass( Event event, EventData data )
		{
			return true;
		}
	};
}
