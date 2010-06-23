package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.neo4j.commons.Predicate;
import org.neo4j.commons.iterator.FilteringIterator;
import org.neo4j.commons.iterator.IteratorWrapper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * A {@link Set} implemented on top of Neo4j primitives.
 * @author mattias
 *
 * @param <T> the type of objects in the set.
 */
public abstract class RelationshipSet<T> extends AbstractSet<T>
	implements Set<T>
{
	private final Node node;
	private final RelationshipType type;
	private final Direction direction;
	
	/**
	 * @param node the {@link Node} to act as the collection.
	 * @param type the relationship type to use internally for each object.
	 */
	public RelationshipSet( Node node, RelationshipType type )
	{
		this( node, type, Direction.OUTGOING );
	}
	
	/**
	 * @param node the {@link Node} to act as the collection.
	 * @param direction the direction to use for the relationships.
	 * @param type the relationship type to use internally for each object.
	 */
	public RelationshipSet( Node node, 
	    RelationshipType type, Direction direction )
	{
		if ( direction == null || direction == Direction.BOTH )
		{
			throw new IllegalArgumentException(
				"Only OUTGOING and INCOMING direction is allowed, since " +
				"this is a read/write collection" );
		}
		
		this.node = node;
		this.type = type;
		this.direction = direction;
	}
	
	protected Node getUnderlyingNode()
	{
		return this.node;
	}
	
	protected RelationshipType getRelationshipType()
	{
		return this.type;
	}
	
	protected Direction getDirection()
	{
		return direction;
	}
	
	protected Direction getInverseDirection()
	{
		return getDirection().reverse();
	}
	
	protected boolean directionIsOut()
	{
		return getDirection() == Direction.OUTGOING;
	}
	
	public boolean add( T item )
	{
		if ( contains( item ) )
		{
			return false;
		}
		
		Node otherNode = getNodeFromItem( item );
		Node startNode = directionIsOut() ? node : otherNode;
		Node endNode = directionIsOut() ? otherNode : node;
		Relationship relationship =
			startNode.createRelationshipTo( endNode, type );
		itemAdded( item, relationship );
		return true;
	}
	
	protected void itemAdded( T item, Relationship relationship )
	{
	}
	
	protected abstract Node getNodeFromItem( Object item );

	public void clear()
	{
		Iterator<Relationship> itr = getAllRelationships();
		while ( itr.hasNext() )
		{
			removeItem( itr.next() );
		}
	}

	public boolean contains( Object item )
	{
		return findRelationship( item ) != null;
	}
	
	protected boolean shouldIncludeRelationship( Relationship rel )
	{
		return true;
	}
	
	protected Iterator<Relationship> getAllRelationships()
	{
		return new FilteringIterator<Relationship>(
			node.getRelationships( type, getDirection() ).iterator(), new Predicate<Relationship>()
            {
			    public boolean accept(Relationship item)
			    {
			        return shouldIncludeRelationship( item );
			    }
            } );
	}
	
	protected Relationship findRelationship( Object item )
	{
		Node otherNode = getNodeFromItem( item );
		Relationship result = null;
		for ( Relationship rel : otherNode.getRelationships(
			type, getInverseDirection() ) )
		{
		    if ( !shouldIncludeRelationship( rel ) )
		    {
		        continue;
		    }
		    
			if ( rel.getOtherNode( otherNode ).equals( node ) )
			{
				result = rel;
				break;
			}
		}
		return result;
	}

	public boolean isEmpty()
	{
	    return !getAllRelationships().hasNext();
	}

	public Iterator<T> iterator()
	{
		Iterator<T> result = new IteratorWrapper<T, Relationship>(
		    getAllRelationships() )
	    {
            @Override
            protected T underlyingObjectToObject( Relationship rel )
            {
                return newObject( rel.getOtherNode(
                    RelationshipSet.this.node ), rel );
            }
	    };
		return result;
	}
	
	protected void removeItem( Relationship rel )
	{
		rel.delete();
	}

	public boolean remove( Object item )
	{
		Relationship rel = findRelationship( item );
		boolean changed = false;
		if ( rel != null )
		{
			removeItem( rel );
			changed = true;
		}
		return changed;
	}

	public boolean retainAll( Collection<?> items )
	{
	    Collection<Relationship> relationships =
	        new HashSet<Relationship>();
	    Iterator<Relationship> allRelationships = getAllRelationships();
	    while ( allRelationships.hasNext() )
	    {
	        relationships.add( allRelationships.next() );
	    }
	    
		for ( Object item : items )
		{
			Relationship rel = findRelationship( item );
			if ( rel != null )
			{
			    relationships.remove( rel );
			}
		}
	    
		Collection<T> itemsToRemove = new HashSet<T>();
		for ( Relationship rel : relationships )
		{
		    itemsToRemove.add( newObject( getOtherNode( rel ), rel ) );
		}
		
		boolean result = this.removeAll( itemsToRemove );
		return result;
	}

	public int size()
	{
		int counter = 0;
		Iterator<Relationship> itr = getAllRelationships();
		while ( itr.hasNext() )
		{
			itr.next();
			counter++;
		}
		return counter;
	}
	
	protected abstract T newObject( Node node, Relationship relationship );
	
	protected Node getOtherNode( Relationship relationship )
	{
		return relationship.getOtherNode( this.getUnderlyingNode() );
	}

	public Object[] toArray()
	{
		Iterator<Relationship> itr = getAllRelationships();
		Collection<Object> result = newArrayCollection();
		while ( itr.hasNext() )
		{
			Relationship rel = itr.next();
			result.add( newObject( getOtherNode( rel ), rel ) );
		}
		return result.toArray();
	}

	public <R> R[] toArray( R[] array )
	{
		Iterator<Relationship> itr = getAllRelationships();
		Collection<R> result = newArrayCollection();
		while ( itr.hasNext() )
		{
			Relationship rel = itr.next();
			result.add( ( R ) newObject( getOtherNode( rel ), rel ) );
		}
		
		int i = 0;
		for ( R item : result )
		{
			array[ i++ ] = item;
		}
		return array;
	}
	
	protected <R> Collection<R> newArrayCollection()
	{
		return new ArrayList<R>();
	}
}