package org.neo4j.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;

/**
 * This class uses the fact that node property values can be arrays.
 * It looks at one property on a node as if it was a collection of values.
 * 
 * @param <T> the type of values.
 */
public class NeoPropertyArraySet<T> extends AbstractNeoSet<T>
    implements List<T>
{
	private Node node;
	private String key;
	private NeoUtil neoUtil;
	
	public NeoPropertyArraySet( NeoService neo, Node node, String key )
	{
		this.neoUtil = new NeoUtil( neo );
		this.node = node;
		this.key = key;
	}
	
	protected NeoUtil neoUtil()
	{
		return this.neoUtil;
	}
	
	protected Node node()
	{
		return this.node;
	}
	
	protected String key()
	{
		return this.key;
	}
	
	public boolean add( T o )
	{
		return neoUtil().addValueToArray( node(), key(), o );
	}
	
	public void clear()
	{
		neoUtil().removeProperty( node(), key() );
	}
	
	private List<Object> values()
	{
	    return neoUtil().getPropertyValues( node(), key() );
	}
	
	private void setValues( Collection<?> collection )
	{
        neoUtil().setProperty( node(), key(),
            neoUtil().asNeoProperty( collection ) );
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
			neoUtil().getPropertyValues( node(), key() ) )
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
		return neoUtil().removeValueFromArray( node(), key(), o );
	}
	
	public boolean retainAll( Collection<?> c )
	{
		Transaction tx = neoUtil().neo().beginTx();
		try
		{
			Collection<Object> values = values();
			boolean altered = values.retainAll( c );
			if ( altered )
			{
				if ( values.isEmpty() )
				{
					node().removeProperty( key() );
				}
				else
				{
					node().setProperty( key(),
						neoUtil().asNeoProperty( values ) );
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
