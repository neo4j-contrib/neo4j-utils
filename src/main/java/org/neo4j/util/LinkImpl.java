package org.neo4j.util;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.transaction.IllegalResourceException;
import org.neo4j.impl.transaction.LockManager;

/**
 * The default implementation of {@link Link}.
 * @author mattias
 *
 * @param <T> the type of objects used in this instance.
 */
public class LinkImpl<T extends NodeWrapper> implements Link<T>
{
	private NeoService neo;
	private Node node;
	private RelationshipType type;
	private Class<? extends T> theClass;
	private Direction direction = Direction.OUTGOING;
	
	/**
	 * @param node the node to act as the link.
	 * @param type the relationship type to be the link relationship.
	 * @param thisIsGenericsFault well, even if we have T we must send the
	 * same class here to make instantiation work.
	 */
	public LinkImpl( NeoService neo, Node node, RelationshipType type,
		Class<? extends T> thisIsGenericsFault )
	{
		this.neo = neo;
		this.node = node;
		this.type = type;
		this.theClass = thisIsGenericsFault;
	}
	
	/**
	 * @param node the node to act as the link.
	 * @param type the relationship type to be the link relationship.
	 * @param thisIsGenericsFault well, even if we have T we must send the
	 * same class here to make instantiation work.
	 * @param direction the direction of the relationship.
	 */
	public LinkImpl( NeoService neo, Node node, RelationshipType type,
		Direction direction, Class<T> thisIsGenericsFault )
	{
		this( neo, node, type, thisIsGenericsFault );
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
	
	protected Class<? extends T> classType()
	{
		return this.theClass;
	}
	
	protected Direction direction()
	{
		return this.direction;
	}
	
	protected T newObject( Node node )
	{
		return NodeWrapperImpl.newInstance( this.classType(), neo, node );
	}
	
	public T get()
	{
		Transaction tx = neo.beginTx();
		try
		{
			Relationship rel = this.node().getSingleRelationship( this.type(),
				this.direction() );
			if ( rel == null )
			{
				throw new RuntimeException( "No link relationship found for " +
					this.node() + ":" + this.direction() + ":" + this.type() );
			}
			Node otherNode = rel.getOtherNode( this.node() );
			T result = this.newObject( otherNode );
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
		Transaction tx = neo.beginTx();
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
		Transaction tx = neo.beginTx();
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
		Transaction tx = neo.beginTx();
		LockManager lockManager =
			( ( EmbeddedNeo ) neo ).getConfig().getLockManager();
		try
		{
			lockManager.getWriteLock( this.node() );
			if ( has() )
			{
				remove();
			}
			
			Node entityNode = ( ( NodeWrapper ) entity ).getUnderlyingNode();
			Node startNode = this.direction() == Direction.OUTGOING ?
				this.node() : entityNode;
			Node endNode = this.direction() == Direction.OUTGOING ?
				entityNode : this.node();
			Relationship rel =
				startNode.createRelationshipTo( endNode, this.type() );
			entitySet( entity, rel );
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
				lockManager.releaseWriteLock( this.node() );
			}
			catch ( Exception e )
			{
				throw new RuntimeException( e );
			}
			tx.finish();
		}
	}
	
	protected void entitySet( T entity, Relationship createdRelationship )
	{
	}
}
