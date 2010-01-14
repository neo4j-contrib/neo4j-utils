package org.neo4j.util;

import java.util.ArrayList;
import java.util.List;
import org.neo4j.kernel.impl.event.EventData;
import org.neo4j.util.CrudEventBufferFilter;
import org.neo4j.util.CrudEventData;
import org.neo4j.util.CrudEventFilter;
import org.neo4j.util.EventContext;
import org.neo4j.util.CrudEventData.AlterationMode;

import test.NeoTest;

/**
 * Tests THE CRUD event filters.
 * @author mattias
 */
public class TestEventFilters extends NeoTest
{
	/**
	 * Tests the {@link CrudEventFilter} class.
	 */
	public void testCrudEventFilter()
	{
		CrudEventFilter filter = new CrudEventFilter();
		assertTrue( filter.pass( null, data( 0, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 0, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 0, AlterationMode.MODIFIED ) ) );
		assertTrue( filter.pass( null, data( 0, AlterationMode.DELETED ) ) );
		
		assertTrue( filter.pass( null, data( 1, AlterationMode.MODIFIED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.MODIFIED ) ) );
		assertTrue( filter.pass( null, data( 1, AlterationMode.DELETED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.MODIFIED ) ) );
		
		assertTrue( filter.pass( null, data( 2, AlterationMode.DELETED ) ) );
		assertTrue( !filter.pass( null, data( 2, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 2, AlterationMode.MODIFIED ) ) );
	}
	
	/**
	 * Tests the {@link CrudEventBufferFilter} class.
	 */
	public void testCrudEventBufferFilter()
	{
		List<EventContext> l = new ArrayList<EventContext>();
		l.add( new EventContext( null, data( 0, AlterationMode.CREATED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 1, AlterationMode.CREATED ) ) );
		l.add( new EventContext( null, data( 1, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 2, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 2, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 2, AlterationMode.MODIFIED ) ) );

		l.add( new EventContext( null, data( 0, AlterationMode.DELETED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		
		l.add( new EventContext( null, data( 3, AlterationMode.DELETED ) ) );
		l.add( new EventContext( null, data( 3, AlterationMode.MODIFIED ) ) );
		
		CrudEventBufferFilter filter = new CrudEventBufferFilter();
		EventContext[] a = filter.filter(
			l.toArray( new EventContext[ l.size() ] ) );
		
		assertEquals( 4, a.length );
		assertTrue( isEqual( a[ 0 ], data( 1, AlterationMode.CREATED ) ) );
		assertTrue( isEqual( a[ 1 ], data( 2, AlterationMode.MODIFIED ) ) );
		assertTrue( isEqual( a[ 2 ], data( 0, AlterationMode.DELETED ) ) );
		assertTrue( isEqual( a[ 3 ], data( 3, AlterationMode.DELETED ) ) );
	}
	
	private EventData data( long id, AlterationMode mode )
	{
		return new EventData( new Data( id, mode ) ); 
	}
	
	private boolean isEqual( EventContext context, EventData data )
	{
		CrudEventData d = ( CrudEventData ) data.getData();
		CrudEventData target = ( CrudEventData ) context.getData().getData();
		return target.getNodeId() == d.getNodeId() &&
			target.getAlterationMode() == d.getAlterationMode();
	}
	
	private static class Data extends CrudEventData
	{
		private long id;
		
		Data( long id, AlterationMode mode )
		{
			super( mode );
			this.id = id;
		}
		
		@Override
		public long getNodeId()
		{
			return this.id;
		}
	}
}
