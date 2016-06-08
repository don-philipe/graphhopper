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
    private final Set<String> indoorWayTypeSet = new HashSet<String>();
    private final Set<String> wayTypeSet = new HashSet<String>();
    private final Set<String> surfaceSet = new HashSet<String>();
    private final Map<String, Integer> wayTypeMap = new HashMap<String, Integer>();
    private final Map<String, Integer> surfaceMap = new HashMap<String, Integer>();
    
    public BlindManFlagEncoder( PMap properties )
    {
        super(properties);
        
        indoorWayTypeSet.addAll(Arrays.asList("corridor", "foyer"));
        wayTypeSet.addAll(Arrays.asList("footway", "steps", "road", "elevator"));
        
        List<String> allWayTypeList = new LinkedList<String>();
        allWayTypeList.add("_default");
        allWayTypeList.addAll(indoorWayTypeSet);
        allWayTypeList.addAll(wayTypeSet);
        int counter = 0;
        for (String s : allWayTypeList)
        {
            wayTypeMap.put(s, counter++);
        }
        
        surfaceSet.addAll(Arrays.asList("_default", "asphalt", "unpaved", "paved", "gravel",
                "ground", "dirt", "grass", "concrete", "paving_stones", "sand", "compacted", "cobblestone",
                "tactile_paving"));
        
        counter = 0;
        for (String s : surfaceSet)
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
        
        // waytype
        Integer wValue;
        if (way.hasTag("indoor", indoorWayTypeSet))    // prefer indoor ways over normal ways
            wValue = wayTypeMap.get(way.getTag("indoor"));
        else
            wValue = wayTypeMap.get(way.getTag("highway"));
        
        if (wValue == null)
            wValue = wayTypeMap.get("_default");
        
        encoded = wayTypeEncoder.setValue(encoded, wValue);
        
        // surface
        Integer sValue;
        if (way.hasTag("tactile_paving", "yes"))    // prefer tactile paving if present
            sValue = surfaceMap.get("tactile_paving");
        else
            sValue = surfaceMap.get(way.getTag("surface"));
        
        if (sValue == null)
            sValue = surfaceMap.get("_default");
            
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
        for (String key : wayTypeMap.keySet())
        {
            if (wayTypeMap.get(key).equals(wayType))
                ia.add(new InstructionAnnotation(0, "wayType", key));
        }
        
        int surfaceType = (int) surfaceEncoder.getValue(flags);
        for (String key : surfaceMap.keySet())
        {
            if (surfaceMap.get(key).equals(surfaceType))
                ia.add(new InstructionAnnotation(0, "surface", key));
        }
        
        return ia;
    }
}
