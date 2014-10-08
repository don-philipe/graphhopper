/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import java.util.HashSet;
import java.util.Set;

import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;

/**
 * Mainly copied from FootFlagEncoder.
 * 
 * Problems:
 *      - wheelchairs should not be routed over streets between towns
 *          on the other hand primary and secondary streets in towns without recorded sidewalks
 *          will be avoided
 * @author Philipp Thöricht
 */
public class WheelchairFlagEncoder extends AbstractFlagEncoder
{
    static final int SLOW = 2;
    static final int MEAN = 5;
    static final int FERRY = 10;
    private int safeWayBit = 0;
    protected HashSet<String> sidewalks = new HashSet<String>();
    private final Set<String> safeHighwayTags = new HashSet<String>();
    private final Set<String> allowedHighwayTags = new HashSet<String>();

    /**
     * Should be only instantied via EncodingManager
     */
    protected WheelchairFlagEncoder()
    {
        this(4, 1);
    }
    
    protected WheelchairFlagEncoder( int speedBits, double speedFactor )
    {
        super(speedBits, speedFactor);
        restrictions = new String[]
        {
            "wheelchair", "access"
        };
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");

        sidewalks.add("yes");
        sidewalks.add("both");
        sidewalks.add("left");
        sidewalks.add("right");

        potentialBarriers.add("gate");
        //potentialBarriers.add( "lift_gate" );
        potentialBarriers.add("swing_gate");

        acceptedRailways.add("station");
        acceptedRailways.add("platform");
        
        safeHighwayTags.add("footway");
//      safeHighwayTags.add("path");
        safeHighwayTags.add("pedestrian");
        safeHighwayTags.add("living_street");
//      safeHighwayTags.add("track");
        safeHighwayTags.add("residential");
        safeHighwayTags.add("service");
        
        allowedHighwayTags.addAll(safeHighwayTags);
//      allowedHighwayTags.add("trunk");
//      allowedHighwayTags.add("trunk_link");
//      allowedHighwayTags.add("primary");
//      allowedHighwayTags.add("primary_link");
        allowedHighwayTags.add("secondary");
        allowedHighwayTags.add("secondary_link");
        allowedHighwayTags.add("tertiary");
        allowedHighwayTags.add("tertiary_link");
        allowedHighwayTags.add("unclassified");
        allowedHighwayTags.add("road");
    }

    @Override
    public int defineWayBits( int index, int shift )
    {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        // larger value required - ferries are faster than wheelchairs
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, MEAN, FERRY);
        shift += speedBits;

        safeWayBit = 1 << shift++;
        return shift;
    }
    
    /**
     * Wheelchair flag encoder does not provide any turn cost / restrictions.
     */
    @Override
    public int defineTurnBits( int index, int shift, int numberCostsBits )
    {
        return shift;
    }
    
    /**
     * Wheelchair flag encoder does not provide any turn cost / restrictions
     * 
     * @return <code>false</code>
     */
    @Override
    public boolean isTurnRestricted( long flag )
    {
        return false;
    }
    
    /**
     * Wheelchair flag encoder does not provide any turn cost / restrictions
     * 
     * @return 0
     */    
    @Override
    public int getTurnCosts( long flag )
    {
        return 0;
    }
    
    @Override
    public String toString()
    {
        return "wheelchair";
    }

    /**
     * Some ways are okay but not separate for wheelchairs.
     * <p/>
     * @param way
     * @return 
     */
    @Override
    public long acceptWay( OSMWay way )
    {
        // TODO: steps may have ramps
        if(way.hasTag("highway", "steps"))
        {
            if(way.hasTag("ramp:wheelchair", intendedValues))
                return acceptBit;
            else
                return 0;
        }
        
        // allow wheelchairs on cycleways which can also be used by pedestrians
        if(way.hasTag("highway", "cycleway") && way.hasTag("foot", intendedValues))
            return acceptBit;
        
        String highwayValue = way.getTag("highway");
        if (highwayValue == null)
        {
            if (way.hasTag("route", ferries))
            {
                String wheelchairTag = way.getTag("wheelchair");
                String footTag = way.getTag("foot");
                if(!"no".equals(footTag) && wheelchairTag == null || "yes".equals(wheelchairTag))
                    return acceptBit | ferryBit;
            }
            return 0;
        }

//        String sacScale = way.getTag("sac_scale");
//        if (sacScale != null)
//        {
//            if (!"hiking".equals(sacScale) && !"mountain_hiking".equals(sacScale))
//                // other scales are too dangerous, see http://wiki.openstreetmap.org/wiki/Key:sac_scale
//                return 0;
//        }

        if (way.hasTag("sidewalk", sidewalks))
            return acceptBit;

        // no need to evaluate ferries or fords - already included here
        if (way.hasTag("wheelchair", intendedValues))
            return acceptBit;

        if (!allowedHighwayTags.contains(highwayValue))
            return 0;

        if (way.hasTag("motorroad", "yes"))
            return 0;

        // do not get our feet wet, "yes" is already included above
        if (way.hasTag("highway", "ford") || way.hasTag("ford"))
            return 0;

//        if (way.hasTag("bicycle", "official"))
//            return 0;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues))
            return 0;

        // do not accept railways (sometimes incorrectly mapped!)
        if (way.hasTag("railway") && !way.hasTag("railway", acceptedRailways))
            return 0;

        return acceptBit;
    }

        @Override
    public long handleRelationTags( OSMRelation relation, long oldRelationFlags )
    {
        return oldRelationFlags;
    }
    
    @Override
    public long handleWayTags(OSMWay way, long allowed, long relationCode)
    {
        if ((allowed & acceptBit) == 0)
            return 0;

        long encoded;
        if ((allowed & ferryBit) == 0)
        {
            String sacScale = way.getTag("sac_scale");
            if (sacScale != null)
            {
                if ("hiking".equals(sacScale))
                    encoded = speedEncoder.setDoubleValue(0, MEAN);
                else
                    encoded = speedEncoder.setDoubleValue(0, SLOW);
            } else
            {
                encoded = speedEncoder.setDoubleValue(0, MEAN);
            }
            encoded |= directionBitMask;

            // mark safe ways or ways with cycle lanes
            if (safeHighwayTags.contains(way.getTag("highway"))
                    || way.hasTag("sidewalk", sidewalks))
            {
                encoded |= safeWayBit;
            }

        } else
        {
            encoded = handleFerryTags(way, SLOW, MEAN, FERRY);
            encoded |= directionBitMask;
        }

        return encoded;
    }
    
//    @Override
//    public long handleNodeTags( OSMNode node )
//    {
//        // movable barriers block if they are not marked as passable
//        if (node.hasTag("barrier", potentialBarriers)
//                && !node.hasTag(restrictions, intendedValues)
//                && !node.hasTag("locked", "no"))
//        {
//            return directionBitMask;
//        }
//
//        if ((node.hasTag("highway", "ford") || node.hasTag("ford"))
//                && !node.hasTag(restrictions, intendedValues))
//        {
//            return directionBitMask;
//        }
//
//        return 0;
//    }
}