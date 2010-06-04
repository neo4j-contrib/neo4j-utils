package org.neo4j.util;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

public class NodeLink extends AbstractLink<Node>
{
    public NodeLink( Node node, RelationshipType type )
    {
        super( node, type );
    }

    public NodeLink( Node node, RelationshipType type,
        Direction direction )
    {
        super( node, type, direction );
    }
    
    @Override
    protected Node getNodeFromItem( Node item )
    {
        return item;
    }

    @Override
    protected Node newObject( Node node )
    {
        return node;
    }
}
