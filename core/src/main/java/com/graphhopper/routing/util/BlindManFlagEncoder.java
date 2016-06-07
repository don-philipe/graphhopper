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

import com.graphhopper.reader.OSMWay;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Translation;
import java.util.*;

/**
 *
 * @author Philipp Thöricht
 */
public class BlindManFlagEncoder extends FootFlagEncoder
{
    private EncodedValue wayTypeEncoder;
    private EncodedValue surfaceEncoder;
    private final Map<String, Integer> surfaceMap = new HashMap<String, Integer>();
    
    public BlindManFlagEncoder( PMap properties )
    {
        super(properties);
        
        List<String> surfaceList = Arrays.asList("_default", "asphalt", "unpaved", "paved", "gravel",
                "ground", "dirt", "grass", "concrete", "paving_stones", "sand", "compacted", "cobblestone",
                "tactile_paving");
        int counter = 0;
        for (String s : surfaceList)
        {
            surfaceMap.put(s, counter++);
        }
    }
    
    @Override
    public int getVersion()
    {
        return 1;
    }
    
    @Override
    public String toString()
    {
        return "blindman";
    }
    
    /**
     * 
     * @param index
     * @param shift
     * @return 
     */
    @Override
    public int defineWayBits( int index, int shift )
    {
        shift = super.defineWayBits(index, shift);
        
        //TODO: change values?
        wayTypeEncoder = new EncodedValue("WayType", shift, 3, 1, 5, 6, true);
        shift += wayTypeEncoder.getBits();
        
        surfaceEncoder = new EncodedValue("surface", shift, 4, 1, 0, surfaceMap.size(), true);
        shift += surfaceEncoder.getBits();
        
        return shift;
    }
    
    /**
     * Called during import process.
     * @param way
     * @param allowed
     * @param relationFlags
     * @return 
     */
    @Override
    public long handleWayTags( OSMWay way, long allowed, long relationFlags )
    {
        long encoded = super.handleWayTags(way, allowed, relationFlags);
        encoded = handleBlindManRelated(way, encoded);
        
        // surface
        Integer sValue;
        if (way.hasTag("tactile_paving", "yes"))    // prefer tactile paving if present
            sValue = surfaceMap.get("tactile_paving");
        else
            sValue = surfaceMap.get(way.getTag("surface"));
        
        if (sValue == null)
            sValue = 0;
            
        encoded = surfaceEncoder.setValue(encoded, sValue);
        
        return encoded;
    }
    
    /**
     * Called during routing.
     * @param flags
     * @param tr
     * @return 
     */
    @Override
    public List<InstructionAnnotation> getAnnotations( long flags, Translation tr )
    {
        List<InstructionAnnotation> ia = new ArrayList<InstructionAnnotation>();
        int wayType = (int) wayTypeEncoder.getValue(flags);
        String wayName = getWayName(wayType, tr);
        ia.add(new InstructionAnnotation(0, "wayType", wayName));
        
        int surfaceType = (int) surfaceEncoder.getValue(flags);
        for (String key : surfaceMap.keySet())
        {
            if (surfaceMap.get(key).equals(surfaceType))
                ia.add(new InstructionAnnotation(0, "surface", key));
        }
        
        return ia;
    }

    /**
     * 
     * @param wayType
     * @param tr
     * @return 
     */
    private String getWayName( int wayType, Translation tr )
    {
        String wayTypeName = "";
        //TODO:
        switch (wayType)
        {
            case 0:
//                wayTypeName = tr.tr("corridor");
                wayTypeName = "corridor";
                break;
            case 1:
//                wayTypeName = tr.tr("foyer");
                wayTypeName = "foyer";
                break;
            case 2:
//                wayTypeName = tr.tr("steps");
                wayTypeName = "steps";
                break;
            case 3:
//                wayTypeName = tr.tr("elevator");
                wayTypeName = "elevator";
                break;
            case 4:
//                wayTypeName = tr.tr("footway");
                wayTypeName = "footway";
                break;
            case 5:
//                wayTypeName = tr.tr("other_way");
                wayTypeName = "other_way";
                break;
        }
        return wayTypeName;
    }
    
    /**
     * 
     * @param way
     * @param encoded
     * @return 
     */
    private long handleBlindManRelated( OSMWay way, long encoded )
    {
        WayType wayType;
        if (way.hasTag("indoor", "corridor"))
            wayType = WayType.CORRIDOR;
        else if (way.hasTag("indoor", "foyer"))
        {
            wayType = WayType.FOYER;
//            if (way.hasTag("tactile_paving", "yes"))
        }
        else if (way.hasTag("highway", "steps"))
            wayType = WayType.STEPS;
        else if (way.hasTag("highway", "elevator"))
            wayType = WayType.ELEVATOR;
        else if (way.hasTag("highway", "footway"))
            wayType = WayType.FOOTWAY;
        else
            wayType = WayType.OTHER_WAY;
            
        //TODO
        return wayTypeEncoder.setValue(encoded, wayType.getValue());
    }
    
    /**
     * 
     */
    private enum WayType
    {
        //TODO:
        CORRIDOR(0),
        FOYER(1),
        STEPS(2),
        ELEVATOR(3),
        FOOTWAY(4),
        OTHER_WAY(5);

        private final int value;

        private WayType( int value )
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return this.value;
        }
    }
}
