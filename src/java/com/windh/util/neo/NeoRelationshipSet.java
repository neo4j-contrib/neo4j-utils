package com.windh.util.neo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Transaction;
import org.neo4j.api.core.TraversalPosition;
import org.neo4j.api.core.Traverser.Order;

public abstract class NeoRelationshipSet<T> extends AbstractNeoSet<T>
{
	private Node node;
	private RelationshipType type;
	private Direction direction;
	
	public NeoRelationshipSet( Node node, RelationshipType type )
	{
		this( node, Direction.OUTGOING, type );
	}
	
	public NeoRelationshipSet( Node node, Direction direction,
		RelationshipType type )
	{
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
		return directionIsOut() ? Direction.INCOMING : Direction.OUTGOING;
	}
	
	protected boolean directionIsOut()
	{
		return getDirection() == Direction.OUTGOING;
	}
	
	public boolean add( T item )
	{
		Transaction tx = Transaction.begin();
		try
		{
			if ( contains( item ) )
			{
				return false;
			}
			
			Node otherNode = getNodeFromItem( item );
			Node startNode = directionIsOut() ? node : otherNode;
			Node endNode = directionIsOut() ? otherNode : node;
			startNode.createRelationshipTo( endNode, type );
			itemAdded( item );
			tx.success();
			return true;
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected void itemAdded( T item )
	{
	}
	
	protected abstract Node getNodeFromItem( Object item );

	public void clear()
	{
		Transaction tx = Transaction.begin();
		try
		{
			for ( Relationship rel : getAllRelationships() )
			{
				removeItem( rel );
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
		Transaction tx = Transaction.begin();
		try
		{
			boolean result = findRelationship( item ) != null;
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected boolean shouldIncludeRelationship( Relationship rel )
	{
		return true;
	}
	
	protected Relationship[] getAllRelationships()
	{
		ArrayList<Relationship> list = new ArrayList<Relationship>();
		for ( Relationship rel : node.getRelationships( type, getDirection() ) )
		{
			if ( shouldIncludeRelationship( rel ) )
			{
				list.add( rel );
			}
		}
		return list.toArray( new Relationship[ list.size() ] );
	}
	
	protected Relationship findRelationship( Object item )
	{
		Node otherNode = getNodeFromItem( item );
		Relationship result = null;
		for ( Relationship rel : otherNode.getRelationships(
			type, getInverseDirection() ) )
		{
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
		Transaction tx = Transaction.begin();
		try
		{
			return getAllRelationships().length == 0;
		}
		finally
		{
			tx.finish();
		}
	}

	public Iterator<T> iterator()
	{
		Transaction tx = Transaction.begin();
		try
		{
			Iterator<T> result = new ItemIterator();
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected void removeItem( Relationship rel )
	{
		rel.delete();
	}

	public boolean remove( Object item )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship rel = findRelationship( item );
			boolean changed = false;
			if ( rel != null )
			{
				removeItem( rel );
				changed = true;
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean retainAll( Collection items )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Collection itemsToRemove = new HashSet();
			for ( Object item : items )
			{
				Relationship rel = findRelationship( item );
				if ( rel != null )
				{
					itemsToRemove.add( newObject( getOtherNode( rel ), rel ) );
				}
			}
			
			boolean result = this.removeAll( itemsToRemove );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	public int size()
	{
		Transaction tx = Transaction.begin();
		try
		{
			int result = getAllRelationships().length;
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected abstract T newObject( Node node, Relationship relationship );
	
	protected Node getOtherNode( Relationship relationship )
	{
		return relationship.getOtherNode( this.getUnderlyingNode() );
	}

	public Object[] toArray()
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship[] rels = getAllRelationships();
			Object[] array = new Object[ rels.length ];
			for ( int i = 0; i < rels.length; i++ )
			{
				array[ i ] = newObject( getOtherNode( rels[ i ] ), rels[ i ] );
			}
			tx.success();
			return array;
		}
		finally
		{
			tx.finish();
		}
	}

	public <R> R[] toArray( R[] array )
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship[] rels = getAllRelationships();
			for ( int i = 0;i < rels.length; i++ )
			{
				array[ i ] = ( R )
					newObject( getOtherNode( rels[ i ] ), rels[ i ] );
			}
			tx.success();
			return array;
		}
		finally
		{
			tx.finish();
		}
	}
	
	protected class ItemIterator implements Iterator<T>
	{
		private Iterator<Node> traverser;
		private Relationship lastRelationship;
		private Relationship nextRelationship;
		private Node nextNode;
		
		ItemIterator()
		{
			StopEvaluator stopEvaluator = new DepthLimitStopEvaluator( 1 )
			{
				@Override
				public boolean isStopNode( TraversalPosition position )
				{
					lastRelationship = position.lastRelationshipTraversed();
					return super.isStopNode( position );
				}
			};
			
			traverser = node.traverse( Order.BREADTH_FIRST, stopEvaluator,
				ReturnableEvaluator.ALL_BUT_START_NODE, type,
				getDirection() ).iterator();
		}
		
		public boolean hasNext()
		{
			if ( this.nextNode != null )
			{
				return true;
			}
			
			Transaction tx = Transaction.begin();
			try
			{
				// Find the next rel
				while ( traverser.hasNext() )
				{
					Node node = traverser.next();
					if ( shouldIncludeRelationship( this.lastRelationship ) )
					{
						this.nextNode = node;
						this.nextRelationship = this.lastRelationship;
						break;
					}
				}
				return this.nextNode != null;
			}
			finally
			{
				tx.finish();
			}
		}

		public T next()
		{
			Transaction tx = Transaction.begin();
			try
			{
				if ( !this.hasNext() )
				{
					throw new NoSuchElementException();
				}
				T result = newObject( this.nextNode, this.nextRelationship );
				this.nextNode = null;
				this.nextRelationship = null;
				tx.success();
				return result;
			}
			finally
			{
				tx.finish();
			}
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}