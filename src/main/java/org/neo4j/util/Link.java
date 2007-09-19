package org.neo4j.util;

/**
 * This interface is used when dealing with a one-to-one relationship
 * between two objects (nodes) 
 */
public interface Link<T>
{
	T get();
	
	void set( T entity );
	
	void remove();
	
	boolean has();
}
