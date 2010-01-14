package org.neo4j.util;

import org.neo4j.kernel.impl.event.Event;
import org.neo4j.kernel.impl.event.EventData;

/**
 * A filter for filtering incoming events to a listener.
 * @author mattias
 *
 */
public interface EventFilter
{
	/**
	 * Decides if an event passes this filter or not.
	 * @param event the received event.
	 * @param data the received event data.
	 * @return {@code true} if the event passes this filter (i.e. is OK),
	 * otherwise {@code false}.
	 */
	public boolean pass( Event event, EventData data );

	/**
	 * A filter which lets all events through.
	 */
	public static final EventFilter HOLLOW_EVENT_FILTER = new EventFilter()
	{
		public boolean pass( Event event, EventData data )
		{
			return true;
		}
	};
}
