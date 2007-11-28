package org.neo4j.util;

/**
 * This interface is used when dealing with a one-to-one relationship
 * between two objects (nodes). 
 * @param <T> the type of objects on both sides of this link.
 */
public interface Link<T>
{
	/**
	 * @return the object on the other side of this link. A runtime exception
	 * will be thrown if this link doesn't point to another object.
	 */
	T get();
	
	/**
	 * Sets {@code entity} to be the object on the other side of this link.
	 * If there's already a link to another object it will be removed first.
	 * @param entity the object in the other side of this link.
	 */
	void set( T entity );
	
	/**
	 * Removes the link. A runtime exception will be thrown if this link
	 * doesn't point to another object.
	 */
	void remove();
	
	/**
	 * @return {@code true} if this link points to an object.
	 */
	boolean has();
}
