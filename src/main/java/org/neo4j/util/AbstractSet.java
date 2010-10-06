/**
 * Copyright (c) 2002-2010 "Neo Technology,"
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

import java.util.Collection;

/**
 * Abstract super class for implementations of Neo4j collections and sets.
 * @author mattias
 *
 * @param <T> The type of objects in this collection.
 */
public abstract class AbstractSet<T> implements Collection<T>
{
	public boolean addAll( Collection<? extends T> items )
	{
		boolean changed = false;
		for ( T item : items )
		{
			if ( add( item ) )
			{
				changed = true;
			}
		}
		return changed;
	}

	public boolean containsAll( Collection<?> items )
	{
		boolean ok = true;
		for ( Object item : items )
		{
			if ( !contains( item ) )
			{
				ok = false;
				break;
			}
		}
		return ok;
	}

	public boolean removeAll( Collection<?> items )
	{
		boolean changed = false;
		for ( Object item : items )
		{
			if ( remove( item ) )
			{
				changed = true;
			}
		}
		return changed;
	}
}
