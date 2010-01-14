package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public abstract class NeoQueueWorker extends Thread
{
    private final GraphDatabaseService neo;
    private final NeoQueue queue;
    private boolean halted;
    private boolean requestedToPause;
    private boolean paused;
    private int batchSize;
    
    public NeoQueueWorker( GraphDatabaseService neo, NeoQueue queue, int batchSize,
        String name )
    {
        super( name );
        this.neo = neo;
        this.queue = queue;
        this.batchSize = batchSize;
    }
    
    public NeoQueueWorker( GraphDatabaseService neo, NeoQueue queue, int batchSize )
    {
        this( neo, queue, batchSize, "NeoQueueWorker" );
    }
    
    public NeoQueue getQueue()
    {
        return this.queue;
    }
    
    public void setPaused( boolean paused )
    {
        if ( this.paused == paused )
        {
            return;
        }
        
        if ( paused && this.requestedToPause )
        {
            waitUntilReallyPaused();
            return;
        }
        
        this.requestedToPause = paused;
        if ( paused )
        {
            waitUntilReallyPaused();
        }
        else
        {
            this.paused = false;
        }
    }
    
    private void waitUntilReallyPaused()
    {
        while ( !this.paused )
        {
            try
            {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e )
            { // OK
            }
        }
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
            if ( this.requestedToPause || this.paused )
            {
                this.paused = true;
                this.requestedToPause = false;
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

            beforeBatch();
            try
            {
                for ( Map<String, Object> entry : entries )
                {
                    doOne( entry );
                }
                
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
            }
            catch ( Exception e )
            {
                // We got an exception, just do nothing and the tx will roll
                // back so that we can try next time instead.
            }
            finally
            {
                afterBatch();
            }
            
            tx.success();
        }
        finally
        {
            tx.finish();
        }
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

    private void doOne( Map<String, Object> entry ) throws Exception
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
    
    protected void handleEntryError( Map<String, Object> entry,
        Exception exception ) throws Exception
    {
        // Add it to the end of the queue
        add( entry );
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
