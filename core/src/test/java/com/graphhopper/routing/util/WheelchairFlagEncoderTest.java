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
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphBuilder;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.GHUtility;


import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, Philipp Thöricht
 */
public class WheelchairFlagEncoderTest
{
    private final EncodingManager encodingManager = new EncodingManager("car,wheelchair");
    private final WheelchairFlagEncoder wheelchairEncoder = (WheelchairFlagEncoder) encodingManager.getEncoder("wheelchair");

    @Test
    public void testGetSpeed()
    {
        long fl = wheelchairEncoder.setProperties(10, true, true);
        assertEquals(10, wheelchairEncoder.getSpeed(fl), 1e-1);
    }

    @Test
    public void testBasics()
    {
        long fl = wheelchairEncoder.flagsDefault(true, true);
        assertEquals(WheelchairFlagEncoder.MEAN_SPEED, wheelchairEncoder.getSpeed(fl), 1e-1);

        long fl1 = wheelchairEncoder.flagsDefault(true, false);
        long fl2 = wheelchairEncoder.reverseFlags(fl1);
        assertEquals(wheelchairEncoder.getSpeed(fl2), wheelchairEncoder.getSpeed(fl1), 1e-1);
    }

    @Test
    public void testCombined()
    {
        FlagEncoder carEncoder = encodingManager.getEncoder("car");
        long fl = wheelchairEncoder.setProperties(10, true, true) | carEncoder.setProperties(100, true, false);
        assertEquals(10, wheelchairEncoder.getSpeed(fl), 1e-1);
        assertTrue(wheelchairEncoder.isForward(fl));
        assertTrue(wheelchairEncoder.isBackward(fl));

        assertEquals(100, carEncoder.getSpeed(fl), 1e-1);
        assertTrue(carEncoder.isForward(fl));
        assertFalse(carEncoder.isBackward(fl));

        assertEquals(0, carEncoder.getSpeed(wheelchairEncoder.setProperties(10, true, true)), 1e-1);
    }

    @Test
    public void testGraph()
    {
        Graph g = new GraphBuilder(encodingManager).create();
        g.edge(0, 1).setDistance(10).setFlags(wheelchairEncoder.setProperties(10, true, true));
        g.edge(0, 2).setDistance(10).setFlags(wheelchairEncoder.setProperties(5, true, true));
        g.edge(1, 3).setDistance(10).setFlags(wheelchairEncoder.setProperties(10, true, true));
        EdgeExplorer out = g.createEdgeExplorer(new DefaultEdgeFilter(wheelchairEncoder, false, true));
        assertEquals(GHUtility.asSet(1, 2), GHUtility.getNeighbors(out.setBaseNode(0)));
        assertEquals(GHUtility.asSet(0, 3), GHUtility.getNeighbors(out.setBaseNode(1)));
        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(out.setBaseNode(2)));
    }

    @Test
    public void testAccess()
    {
        OSMWay way = new OSMWay(1);

        way.setTag("highway", "motorway");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("sidewalk", "yes");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("sidewalk", "left");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "motorway");
        way.setTag("sidewalk", "none");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "pedestrian");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "footway");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("smoothness", "bad");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "path");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("wheelchair", "official");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "service");
        way.setTag("access", "no");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "tertiary");
        way.setTag("motorroad", "yes");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "cycleway");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("foot", "yes");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);
        
        way.clearTags();
        way.setTag("highway", "track");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("tracktype", "grade4");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        way.removeTag("tracktype");
        way.setTag("tracktype", "grade2");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);
        way.removeTag("tracktype");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("highway", "track");
        way.setTag("ford", "yes");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("wheelchair", "yes");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);

        way.clearTags();
        way.setTag("route", "ferry");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);
        way.setTag("foot", "no");
        assertFalse(wheelchairEncoder.acceptWay(way) > 0);
        
        way.clearTags();
        way.setTag("highway", "residential");
        way.setTag("incline", "down");
        assertTrue(wheelchairEncoder.acceptWay(way) > 0);
    }

    @Test
    public void testMixSpeedAndSafe()
    {
        OSMWay way = new OSMWay(1);

        way.setTag("highway", "motorway");
        long flags = wheelchairEncoder.handleWayTags(way, wheelchairEncoder.acceptWay(way), 0);
        assertEquals(0, flags);

        way.setTag("sidewalk", "yes");
        flags = wheelchairEncoder.handleWayTags(way, wheelchairEncoder.acceptWay(way), 0);
        assertEquals(5, wheelchairEncoder.getSpeed(flags), 1e-1);

//        way.clearTags();
//        way.setTag("highway", "track");
//        flags = wheelchairEncoder.handleWayTags(way, wheelchairEncoder.acceptWay(way), 0);
//        assertEquals(5, wheelchairEncoder.getSpeed(flags), 1e-1);
    }

    @Test
    public void testTurnFlagEncoding_noCostsAndRestrictions()
    {
        long flags_r0 = wheelchairEncoder.getTurnFlags(true, 0);
        long flags_0 = wheelchairEncoder.getTurnFlags(false, 0);

        long flags_r20 = wheelchairEncoder.getTurnFlags(true, 20);
        long flags_20 = wheelchairEncoder.getTurnFlags(false, 20);

        assertEquals(0, wheelchairEncoder.getTurnCost(flags_r0), 1e-1);
        assertEquals(0, wheelchairEncoder.getTurnCost(flags_0), 1e-1);

        assertEquals(0, wheelchairEncoder.getTurnCost(flags_r20), 1e-1);
        assertEquals(0, wheelchairEncoder.getTurnCost(flags_20), 1e-1);

        assertFalse(wheelchairEncoder.isTurnRestricted(flags_r0));
        assertFalse(wheelchairEncoder.isTurnRestricted(flags_0));

        assertFalse(wheelchairEncoder.isTurnRestricted(flags_r20));
        assertFalse(wheelchairEncoder.isTurnRestricted(flags_20));
    }
    
    @Test
    public void testPriority()
    {
        OSMWay way = new OSMWay(1);
        way.clearTags();
        way.setTag("smoothness", "good");
        assertEquals(PriorityCode.VERY_NICE.getValue(), wheelchairEncoder.handlePriority(way, 0));
    }
}
