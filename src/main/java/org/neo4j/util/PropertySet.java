package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Node;

/**
 * A collection implemented with one property where the values are separated
 * with a delimiter.
 * @author mattias
 *
 * @param <T> the type of objects in the collection.
 */
public abstract class PropertySet<T> extends AbstractSet<T>
	implements Set<T>
{
	/**
	 * The delimiter used for separating values.
	 */
	public static final String DEFAULT_DELIMITER = "|";
	
	private final Node node;
	private final String key;
	private final String delimiter;
	
	/**
	 * @param node the node to act as the collection.
	 * @param propertyKey the property key to use for the collection node to
	 * store the values.
	 */
	public PropertySet( Node node, String propertyKey )
	{
		this( node, propertyKey, DEFAULT_DELIMITER );
	}
	
	/**
	 * @param node the node to act as the collection.
	 * @param propertyKey the property key to use for the collection node to
	 * store the values.
	 * @param delimiter custom delimiter instead of {@link #DEFAULT_DELIMITER}.
	 */
	public PropertySet( Node node, String propertyKey,
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
		Set<String> set = new HashSet<String>();
		if ( this.node.hasProperty( this.key ) )
		{
			String value = ( String ) this.node.getProperty( this.key );
			if ( value.length() > 0 )
			{
				for ( String token :
					value.split( Pattern.quote( this.delimiter ) ) )
				{
					set.add( token );
				}
			}
		}
		return set;
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
		
		this.node.setProperty( this.key, value );
	}
	
	public boolean add( T item )
	{
		Set<String> set = tokenize();
		boolean changed = set.add( itemToString( item ) );
		store( glue( set ), changed );
		return changed;
	}

	public void clear()
	{
		if ( this.node.hasProperty( this.key ) )
		{
			this.node.removeProperty( this.key );
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
		Set<String> set = tokenize();
		boolean changed = set.remove( itemToString( item ) );
		store( glue( set ), changed );
		return changed;
	}

	public boolean retainAll( Collection<?> realItems )
	{
		Collection<String> items = new ArrayList<String>();
		for ( Object item : realItems )
		{
			items.add( itemToString( item ) );
		}
		
		Set<String> set = tokenize();
		boolean changed = set.retainAll( items );
		store( glue( set ), changed );
		return changed;
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
