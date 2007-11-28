package org.neo4j.util;

import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;

/**
 * A small class for coupling an {@link Event} with an {@link EventData}.
 * @author mattias
 */
public class EventContext
{
	private Event event;
	private EventData data;
	
	/**
	 * @param event the event.
	 * @param data the event data.
	 */
	public EventContext( Event event, EventData data )
	{
		this.event = event;
		this.data = data;
	}
	
	/**
	 * @return the event instance from the constructor.
	 */
	public Event getEvent()
	{
		return this.event;
	}
	
	/**
	 * @return the event data instance from the constructor.
	 */
	public EventData getData()
	{
		return this.data;
	}
}
