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

import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Translation;
import java.text.DecimalFormat;
import java.util.*;

/**
 *
 * @author Philipp Thöricht
 */
public class BlindManFlagEncoder extends FootFlagEncoder
{
    private EncodedValue wayTypeEncoder;
    private EncodedValue surfaceEncoder;
    private HashMap<Long, String> rooms;
    private HashMap<Long, String> landmarks;
    private EncodedValue buildingIdEncoder;
    private EncodedValue levelEncoder;
    private EncodedValue levelsignEncoder;
    private EncodedValue endRoomEncoder;
    private EncodedValue landmarkEncoder;
    private final Set<String> indoorWayTypeSet = new HashSet<String>();
    private final Set<String> wayTypeSet = new HashSet<String>();
    private final Set<String> indoorSurfaceSet = new HashSet<String>();
    private final Set<String> surfaceSet = new HashSet<String>();
    private final Set<String> landmarkSet = new HashSet<String>();
    private final Map<String, Integer> wayTypeMap = new HashMap<String, Integer>();
    private final Map<String, Integer> surfaceMap = new HashMap<String, Integer>();
    private final Map<String, Integer> landmarkMap = new HashMap<String, Integer>();
    
    public BlindManFlagEncoder( PMap properties )
    {
        super(properties);
        
        indoorWayTypeSet.addAll(Arrays.asList("corridor", "foyer", "vestibule", "bridge"));
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
                "dirt", "grass", "concrete", "paving_stones", "sand", "cobblestone",
                "tactile_paving"));
        indoorSurfaceSet.addAll(Arrays.asList("carpet", "laminat", "doormat"));
        
        counter = 0;
        for (String s : surfaceSet)
            surfaceMap.put(s, counter++);
        for (String s : indoorSurfaceSet)
            surfaceMap.put(s, counter++);
        
        landmarkSet.addAll(Arrays.asList("_default", "entrance", "door", "elevator"));
        
        counter = 0;
        for (String s : landmarkSet)
        {
            landmarkMap.put(s, counter++);
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
    public int defineNodeBits( int index, int shift )
    {
        rooms = new HashMap<Long, String>();
        landmarks = new HashMap<Long, String>();
        shift = super.defineNodeBits(index, shift);
        
        return shift;
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
        
        buildingIdEncoder = new EncodedValue("buildingId", shift, 14, 1, 0, 9999);
        shift += buildingIdEncoder.getBits();
        endRoomEncoder = new EncodedValue("endRoom", shift, 14, 1, 0, 9999);
        shift += endRoomEncoder.getBits();
        levelEncoder = new EncodedValue("level", shift, 4, 1, 0, 10);
        shift += levelEncoder.getBits();
        levelsignEncoder = new EncodedValue("levelsign", shift, 1, 1, 0, 1);
        shift += levelEncoder.getBits();

        landmarkEncoder = new EncodedValue("landmark", shift, 3, 1, 0, landmarkMap.size(), true);
        shift += landmarkEncoder.getBits();
        
        //TODO: change values?
        wayTypeEncoder = new EncodedValue("WayType", shift, 4, 1, 5, wayTypeMap.size(), true);
        shift += wayTypeEncoder.getBits();
        
        surfaceEncoder = new EncodedValue("surface", shift, 4, 1, 0, surfaceMap.size(), true);
        shift += surfaceEncoder.getBits();
        
        return shift;
    }
    
    @Override
    public long handleNodeTags( OSMNode node )
    {
        long encoded = super.handleNodeTags(node);
        if (node.hasTag("door", "yes"))
        {
            landmarks.put(node.getId(), "door");
            if(node.hasTag("ref"))
                rooms.put(node.getId(), "ref:" + node.getTag("ref"));
        }
        if (node.hasTag("entrance"))
            landmarks.put(node.getId(), "entrance");
        if (node.hasTag("elevator"))
            landmarks.put(node.getId(), "elevator");
        
        return encoded;
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
        
//        if(landmarks.containsKey(way.getNodes().get(0)))
//        {
//            String startroom = landmarks.get(way.getNodes().get(0));
////            String buildingid = startroom.substring(4, 8);
//            String level = startroom.substring(8, 10);
//            startroom = startroom.substring(11);
//            
//            encoded = startRoomEncoder.setValue(encoded, Long.valueOf(startroom));
//            encoded = levelEncoder.setValue(encoded, Long.valueOf(level));
//        }
        
        if (way.getNodes().size() > 1)
        {
            long key = way.getNodes().get(way.getNodes().size() - 1);
//            if (rooms.containsKey(key))
//            {
                String buildingid = "";
                String level = "";
                long levelsign = 1;
                String endroom = rooms.get(key);
                if (endroom != null && endroom.length() > 11)
                {
                    buildingid = endroom.substring(4, 8);
                    level = endroom.substring(8, 10);
                    if (level.contains("-")) {
                        level = String.valueOf(Math.abs(Long.valueOf(level)));
                        levelsign = 0;
                    }
                    endroom = endroom.substring(11);
                }
                else
                {
                    buildingid = "0";
                    level = "0";
                    endroom = "0";
                }
                encoded = buildingIdEncoder.setValue(encoded, Long.valueOf(buildingid));
                encoded = levelEncoder.setValue(encoded, Long.valueOf(level));
                encoded = levelsignEncoder.setValue(encoded, levelsign);
                encoded = endRoomEncoder.setValue(encoded, Long.valueOf(endroom));
//            }
//            if (landmarks.containsKey(key))
//            {
                Integer value = landmarkMap.get(landmarks.get(key));
                if (value == null)
                    value = landmarkMap.get("_default");
                encoded = landmarkEncoder.setValue(encoded, value);
//            }
        }
        
        // waytype
        Integer wValue;
        if (way.hasTag("indoor", indoorWayTypeSet))    // prefer indoor ways over normal ways
            wValue = wayTypeMap.get(way.getTag("indoor"));
        else
            wValue = wayTypeMap.get(way.getTag("highway"));
        
        if (wValue == null)
            wValue = wayTypeMap.get("_default");
        
        encoded = wayTypeEncoder.setValue(encoded, wValue);
        
        // level
        if (way.hasTag("level")) {
            long lvl = Long.valueOf(way.getTag("level"));
            encoded = levelEncoder.setValue(encoded, Math.abs(lvl));
            if (lvl < 0) {
                encoded = levelsignEncoder.setValue(encoded, 0);
            } else {
                encoded = levelsignEncoder.setValue(encoded, 1);
            }
        }

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
//        long level = levelEncoder.getValue(flags);
//        DecimalFormat df1 = new DecimalFormat("#00");
//        String levelstring = df1.format(level);
//        long startroomid = startRoomEncoder.getValue(flags);
//        DecimalFormat df1 = new DecimalFormat("#0000");
//        DecimalFormat df2 = new DecimalFormat("#00");
//        if (startroomid > 0)
//        {
//            String formatted = df2.format(startroomid);
//            ia.add(new InstructionAnnotation(0, "startRoom", levelstring + "." + formatted));
//        }
        long buildingid = buildingIdEncoder.getValue(flags);
        long level = levelEncoder.getValue(flags);
        long levelsign = levelsignEncoder.getValue(flags);
        if (levelsign == 0) {
            level *= -1;
        }
        long endroomid = endRoomEncoder.getValue(flags);
        if (endroomid > 0)
        {
            String formatted = String.valueOf(buildingid) + String.valueOf(level) + "." + String.valueOf(endroomid); //df1.format(buildingid) + df2.format(level) + "." + df1.format(endroomid);
            ia.add(new InstructionAnnotation(0, "room", formatted));
        }
        
        if(level != Double.NaN)
            ia.add(new InstructionAnnotation(0, "level", String.valueOf(level)));
        
        int landmark = (int) landmarkEncoder.getValue(flags);
        for (String key : landmarkMap.keySet())
        {
            if (landmarkMap.get(key).equals(landmark))
                ia.add(new InstructionAnnotation(0, "landmark", key));
        }
        
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
    
    /**
     * 
     * @param flags
     * @return 
     */
    public String getWayType( long flags )
    {
        int wayType = (int) wayTypeEncoder.getValue(flags);
        for (String key : wayTypeMap.keySet())
        {
            if (wayTypeMap.get(key).equals(wayType))
                return key;
        }
        
        return "";
    }
}
