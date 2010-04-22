package org.neo4j.util;

public interface ObjectFilter<T>
{
    boolean pass( T object );
}
