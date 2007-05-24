package com.windh.util.neo;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.transaction.IllegalResourceException;
import org.neo4j.impl.transaction.LockManager;

public class LinkImpl<T extends NodeWrapper> implements Link<T>
{
	private Node node;
	private RelationshipType type;
	private Class<T> theClass;
	private Direction direction = Direction.OUTGOING;
	
	public LinkImpl( Node node, RelationshipType type,
		Class<T> thisIsGenericsFault )
	{
		this.node = node;
		this.type = type;
		this.theClass = thisIsGenericsFault;
	}
	
	public LinkImpl( Node node, RelationshipType type,
		Class<T> thisIsGenericsFault, Direction direction )
	{
		this( node, type, thisIsGenericsFault );
		this.direction = direction;
	}
	
	protected Node node()
	{
		return this.node;
	}
	
	protected RelationshipType type()
	{
		return this.type;
	}
	
	protected Class<T> classType()
	{
		return this.theClass;
	}
	
	protected Direction direction()
	{
		return this.direction;
	}
	
	public T get()
	{
		Transaction tx = Transaction.begin();
		try
		{
			Relationship rel = this.node().getSingleRelationship( this.type(),
				this.direction() );
			if ( rel == null )
			{
				throw new RuntimeException( "No link relationship found for " +
					this.node() + ":" + this.direction() + ":" + this.type() );
			}
			Node node = rel.getOtherNode( this.node() );
			T result = NodeWrapper.newInstance( this.classType(), node );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean has()
	{
		Transaction tx = Transaction.begin();
		try
		{
			boolean result = this.node().getRelationships(
				this.type(), this.direction() ).iterator().hasNext();
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
		Transaction tx = Transaction.begin();
		try
		{
			this.node().getSingleRelationship(
				this.type(), this.direction() ).delete();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}

	public void set( T entity )
	{
		Transaction tx = Transaction.begin();
		try
		{
			LockManager.getManager().getWriteLock( this.node() );
			if ( has() )
			{
				remove();
			}
			
			Node entityNode = ( ( NodeWrapper ) entity ).getUnderlyingNode();
			Node startNode = this.direction() == Direction.OUTGOING ?
				this.node() : entityNode;
			Node endNode = this.direction() == Direction.OUTGOING ?
				entityNode : this.node();
			startNode.createRelationshipTo( endNode, this.type() );
			tx.success();
		}
		catch ( IllegalResourceException e )
		{
			throw new RuntimeException( e );
		}
		finally
		{
			try
			{
				LockManager.getManager().releaseWriteLock( this.node() );
			}
			catch ( Exception e )
			{
				throw new RuntimeException( e );
			}
			tx.finish();
		}
	}
}
