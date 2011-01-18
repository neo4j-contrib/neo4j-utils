/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IterableWrapper;
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
