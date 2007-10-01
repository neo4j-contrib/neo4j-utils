package org.neo4j.util;

/**
 * A filter for event arrays (used in {@link TransactionEventManager}.
 * @author mattias
 */
public interface EventBufferFilter
{
	/**
	 * Filters the events.
	 * @param events the events to filter.
	 * @return an array of events which passed the filter.
	 */
	public EventContext[] filter( EventContext[] events );
	
	/**
	 * A filter which lets all events through.
	 */
	public static final EventBufferFilter HOLLOW_EVENT_FILTER =
		new EventBufferFilter()
	{
		public EventContext[] filter( EventContext[] events )
		{
			return events;
		}
	};
}
