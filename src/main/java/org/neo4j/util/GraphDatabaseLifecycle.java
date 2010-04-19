package org.neo4j.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;

/**
 * Manages the life cycle of a {@link GraphDatabaseService} as well as other components.
 * Removes the tedious work of having to think about shutting down components
 * and the {@link GraphDatabaseService} when the JVM exists, in the right order as well.
 */
public class GraphDatabaseLifecycle
{
    /**
     * Field not final since it's nulled in the shutdown process (to be able
     * to support multiple calls to shutdown).
     */
    private GraphDatabaseService graphDb;
    private IndexService indexService;
    private Thread shutdownHook;
    
    /**
     * Constructs a new {@link GraphDatabaseLifecycle} instance with {@code graphDb}
     * as the {@link GraphDatabaseService}. Other components can be instantiated using
     * methods, f.ex. {@link #addIndexService(IndexService)}.
     * 
     * @param graphDb the {@link GraphDatabaseService} instance to manage.
     */
    public GraphDatabaseLifecycle( GraphDatabaseService graphDb )
    {
        this.graphDb = graphDb;
        this.shutdownHook = new Thread()
        {
            @Override
            public void run()
            {
                runJvmShutdownHook();
            }
        };
        Runtime.getRuntime().addShutdownHook( this.shutdownHook );
    }
    
    /**
     * Runs the shutdown process manually instead of waiting for it to happen
     * just before the JVM exists, see {@link #runJvmShutdownHook()}. Normally
     * this method isn't necessary to call, but can be good to have for special
     * cases.
     */
    public void manualShutdown()
    {
        runShutdown();
        if ( this.shutdownHook != null )
        {
            try
            {
                Runtime.getRuntime().removeShutdownHook( this.shutdownHook );
                this.shutdownHook = null;
            }
            catch ( IllegalStateException ise )
            {
                // already shutting down, so to late to remove hook
            }
        }
    }
    
    /**
     * Runs the shutdown process of all started services. Supports multiple
     * calls to it (if such would accidentally be done).
     */
    protected void runShutdown()
    {
        if ( this.indexService != null )
        {
            this.indexService.shutdown();
            this.indexService = null;
        }
        
        if ( this.graphDb != null )
        {
            this.graphDb.shutdown();
            this.graphDb = null;
        }
    }
    
    /**
     * Called right before the JVM exists. It's called from a thread registered
     * with {@link Runtime#addShutdownHook(Thread)}.
     */
    protected void runJvmShutdownHook()
    {
        runShutdown();
    }
    
    /**
     * Convenience method for adding a {@link LuceneIndexService} as the
     * {@link IndexService}. See {@link #addIndexService(IndexService)}.
     * 
     * @return the created {@link LuceneIndexService} instance.
     */
    public IndexService addLuceneIndexService()
    {
        assertIndexServiceNotInstantiated();
        return addIndexService( new LuceneIndexService( this.graphDb ) );
    }
    
    /**
     * Adds an {@link IndexService} to list of components to manage (which
     * means the shutdown of it will be managed automatically).
     * Currently only one {@link IndexService} instance is supported so if
     * you try to instantiate more than one instance a
     * {@link UnsupportedOperationException} will be thrown. There are
     * convenience methods for creating common index services,
     * {@link #addLuceneIndexService()}.
     * 
     * @param indexService the {@link IndexService} to add to the list if
     * managed components by this life cycle object.
     * @return the created {@link IndexService} instance.
     */
    public IndexService addIndexService( IndexService indexService )
    {
        assertIndexServiceNotInstantiated();
        this.indexService = indexService;
        return indexService;
    }
    
    /**
     * @return the {@link GraphDatabaseService} instance passed in to the constructor,
     */
    public GraphDatabaseService graphDb()
    {
        return this.graphDb;
    }
    
    /**
     * @return the {@link IndexService} instance managed by this life cycle
     * object or {@code null} if no {@link IndexService} has been instantiated.
     * See {@link #addIndexService(IndexService)}.
     */
    public IndexService indexService()
    {
        return this.indexService;
    }

    private void assertIndexServiceNotInstantiated()
    {
        if ( this.indexService != null )
        {
            throw new UnsupportedOperationException( "This utility class " +
                "only supports zero or one IndexService, there's already a " +
                this.indexService + " instantiated" );
        }
    }
}
