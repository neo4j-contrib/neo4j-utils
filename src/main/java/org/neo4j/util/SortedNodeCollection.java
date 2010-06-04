package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import org.neo4j.commons.iterator.IterableWrapper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.index.impl.btree.BTree.RelTypes;
import org.neo4j.index.impl.sortedtree.SortedTree;

public class SortedNodeCollection<T extends NodeWrapper>
	extends AbstractSet<T>
{
	private Node rootNode;
	private Class<T> instanceClass;
	private Comparator<T> comparator;
	private SortedTree index;
	
	public SortedNodeCollection( Node rootNode,
		Comparator<T> comparator, Class<T> instanceClass )
	{
		this.rootNode = rootNode;
		this.instanceClass = instanceClass;
		this.comparator = comparator;
		instantiateIndex();
	}
	
	private Node ensureTheresARoot()
	{
		Node result = null;
		Relationship relationship = rootNode.getSingleRelationship(
			RelTypes.TREE_ROOT, Direction.OUTGOING );
		if ( relationship != null )
		{
			result = relationship.getOtherNode( rootNode );
		}
		else
		{
			result = rootNode.getGraphDatabase().createNode();
			rootNode.createRelationshipTo( result, RelTypes.TREE_ROOT );
		}
		return result;
	}
	
	private void instantiateIndex()
	{
		Node treeRootNode = ensureTheresARoot();
		this.index = new SortedTree( rootNode.getGraphDatabase(), treeRootNode,
			new ComparatorWrapper( this.comparator ) );
	}
	
	protected Node rootNode()
	{
		return this.rootNode;
	}
	
	protected SortedTree index()
	{
		return this.index;
	}
	
	protected T instantiateItem( Node itemNode )
	{
		return NodeWrapperImpl.newInstance( instanceClass, itemNode );
	}
	
	public boolean add( T item )
	{
		return index().addNode( item.getUnderlyingNode() );
	}

	public void clear()
	{
		index().delete();
		instantiateIndex();
	}

	public boolean contains( Object item )
	{
		T nodeItem = ( T ) item;
		return index().containsNode( nodeItem.getUnderlyingNode() );
	}

	public boolean isEmpty()
	{
		return index().getSortedNodes().iterator().hasNext();
	}

	public Iterator<T> iterator()
	{
		return new IterableWrapper<T, Node>(
			index().getSortedNodes() )
		{
			@Override
			protected T underlyingObjectToObject( Node node )
			{
				return instantiateItem( node );
			}
		}.iterator();
	}

	public boolean remove( Object item )
	{
		T nodeItem = ( T ) item;
		return index().removeNode( nodeItem.getUnderlyingNode() );
	}

	public boolean retainAll( Collection<?> items )
	{
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	public int size()
	{
		return toArray().length;
	}
	
	private <R> Collection<R> toCollection()
	{
		Collection<R> result = new ArrayList<R>();
		for ( Node node : index().getSortedNodes() )
		{
			result.add( ( R ) instantiateItem( node ) );
		}
		return result;
	}

	public Object[] toArray()
	{
		return toCollection().toArray();
	}

	public <R> R[] toArray( R[] array )
	{
		return toCollection().toArray( array );
	}
	
	/**
	 * Since this collection creates a sub-root to the supplied collection
	 * root node, it will have to be explicitly deleted from outside when
	 * you don't want this collection to exist anymore.
	 */
	public void delete()
	{
		index().delete();
	}
	
	private class ComparatorWrapper implements Comparator<Node>
	{
		private Comparator<T> source;
		
		ComparatorWrapper( Comparator<T> source )
		{
			this.source = source;
		}

		public int compare( Node o1, Node o2 )
		{
			// This is slow, I guess
			return source.compare(
				NodeWrapperImpl.newInstance( instanceClass, o1 ),
				NodeWrapperImpl.newInstance( instanceClass, o2 ) );
		}
	}
}
