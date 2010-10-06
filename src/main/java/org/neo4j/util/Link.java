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

/**
 * This interface is used when dealing with a one-to-one relationship
 * between two objects (nodes). 
 * @param <T> the type of objects on both sides of this link.
 */
public interface Link<T>
{
	/**
	 * @return the object on the other side of this link. If there's no
	 * object connected null will be returned.
	 */
	T get();
	
	/**
	 * Sets {@code entity} to be the object on the other side of this link.
	 * If there's already a link to another object it will be removed first.
	 * @param object the object on the other side of this link.
	 * @return the previously set object or null if there was no previous
	 * object set.
	 */
	T set( T object );
	
	/**
	 * Removes the link if one is set.
	 * @return the removed object or null if no object was set.
	 */
	T remove();
	
	/**
	 * @return {@code true} if there's a link to another object.
	 */
	boolean has();
}
