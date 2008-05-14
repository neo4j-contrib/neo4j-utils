package org.neo4j.util;

import org.neo4j.api.core.NeoService;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.ProActiveEventListener;

public abstract class SimpleProActiveEventListener
	implements ProActiveEventListener
{
	private NeoUtil neoUtil;
	
	protected abstract Event[] getEventsToListenFor();
	
	public SimpleProActiveEventListener( NeoService neo )
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
