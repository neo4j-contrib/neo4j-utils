package com.windh.util.neo.xaworker;

import org.neo4j.impl.event.Event;

public class XaWorkerEvent extends Event
{
	XaWorkerEvent( String name )
	{
		super( name );
	}
	
	public static final XaWorkerEvent XA_WORKER_EVENT =
		new XaWorkerEvent( "XaWorkerEvent" );
}
