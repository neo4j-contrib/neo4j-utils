package org.neo4j.util;

import java.util.Iterator;

public class CombiningIterator<T> extends PrefetchingIterator<T>
{
    private Iterator<Iterator<T>> iterators;
    private Iterator<T> currentIterator;
    
    public CombiningIterator( Iterable<Iterator<T>> iterators )
    {
        this.iterators = iterators.iterator();
    }

    @Override
    protected T fetchNextOrNull()
    {
        if ( currentIterator == null || !currentIterator.hasNext() )
        {
            while ( iterators.hasNext() )
            {
                currentIterator = iterators.next();
                if ( currentIterator.hasNext() )
                {
                    break;
                }
            }
        }
        return currentIterator != null && currentIterator.hasNext() ?
            currentIterator.next() : null;
    }
}
