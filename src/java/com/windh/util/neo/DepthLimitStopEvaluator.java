package com.windh.util.neo;

import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.TraversalPosition;

public class DepthLimitStopEvaluator implements StopEvaluator
{
	private int maxDepth;
	
	public DepthLimitStopEvaluator( int maxDepth )
	{
		this.maxDepth = maxDepth;
	}

	public boolean isStopNode( TraversalPosition position )
	{
		return position.depth() >= maxDepth;
	}
}
