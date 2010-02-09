package org.neo4j.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.event.Event;
import org.neo4j.kernel.impl.event.ProActiveEventListener;

public abstract class SimpleProActiveEventListener
	implements ProActiveEventListener
{
	private GraphDatabaseUtil graphDBUtil;
	
	protected abstract Event[] getEventsToListenFor();
	
	public SimpleProActiveEventListener( GraphDatabaseService graphDb )
	{
		this.graphDBUtil = new GraphDatabaseUtil( graphDb );
		for ( Event event : getEventsToListenFor() )
		{
			graphDBUtil.registerProActiveEventListener( this, event );
		}
	}
	
	public void unregister()
	{
		for ( Event event : getEventsToListenFor() )
		{
			graphDBUtil.unregisterProActiveEventListener( this, event );
		}
	}
}
