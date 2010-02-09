package org.neo4j.util;

import org.neo4j.graphdb.Node;

/**
 * All objects which uses Neo4j in its implementation will benefit from neo-utils
 * if implementing this interface. The instance can then be used in collection
 * a.s.o.
 */
public interface NodeWrapper
{
	Node getUnderlyingNode();
}
