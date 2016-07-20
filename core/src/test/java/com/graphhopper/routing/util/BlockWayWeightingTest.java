package com.graphhopper.routing.util;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPoint;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Philipp Thöricht <philipp.thoericht@tu-dresden.de>
 */
public class BlockWayWeightingTest
{
    private static GraphHopper gh;
    private static final String graphFileBlindman = "target/gh-blockwaystest";
    private static final String osmFile = "files/blockwaystest.osm";
    
    @BeforeClass
    public static void beforeClass()
    {
        // make sure we are using fresh graphhopper files with correct vehicle
        Helper.removeDir(new File(graphFileBlindman));

        gh = new MyGraphHopper().
                setStoreOnFlush(true).
                setOSMFile(osmFile).
                setCHEnable(false).
                setGraphHopperLocation(graphFileBlindman).
                setEncodingManager(new EncodingManager("BLINDMAN", 8)).
                importOrLoad();
    }
    
    @Test
    public void testBlockedSteps()
    {
        GHRequest req = new GHRequest().
                addPoint(new GHPoint(51.052764596113, 13.778019504270393), 0.).
                addPoint(new GHPoint(51.052725131037995, 13.780775418583353), 190.).
                setVehicle("BLINDMAN").setWeighting("blockway");
        req.getHints().put("block_way", "steps");
        GHResponse rsp = gh.route(req);
        PathWrapper arsp = rsp.getBest();
        assertEquals(396.9, arsp.getDistance(), 1.);
        assertEquals(6, arsp.getPoints().getSize());
        
        // now unblock steps
        req.getHints().remove("block_way");
        rsp = gh.route(req);
        arsp = rsp.getBest();
        assertEquals(192.9, arsp.getDistance(), 1.);
        assertEquals(4, arsp.getPoints().getSize());
    }
    
    private static class MyGraphHopper extends GraphHopper
    {
        @Override
        public Weighting createWeighting( WeightingMap wMap, FlagEncoder encoder )
        {
            String weighting = wMap.getWeighting();
            if ("blockway".equalsIgnoreCase(weighting)) 
            {
                BlockWayWeighting w = new BlockWayWeighting(encoder, wMap);
                return w;
            } else 
            {
                return super.createWeighting(wMap, encoder);
            }
        }
    }
}
