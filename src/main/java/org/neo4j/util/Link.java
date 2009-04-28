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
