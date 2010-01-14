package org.neo4j.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.event.Event;
import org.neo4j.kernel.impl.event.ProActiveEventListener;

public abstract class SimpleProActiveEventListener
	implements ProActiveEventListener
{
	private NeoUtil neoUtil;
	
	protected abstract Event[] getEventsToListenFor();
	
	public SimpleProActiveEventListener( GraphDatabaseService neo )
	{
		this.neoUtil = new NeoUtil( neo );
		for ( Event event : getEventsToListenFor() )
		{
			neoUtil.registerProActiveEventListener( this, event );
		}
	}
	
	public void unregister()
	{
		for ( Event event : getEventsToListenFor() )
		{
			neoUtil.unregisterProActiveEventListener( this, event );
		}
	}
}
