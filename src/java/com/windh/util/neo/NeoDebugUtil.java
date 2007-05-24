package com.windh.util.neo;

import java.io.PrintStream;
import java.util.TreeSet;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;
import org.neo4j.impl.core.NodeManager;
import org.neo4j.impl.core.NotFoundException;

public abstract class NeoDebugUtil
{
	public static void printNodeInfo( int nodeId, PrintStream writer )
	{
		Transaction tx = Transaction.begin();
		try
		{
			writer.println( "--- Printing Node info for " + nodeId + " ---" );
			Node node = NodeManager.getManager().getNodeById( nodeId );
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
	
	public static void printRelationshipInfo( Relationship rel,
		PrintStream writer )
	{
		Transaction tx = Transaction.begin();
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
		TreeSet set = new TreeSet();
		for ( String key : keys )
		{
			set.add( key );
		}
		return ( String[] ) set.toArray( new String[ set.size() ] );
	}
}
