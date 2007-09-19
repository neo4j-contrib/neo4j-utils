package org.neo4j.util;

public interface EventBufferFilter
{
	public EventContext[] filter( EventContext[] events );
	
	public static final EventBufferFilter HOLLOW_EVENT_FILTER =
		new EventBufferFilter()
	{
		public EventContext[] filter( EventContext[] events )
		{
			return events;
		}
	};
}
