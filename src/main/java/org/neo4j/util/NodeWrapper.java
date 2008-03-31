package org.neo4j.util;

import org.neo4j.api.core.Node;

/**
 * All objects which uses neo in its implementation will benefit from neo-utils
 * if implementing this interface. The instance can then be used in collection
 * a.s.o.
 */
public interface NodeWrapper
{
	Node getUnderlyingNode();
}
