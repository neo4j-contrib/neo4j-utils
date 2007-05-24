package com.windh.util.neo;

import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;

public class EventContext
{
	private Event event;
	private EventData data;
	
	public EventContext( Event event, EventData data )
	{
		this.event = event;
		this.data = data;
	}
	
	public Event getEvent()
	{
		return this.event;
	}
	
	public EventData getData()
	{
		return this.data;
	}
}
