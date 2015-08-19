/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
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

import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.util.PMap;

import static com.graphhopper.routing.util.PriorityCode.*;

import java.util.*;

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
    static final int SLOW_SPEED = 2;
    static final int MEAN_SPEED = 5;
    static final int FERRY_SPEED = 10;
    private EncodedValue priorityWayEncoder;
    private EncodedValue relationCodeEncoder;
    protected HashSet<String> sidewalks = new HashSet<String>();
    private final Set<String> safeHighwayTags = new HashSet<String>();
    private final Set<String> allowedHighwayTags = new HashSet<String>();
    private final Set<String> avoidHighwayTags = new HashSet<String>();

    /**
     * Should be only instantied via EncodingManager
     */
    protected WheelchairFlagEncoder()
    {
        this(4, 1);
    }
    
    public WheelchairFlagEncoder( PMap properties )
    {
        this(
                (int) properties.getLong("speedBits", 4),
                properties.getDouble("speedFactor", 1)
        );
        this.properties = properties;
        this.setBlockFords(properties.getBool("blockFords", true));
    }
    
    public WheelchairFlagEncoder( String propertiesStr )
    {
        this(new PMap(propertiesStr));
    }

    public WheelchairFlagEncoder( int speedBits, double speedFactor )
    {
        super(speedBits, speedFactor, 0);
        restrictions.addAll(Arrays.asList("wheelchair", "access"));
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");
        restrictedValues.add("military");

        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");

        sidewalks.add("yes");
        sidewalks.add("both");
        sidewalks.add("left");
        sidewalks.add("right");

        setBlockByDefault(false);
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
        
        avoidHighwayTags.add("trunk");
        avoidHighwayTags.add("trunk_link");
        avoidHighwayTags.add("primary");
        avoidHighwayTags.add("primary_link");
        avoidHighwayTags.add("tertiary");
        avoidHighwayTags.add("tertiary_link");
        // for now no explicit avoiding #257
        //avoidHighwayTags.add("cycleway"); 

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
        
        //TODO validate
        maxPossibleSpeed = FERRY_SPEED;
    }

    @Override
    public int getVersion()
    {
        return 1;
    }

    @Override
    public int defineWayBits( int index, int shift )
    {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        // larger value required - ferries are faster than wheelchairs
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, MEAN_SPEED, maxPossibleSpeed);
        shift += speedEncoder.getBits();

        priorityWayEncoder = new EncodedValue("PreferWay", shift, 3, 1, 0, 7);
        shift += priorityWayEncoder.getBits();
        return shift;
    }
    
    @Override
    public int defineRelationBits( int index, int shift )
    {
        relationCodeEncoder = new EncodedValue("RelationCode", shift, 3, 1, 0, 7);
        return shift + relationCodeEncoder.getBits();
    }

    /**
     * Wheelchair flag encoder does not provide any turn cost / restrictions.
     * @param index
     * @param shift
     * @return 
     */
    @Override
    public int defineTurnBits( int index, int shift )
    {
        return shift;
    }
    
    /**
     * Wheelchair flag encoder does not provide any turn cost / restrictions
     * 
     * @param flag
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
     * @param flag
     * @return 0
     */    
    @Override
    public double getTurnCost( long flag )
    {
        return 0;
    }
    
    @Override
    public long getTurnFlags( boolean restricted, double costs )
    {
        return 0;
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

            // special case not for all acceptedRailways, only platform
            if (way.hasTag("railway", "platform"))
                return acceptBit;

            return 0;
        }

        if (way.hasTag("sidewalk", sidewalks))
            return acceptBit;

        // no need to evaluate ferries or fords - already included here
        if (way.hasTag("wheelchair", intendedValues))
            return acceptBit;
        
        // sometimes footways are tagged as paths
        // but hikingtrails are also often tagged as paths, so some more tagevaluation is required here
        if(way.hasTag("highway", "path") && way.hasTag("bicycle", intendedValues))
            return acceptBit;

        if (!allowedHighwayTags.contains(highwayValue))
            return 0;

        if (way.hasTag("motorroad", "yes"))
            return 0;

        // do not get our wheels wet
        if (isBlockFords() && (way.hasTag("highway", "ford") || way.hasTag("ford")))
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
        int code = 0;
        if (relation.hasTag("route", "ferry"))
        {
            code = PriorityCode.AVOID_IF_POSSIBLE.getValue();
        }

        int oldCode = (int) relationCodeEncoder.getValue(oldRelationFlags);
        if (oldCode < code)
            return relationCodeEncoder.setValue(0, code);
        return oldRelationFlags;
    }
    
    @Override
    public long handleWayTags(OSMWay way, long allowed, long relationFlags)
    {
        if (!isAccept(allowed))
            return 0;

        long encoded = 0;
        if (!isFerry(allowed))
        {
            encoded = speedEncoder.setDoubleValue(0, MEAN_SPEED);
            encoded |= directionBitMask;

            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if (isRoundabout)
                encoded = setBool(encoded, K_ROUNDABOUT, true);

        }
        else
        {
            encoded = encoded | handleFerryTags(way, SLOW_SPEED, MEAN_SPEED, FERRY_SPEED);
            encoded |= directionBitMask;
        }

        int priorityFromRelation = 0;
        if (relationFlags != 0)
            priorityFromRelation = (int) relationCodeEncoder.getValue(relationFlags);

        encoded = priorityWayEncoder.setValue(encoded, handlePriority(way, priorityFromRelation));
        return encoded;
    }
    
    @Override
    public double getDouble( long flags, int key )
    {
        switch (key)
        {
            case PriorityWeighting.KEY:
                return (double) priorityWayEncoder.getValue(flags) / BEST.getValue();
            default:
                return super.getDouble(flags, key);
}
    }

    protected int handlePriority( OSMWay way, int priorityFromRelation )
    {
        TreeMap<Double, Integer> weightToPrioMap = new TreeMap<Double, Integer>();
        if (priorityFromRelation == 0)
            weightToPrioMap.put(0d, UNCHANGED.getValue());
        else
            weightToPrioMap.put(110d, priorityFromRelation);

        collect(way, weightToPrioMap);

        // pick priority with biggest order value
        return weightToPrioMap.lastEntry().getValue();
    }

    /**
     * @param weightToPrioMap associate a weight with every priority. This sorted map allows
     * subclasses to 'insert' more important priorities as well as overwrite determined priorities.
     */
    void collect( OSMWay way, TreeMap<Double, Integer> weightToPrioMap )
    {
        String highway = way.getTag("highway");
        if (way.hasTag("foot", "designated"))
            weightToPrioMap.put(100d, PREFER.getValue());

        double maxSpeed = getMaxSpeed(way);
        if (safeHighwayTags.contains(highway) || maxSpeed > 0 && maxSpeed <= 20)
        {
            weightToPrioMap.put(40d, PREFER.getValue());
            if (way.hasTag("tunnel", intendedValues))
            {
                if (way.hasTag("sidewalk", "no"))
                    weightToPrioMap.put(40d, REACH_DEST.getValue());
                else
                weightToPrioMap.put(40d, UNCHANGED.getValue());
        }
        } else if (maxSpeed > 50 || avoidHighwayTags.contains(highway))
        {
            if (way.hasTag("sidewalk", "no"))
                weightToPrioMap.put(45d, WORST.getValue());
            else
                weightToPrioMap.put(45d, REACH_DEST.getValue());
        }

        if (way.hasTag("bicycle", "official") || way.hasTag("bicycle", "designated"))
            weightToPrioMap.put(44d, AVOID_IF_POSSIBLE.getValue());
        }

    @Override
    public boolean supports( Class<?> feature )
    {
        if (super.supports(feature))
            return true;

        return PriorityWeighting.class.isAssignableFrom(feature);
    }
    
    @Override
    public String toString()
    {
        return "wheelchair";
    }
}