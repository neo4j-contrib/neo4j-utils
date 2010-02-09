package org.neo4j.util;

import java.io.PrintStream;
import java.util.TreeSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

/**
 * Simple debugging utility for neo-related objects.
 */
public class GraphDbDebugUtil
{
	private GraphDatabaseService graphDb;
	
	public GraphDbDebugUtil( GraphDatabaseService graphDB )
	{
		this.graphDb = graphDB;
	}
	
	/**
	 * Prints information about a node, its properties and relationships.
	 * @param nodeId the node id to print.
	 * @param writer the writer to writer to.
	 */
	public void printNodeInfo( int nodeId, PrintStream writer )
	{
		Transaction tx = graphDb.beginTx();
		try
		{
			writer.println( "--- Printing Node info for " + nodeId + " ---" );
			Node node = graphDb.getNodeById( nodeId );
			writer.println( "Relationships: " );
			for ( Relationship rel : node.getRelationships() )
			{
				writer.print( rel.getType() + "(" + rel.getId() +
					")" );
				if ( rel.getStartNode().equals( node ) )
				{
					writer.print( " --> " );
				}
				else
				{
					writer.print( " <-- " );
				}
				writer.println( rel.getOtherNode( node ) );
			}
			String[] sortedKeys = sortIndexes( node.getPropertyKeys() );
			writer.println( "Properties:" );
			for ( int i = 0; i < sortedKeys.length; i++ )
			{
				writer.println( sortedKeys[ i ] + "=" + node.getProperty(
					sortedKeys[ i ] ) );
			}
			tx.success();
		}
		catch ( NotFoundException e )
		{
			writer.println( "Node id " + nodeId + " not found" );
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Prints information about a relationship, its properties and nodes.
	 * @param rel the relationship.
	 * @param writer the writer to write to.
	 */
	public void printRelationshipInfo( Relationship rel, PrintStream writer )
	{
		Transaction tx = graphDb.beginTx();
		try
		{
			writer.println( "--- Printing Relationship info for " +
				rel.getId() + " ---" );
			writer.println( "Start node: " + rel.getStartNode() );
			writer.println( "End node: " + rel.getEndNode() );
			String[] sortedKeys = sortIndexes( rel.getPropertyKeys() );
			writer.println( "Properties:" );
			for ( int i = 0; i < sortedKeys.length; i++ )
			{
				writer.println( sortedKeys[ i ] + "=" + rel.getProperty(
					sortedKeys[ i ] ) );
			}
			tx.success();
		}
		catch ( NotFoundException e )
		{
			throw new RuntimeException( e );
		}
		finally
		{
			tx.finish();
		}
	}
	
	private static String[] sortIndexes( Iterable<String> keys )
	{
		TreeSet<String> set = new TreeSet<String>();
		for ( String key : keys )
		{
			set.add( key );
		}
		return set.toArray( new String[ set.size() ] );
	}
}
