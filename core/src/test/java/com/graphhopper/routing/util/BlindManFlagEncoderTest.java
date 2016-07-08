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
import com.graphhopper.util.Translation;
import static com.graphhopper.util.TranslationMapTest.SINGLETON;
import java.util.*;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Philipp Thöricht
 */
public class BlindManFlagEncoderTest
{    
    @Test
    public void testAnnotations1()
    {
        OSMWay way = new OSMWay(1);
        way.setTag("highway", "footway");
        way.setTag("indoor", "corridor");
        
        EncodingManager encodingManager = new EncodingManager("BLINDMAN", 8);
        BlindManFlagEncoder bmfe = (BlindManFlagEncoder) encodingManager.getEncoder("BLINDMAN");
        assertTrue(bmfe.acceptWay(way) > 0);
        
        long flags = bmfe.handleWayTags(way, bmfe.acceptWay(way), 0);
        Translation tr = SINGLETON.getWithFallBack(Locale.UK);
        List<InstructionAnnotation> ias = bmfe.getAnnotations(flags, tr);
        String wayname = "";
        for (InstructionAnnotation ia : ias)
        {
            if (ia.getType().equals("wayType"))
            {
                wayname = ia.getMessage();
                break;
            }
        }
        assertEquals("corridor", wayname);
        
        way.setTag("highway", "elevator");
        way.removeTag("indoor");
        flags = bmfe.handleWayTags(way, bmfe.acceptWay(way), 0);
        ias = bmfe.getAnnotations(flags, tr);
        for (InstructionAnnotation ia : ias)
        {
            if (ia.getType().equals("wayType"))
            {
                wayname = ia.getMessage();
                break;
            }
        }
        assertEquals("elevator", wayname);
        
        way.removeTag("highway");
        way.setTag("highway", "steps");
        way.setTag("lit", "yes");
        flags = bmfe.handleWayTags(way, bmfe.acceptWay(way), 0);
        ias = bmfe.getAnnotations(flags, tr);
        for (InstructionAnnotation ia : ias)
        {
            if (ia.getType().equals("wayType"))
            {
                wayname = ia.getMessage();
                break;
            }
        }
        assertEquals("steps", wayname);
    }
    
    @Test
    public void testSurfaceAnnotations()
    {
        OSMWay way = new OSMWay(1);
        way.setTag("highway", "footway");
        way.setTag("surface", "cobblestone");
        
        EncodingManager encodingManager = new EncodingManager("BLINDMAN", 8);
        BlindManFlagEncoder bmfe = (BlindManFlagEncoder) encodingManager.getEncoder("BLINDMAN");
        assertTrue(bmfe.acceptWay(way) > 0);
        
        long flags = bmfe.handleWayTags(way, bmfe.acceptWay(way), 0);
        Translation tr = SINGLETON.getWithFallBack(Locale.UK);
        List<InstructionAnnotation> ias = bmfe.getAnnotations(flags, tr);
        String surface = "";
        for (InstructionAnnotation ia : ias)
        {
            if (ia.getType().equals("surface"))
            {
                surface = ia.getMessage();
                break;
            }
        }
        assertEquals("cobblestone", surface);
        
        way = new OSMWay(1);
        way.setTag("highway", "service");
        way.setTag("name", "Am Jägerpark");
        way.setTag("oneway", "yes");
        
        flags = bmfe.handleWayTags(way, bmfe.acceptWay(way), 0);
        ias = bmfe.getAnnotations(flags, tr);
        surface = "";
        for (InstructionAnnotation ia : ias)
        {
            if (ia.getType().equals("surface"))
            {
                surface = ia.getMessage();
                break;
            }
        }
        assertEquals("_default", surface);
    }
}
