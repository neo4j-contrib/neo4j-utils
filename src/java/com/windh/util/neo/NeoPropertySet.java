package com.windh.util.neo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;

public abstract class NeoPropertySet<T> extends AbstractNeoSet<T>
{
	public static final String DEFAULT_DELIMITER = "|";
	
	private Node node;
	private String key;
	private String delimiter;
	
	public NeoPropertySet( Node node, String propertyKey )
	{
		this( node, propertyKey, DEFAULT_DELIMITER );
	}
	
	public NeoPropertySet( Node node, String propertyKey,
		String delimiter )
	{
		this.node = node;
		this.key = propertyKey;
		this.delimiter = delimiter;
	}
	
	protected abstract String itemToString( Object item );
	
	protected abstract T stringToItem( String string );
	
	private Set<String> tokenize()
	{
		Transaction tx = Transaction.begin();
		try
		{
			Set<String> set = new HashSet<String>();
			if ( this.node.hasProperty( this.key ) )
			{
				String value = ( String ) NeoUtil.getInstance().
					getProperty( this.node, this.key );
				if ( value.length() > 0 )
				{
					for ( String token :
						value.split( Pattern.quote( this.delimiter ) ) )
					{
						set.add( token );
					}
				}
			}
			tx.success();
			return set;
		}
		finally
		{
			tx.finish();
		}
	}
	
	private String glue( Set<String> set )
	{
		StringBuffer buffer = new StringBuffer();
		for ( String token : set )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( this.delimiter );
			}
			buffer.append( token );
		}
		return buffer.toString();
	}
	
	private void store( String value, boolean changed )
	{
		if ( !changed )
		{
			return;
		}
		
		Transaction tx = Transaction.begin();
		try
		{
			NeoUtil.getInstance().setProperty( this.node, this.key, value );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public boolean add( T item )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Set<String> set = tokenize();
			boolean changed = set.add( itemToString( item ) );
			store( glue( set ), changed );
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public void clear()
	{
		Transaction tx = Transaction.begin();
		try
		{
			if ( this.node.hasProperty( this.key ) )
			{
				NeoUtil.getInstance().removeProperty( this.node, this.key );
			}
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean contains( Object item )
	{
		return this.tokenize().contains( itemToString( item ) );
	}

	public boolean isEmpty()
	{
		return this.tokenize().size() == 0;
	}

	public Iterator<T> iterator()
	{
		return new ItemIterator( this.tokenize().iterator() );
	}

	public boolean remove( Object item )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Set<String> set = tokenize();
			boolean changed = set.remove( itemToString( item ) );
			store( glue( set ), changed );
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean retainAll( Collection realItems )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Collection<String> items = new ArrayList<String>();
			for ( Object item : realItems )
			{
				items.add( itemToString( item ) );
			}
			
			Set<String> set = tokenize();
			boolean changed = set.retainAll( items );
			store( glue( set ), changed );
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public int size()
	{
		return this.tokenize().size();
	}

	public Object[] toArray()
	{
		return this.tokenize().toArray();
	}

	public <R> R[] toArray( R[] array )
	{
		Object[] source = this.tokenize().toArray();
		for ( int i = 0; i < source.length; i++ )
		{
			array[ i ] = ( R ) stringToItem( ( String ) source[ i ] );
		}
		return array;
	}

	private class ItemIterator implements Iterator<T>
	{
		private Iterator<String> iterator;
		
		ItemIterator( Iterator<String> iterator )
		{
			this.iterator = iterator;
		}
		
		public boolean hasNext()
		{
			return iterator.hasNext();
		}

		public T next()
		{
			return stringToItem( iterator.next() );
		}

		public void remove()
		{
			this.iterator.remove();
		}
	}
}
