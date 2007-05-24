package com.windh.util.neo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.windh.util.neo.CrudEventData.AlterationMode;

public class CrudEventBufferFilter implements EventBufferFilter
{
	public EventContext[] filter( EventContext[] events )
	{
		Set<Long> deleted = new HashSet<Long>();
		for ( EventContext context : events )
		{
			CrudEventData data = ( CrudEventData )
				context.getData().getData();
			if ( data.getAlterationMode() == AlterationMode.DELETED )
			{
				deleted.add( data.getNodeId() );
			}
		}
		
		Set<Long> taken = new HashSet<Long>();
		List<EventContext> list = new ArrayList<EventContext>();
		for ( EventContext context : events )
		{
			CrudEventData data = ( CrudEventData )
				context.getData().getData();
			long nodeId = data.getNodeId();
			if ( data.getAlterationMode() == AlterationMode.DELETED )
			{
				list.add( context );
			}
			else if ( !taken.contains( nodeId ) &&
				!deleted.contains( nodeId ) )
			{
				list.add( context );
				taken.add( nodeId );
			}
		}
		return list.toArray( new EventContext[ list.size() ] );
	}
}
