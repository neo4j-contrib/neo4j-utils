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

package examples;

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.util.GraphDatabaseLifecycle;
import org.neo4j.util.GraphDatabaseUtil;
import org.neo4j.util.Migrator;
import org.neo4j.util.NodeWrapperImpl;
import org.neo4j.util.NodeWrapperRelationshipSet;
import org.neo4j.util.SimpleMigration;

public class SiteExamples
{
    private static EmbeddedGraphDatabase graphDb;

    @BeforeClass
    public static void setUpDb()
    {
        graphDb = new EmbeddedGraphDatabase( "target/var/examples" );
    }

    private Transaction tx;
    
    @Before
    public void doBefore()
    {
        tx = graphDb.beginTx();
    }
    
    @After
    public void doAfter()
    {
        tx.success();
        tx.finish();
    }
    
    @AfterClass
    public static void shutdownDb()
    {
        graphDb.shutdown();
    }
    
    public void graphDbLifecycleUsage()
    {
        // START SNIPPET: graphDbLifecycleUsage
        GraphDatabaseLifecycle graphDb = new GraphDatabaseLifecycle(
                new EmbeddedGraphDatabase( "path/to/db" ) );
        graphDb.addLuceneIndexService();
        // END SNIPPET: graphDbLifecycleUsage
        
        graphDb.manualShutdown();
    }
    
    public void graphDbLifecycleReplaces()
    {
        // START SNIPPET: graphDbLifecycleReplaces
        final GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "path/to/db" );
        final IndexService indexService = new LuceneIndexService( graphDb );
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                indexService.shutdown();
                graphDb.shutdown();
            }
        } );
        // END SNIPPET: graphDbLifecycleReplaces
    }
    
    @Test
    public void handleArrayValues()
    {
        Node node = graphDb.createNode();
        
        // START SNIPPET: handleArrayValues
        node.setProperty( "numbers", 5 );
        List<Object> oneNumber = GraphDatabaseUtil.getPropertyValues( node, "numbers" );
        // Will contain one item (5).

        node.setProperty( "numbers", new int[] { 5, 10, 15 } );
        List<Object> numbers = GraphDatabaseUtil.getPropertyValues( node, "numbers" );
        // Will contain three items (5, 10, 15).
        
        // There's also methods for adding/removing values to/from a property.
        GraphDatabaseUtil.removeValueFromArray( node, "numbers", 10 );
        GraphDatabaseUtil.addValueToArray( node, "numbers", 20 );
        List<Object> newNumbers = GraphDatabaseUtil.getPropertyValues( node, "numbers" );
        // Will contain three items (5, 15, 20).
        // END SNIPPET: handleArrayValues
    }
    
    private static enum ExampleTypes implements RelationshipType
    {
        USERS,
        MY_TYPE,
    }
    
     // START SNIPPET: nodeWrapperSetUsage
     public void nodeWrapperSetUsage()
     {
         Node baseNode = graphDb.createNode();
         Node itemNode1 = graphDb.createNode();
         Node itemNode2 = graphDb.createNode();
         
         Collection<MyDomainObject> items = new NodeWrapperRelationshipSet<MyDomainObject>(
                 baseNode, ExampleTypes.MY_TYPE, MyDomainObject.class );
         MyDomainObject item1 = new MyDomainObject( itemNode1 );
         MyDomainObject item2 = new MyDomainObject( itemNode2 );
         items.add( item1 );
         items.add( item2 );
         for ( MyDomainObject item : items )
         {
             // Do something with the item
         }
     }
     
     public class MyDomainObject extends NodeWrapperImpl
     {
        public MyDomainObject( Node node )
        {
            super( node );
        }
     }
     // END SNIPPET: nodeWrapperSetUsage
     
     // START SNIPPET: migration
     class MyMigration extends SimpleMigration
     {
         public MyMigration( GraphDatabaseService graphDb )
         {
             super( graphDb );
         }
     
         @Override
         protected int getCodeVersion()
         {
             // Default version is 0, so the first version difference is 1.
             return 1;
         }
     }
     
     // The migrator for version 1, notice that SimpleMigration class' default
     // lookup method for finding a migrator is to find a class in the same
     // package as itself named "Migrator" + <version>.
     class Migrator1 implements Migrator
     {
         public void performMigration( GraphDatabaseService graphDb )
         {
             // Go ahead, do your thing... transactions are managed by the
             // Migration instance.
         }
     }
     // END SNIPPET: migration
     
     public void syncVersions()
     {
         // START SNIPPET: syncVersions
         GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "path/to/db" );
         new MyMigration( graphDb ).syncVersion();
         // END SNIPPET: syncVersions
     }
}
