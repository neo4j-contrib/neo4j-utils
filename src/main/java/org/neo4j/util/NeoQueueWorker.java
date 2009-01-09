package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;

public abstract class NeoQueueWorker extends Thread
{
    private final NeoService neo;
    private final NeoQueue queue;
    private boolean halted;
    private boolean paused;
    private int batchSize;
    
    public NeoQueueWorker( NeoService neo, NeoQueue queue, int batchSize )
    {
        this.neo = neo;
        this.queue = queue;
        this.batchSize = batchSize;
    }
    
    public NeoQueue getQueue()
    {
        return this.queue;
    }
    
    public void setPaused()
    {
        this.paused = true;
    }
    
    public boolean isPaused()
    {
        return this.paused;
    }
    
    private void sleepQuiet( long millis )
    {
        try
        {
            Thread.sleep( millis );
        }
        catch ( InterruptedException e )
        {
            // Ok
        }
    }
    
    @Override
    public void run()
    {
        while ( !this.halted )
        {
            if ( this.isPaused() )
            {
                sleepQuiet( 1000 );
                continue;
            }
            
            if ( !executeOneBatch() )
            {
                sleepQuiet( 100 );
            }
        }
    }
    
    public void add( Map<String, Object> values )
    {
        Node entry = this.queue.add();
        for ( Map.Entry<String, Object> value : values.entrySet() )
        {
            entry.setProperty( value.getKey(), value.getValue() );
        }
    }
    
    protected void beforeBatch()
    {
    }
    
    protected void afterBatch()
    {
    }
    
    private boolean executeOneBatch()
    {
        int entrySize = 0;
        Collection<Map<String, Object>> entries = null;
        Transaction tx = neo.beginTx();
        try
        {
            Node[] nodes = this.queue.peek( batchSize );
            if ( nodes.length == 0 )
            {
                return false;
            }
            entrySize = nodes.length;
            
            entries = new ArrayList<Map<String,Object>>( entrySize );
            for ( Node node : nodes )
            {
                entries.add( readNode( node ) );
            }
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        
        beforeBatch();
        for ( Map<String, Object> entry : entries )
        {
            doOne( entry );
        }
        afterBatch();
        final int size = entrySize;
        new DeadlockCapsule<Object>( "remover" )
        {
            @Override
            public Object tryOnce()
            {
                queue.remove( size );
                return null;
            }
        }.run();
        return true;
    }
    
    private Map<String, Object> readNode( Node node )
    {
        Map<String, Object> result = new HashMap<String, Object>();
        for ( String key : node.getPropertyKeys() )
        {
            result.put( key, node.getProperty( key ) );
        }
        return result;
    }

    private void doOne( Map<String, Object> entry )
    {
        // Try a max of ten times if it fails.
        Exception exception = null;
        for ( int i = 0; !this.halted && i < 10; i++ )
        {
            try
            {
                doHandleEntry( entry );
                return;
            }
            catch ( Exception e )
            {
                exception = e;
            }
            sleepQuiet( 500 );
        }
        handleEntryError( entry, exception );
    }
    
    private void handleEntryError( Map<String, Object> entry,
        Exception exception )
    {
        // Add it to the end of the queue
        Transaction tx = neo.beginTx();
        try
        {
            add( entry );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    private void doHandleEntry( Map<String, Object> entry )
    {
        handleEntry( entry );
    }
    
    protected abstract void handleEntry( Map<String, Object> entry );
    
    public void startUp()
    {
        this.start();
    }

    public void shutDown()
    {
        this.halted = true;
        while ( this.isAlive() )
        {
            sleepQuiet( 200 );
        }
    }
}
