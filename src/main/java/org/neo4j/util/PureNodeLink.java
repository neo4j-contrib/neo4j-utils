package org.neo4j.util;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

public class PureNodeLink extends AbstractLink<Node>
{
    public PureNodeLink( GraphDatabaseService neo, Node node, RelationshipType type )
    {
        super( neo, node, type );
    }

    public PureNodeLink( GraphDatabaseService neo, Node node, RelationshipType type,
        Direction direction )
    {
        super( neo, node, type, direction );
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
