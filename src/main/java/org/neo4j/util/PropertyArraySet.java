package org.neo4j.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.neo4j.commons.iterator.CollectionWrapper;

/**
 * This class uses the fact that node property values can be arrays.
 * It looks at one property on a node as if it was a collection of values.
 *
 * @param <T> the type of values.
 */
public class PropertyArraySet<T> extends AbstractSet<T>
    implements List<T>
{
	private PropertyContainer container;
	private String key;
	private GraphDatabaseUtil graphDBUtil;

	public PropertyArraySet( GraphDatabaseService graphDb, PropertyContainer container,
	    String key )
	{
		super( graphDb );
		this.graphDBUtil = new GraphDatabaseUtil( graphDb );
		this.container = container;
		this.key = key;
	}

	protected GraphDatabaseUtil graphDbUtil()
	{
		return this.graphDBUtil;
	}

	protected PropertyContainer container()
	{
		return this.container;
	}

	protected String key()
	{
		return this.key;
	}

	public boolean add( T o )
	{
		return graphDbUtil().addValueToArray( container(), key(), o );
	}

	public void clear()
	{
		graphDbUtil().removeProperty( container(), key() );
	}

	private List<Object> values()
	{
	    return graphDbUtil().getPropertyValues( container(), key() );
	}

	private void setValues( Collection<?> collection )
	{
		graphDbUtil().setProperty( container(), key(),
			graphDbUtil().asPropertyValue( collection ) );
	}

	public boolean contains( Object o )
	{
		return values().contains( o );
	}

	public boolean isEmpty()
	{
		return values().isEmpty();
	}

	public Iterator<T> iterator()
	{
		return new CollectionWrapper<T, Object>(
			graphDbUtil().getPropertyValues( container(), key() ) )
		{
			@Override
			protected Object objectToUnderlyingObject( T object )
			{
				return object;
			}

			@Override
			protected T underlyingObjectToObject( Object object )
			{
				return ( T ) object;
			}
		}.iterator();
	}

	public boolean remove( Object o )
	{
		return graphDbUtil().removeValueFromArray( container(), key(), o );
	}

	public boolean retainAll( Collection<?> c )
	{
		Transaction tx = graphDbUtil().graphDb().beginTx();
		try
		{
			Collection<Object> values = values();
			boolean altered = values.retainAll( c );
			if ( altered )
			{
				if ( values.isEmpty() )
				{
					container().removeProperty( key() );
				}
				else
				{
					container().setProperty( key(),
						graphDbUtil().asPropertyValue( values ) );
				}
			}
			tx.success();
			return altered;
		}
		finally
		{
			tx.finish();
		}
	}

	public int size()
	{
		return values().size();
	}

	public Object[] toArray()
	{
		return values().toArray();
	}

	public <R> R[] toArray( R[] a )
	{
		return values().toArray( a );
	}

	public T set( int index, T value )
	{
		List<Object> values = values();
		T oldValue = ( T ) values.set( index, value );
		setValues( values );
		return oldValue;
	}

	public T remove( int index )
	{
		List<Object> values = values();
		T oldValue = ( T ) values.remove( index );
		setValues( values );
		return oldValue;
	}

	public int lastIndexOf( Object value )
	{
		return values().lastIndexOf( value );
	}

	public int indexOf( Object value )
	{
		return values().indexOf( value );
	}

	public boolean addAll( int index, Collection collection )
	{
		List<Object> values = values();
		boolean result = values.addAll( collection );
		if ( result )
		{
			setValues( values );
		}
		return result;
	}

	public void add( int index, T item )
	{
		List<Object> values = values();
		values.add( index, item );
		setValues( values );
	}

	public ListIterator<T> listIterator()
	{
		throw new UnsupportedOperationException();
	}

	public ListIterator<T> listIterator( int index )
	{
		throw new UnsupportedOperationException();
	}

	public List<T> subList( int start, int end )
	{
		throw new UnsupportedOperationException();
	}

	public T get( int index )
	{
		return ( T ) values().get( index );
	}
}
