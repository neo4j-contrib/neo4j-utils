package org.neo4j.util;

import org.neo4j.api.core.Node;

/**
 * This interface allows for composite deletion patterns as well as hierarchial
 * delete functionality by implementations
 */
public interface NeoDeletor
{
	/**
	 * Deletes from the node space, starting at <code>startNode</code>
	 * @param startNode the node to start delete from
	 */
	void delete( Node startNode );
}
