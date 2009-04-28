package org.neo4j.util;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;

public class PureNodeLink extends AbstractLink<Node>
{
    public PureNodeLink( NeoService neo, Node node, RelationshipType type )
    {
        super( neo, node, type );
    }

    public PureNodeLink( NeoService neo, Node node, RelationshipType type,
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
