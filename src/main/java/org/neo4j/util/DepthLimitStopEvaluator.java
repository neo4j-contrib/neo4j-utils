package org.neo4j.util;

import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;

/**
 * A {@link StopEvaluator} which stops after X levels.
 * @author mattias
 */
public class DepthLimitStopEvaluator implements StopEvaluator
{
	private int maxDepth;
	
	/**
	 * @param maxDepth the depth to go to before stopping.
	 */
	public DepthLimitStopEvaluator( int maxDepth )
	{
		this.maxDepth = maxDepth;
	}

	public boolean isStopNode( TraversalPosition position )
	{
		return position.depth() >= maxDepth;
	}
}
