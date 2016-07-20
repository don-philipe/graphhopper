package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

/**
 *
 * @author Philipp Thöricht <philipp.thoericht@tu-dresden.de>
 */
public class BlockWayWeighting extends AbstractWeighting
{    
    private String blockedWay = "";
    public BlockWayWeighting( FlagEncoder encoder, PMap pMap )
    {
        super(encoder);
        blockedWay = pMap.get("block_way", "");
    }

    public BlockWayWeighting( FlagEncoder encoder )
    {
        this(encoder, new PMap(0));
    }

    @Override
    public double getMinWeight( double distance )
    {
        return 0;
    }

    @Override
    public double calcWeight( EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId )
    {
        if (flagEncoder instanceof BlindManFlagEncoder)
        {
            BlindManFlagEncoder blindFlagEncoder = (BlindManFlagEncoder) flagEncoder;
            if (blindFlagEncoder.getWayType(edge.getFlags()).equals(blockedWay))
                return Double.POSITIVE_INFINITY;
        }
        
        return 0;
    }

    @Override
    public String getName()
    {
        return "blockway";
    }
}
