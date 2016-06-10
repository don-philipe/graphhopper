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
package com.graphhopper;

import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.BBox;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class holds the data like points and instructions of a Path.
 * <p>
 * @author Peter Karich
 */
public class PathWrapper
{
    private List<String> description;
    private double distance;
    private double ascend;
    private double descend;
    private double routeWeight;
    private long time;
    private String debugInfo = "";
    private InstructionList instructions;
    private PointList list = PointList.EMPTY;
    private final List<Throwable> errors = new ArrayList<Throwable>(4);
    private String detailedOverview = "";

    /**
     * @return the description of this route alternative to make it meaningful for the user e.g. it
     * displays one or two main roads of the route.
     */
    public List<String> getDescription()
    {
        if (description == null)
            return Collections.emptyList();
        return description;
    }

    public PathWrapper setDescription( List<String> names )
    {
        this.description = names;
        return this;
    }

    public PathWrapper addDebugInfo( String debugInfo )
    {
        if (debugInfo == null)
            throw new IllegalStateException("Debug information has to be none null");

        if (!this.debugInfo.isEmpty())
            this.debugInfo += ";";

        this.debugInfo += debugInfo;
        return this;
    }

    public String getDebugInfo()
    {
        return debugInfo;
    }

    public PathWrapper setPoints( PointList points )
    {
        list = points;
        return this;
    }

    /**
     * This method returns all points on the path. Keep in mind that calculating the distance from
     * these points might yield different results compared to getDistance as points could have been
     * simplified on import or after querying.
     */
    public PointList getPoints()
    {
        check("getPoints");
        return list;
    }

    public PathWrapper setDistance( double distance )
    {
        this.distance = distance;
        return this;
    }

    /**
     * This method returns the distance of the path. Always prefer this method over
     * getPoints().calcDistance
     * <p>
     * @return distance in meter
     */
    public double getDistance()
    {
        check("getDistance");
        return distance;
    }

    public PathWrapper setAscend( double ascend )
    {
        if (ascend < 0)
            throw new IllegalArgumentException("ascend has to be strictly positive");

        this.ascend = ascend;
        return this;
    }

    /**
     * This method returns the total elevation change (going upwards) in meter.
     * <p>
     * @return ascend in meter
     */
    public double getAscend()
    {
        return ascend;
    }

    public PathWrapper setDescend( double descend )
    {
        if (descend < 0)
            throw new IllegalArgumentException("descend has to be strictly positive");

        this.descend = descend;
        return this;
    }

    /**
     * This method returns the total elevation change (going downwards) in meter.
     * <p>
     * @return decline in meter
     */
    public double getDescend()
    {
        return descend;
    }

    public PathWrapper setTime( long timeInMillis )
    {
        this.time = timeInMillis;
        return this;
    }

    /**
     * @return time in millis
     */
    public long getTime()
    {
        check("getTimes");
        return time;
    }

    public PathWrapper setRouteWeight( double weight )
    {
        this.routeWeight = weight;
        return this;
    }

    /**
     * This method returns a double value which is better than the time for comparison of routes but
     * only if you know what you are doing, e.g. only to compare routes gained with the same query
     * parameters like vehicle.
     */
    public double getRouteWeight()
    {
        check("getRouteWeight");
        return routeWeight;
    }

    /**
     * Calculates the bounding box of this route response
     */
    public BBox calcRouteBBox( BBox _fallback )
    {
        check("calcRouteBBox");
        BBox bounds = BBox.createInverse(_fallback.hasElevation());
        int len = list.getSize();
        if (len == 0)
            return _fallback;

        for (int i = 0; i < len; i++)
        {
            double lat = list.getLatitude(i);
            double lon = list.getLongitude(i);
            if (bounds.hasElevation())
            {
                double ele = list.getEle(i);
                bounds.update(lat, lon, ele);
            } else
            {
                bounds.update(lat, lon);
            }
        }
        return bounds;
    }

    @Override
    public String toString()
    {
        String str = "nodes:" + list.getSize() + "; " + list.toString();
        if (instructions != null && !instructions.isEmpty())
            str += ", " + instructions.toString();

        if (hasErrors())
            str += ", " + errors.toString();

        return str;
    }

    public void setInstructions( InstructionList instructions )
    {
        this.instructions = instructions;
    }

    public InstructionList getInstructions()
    {
        check("getInstructions");
        if (instructions == null)
            throw new IllegalArgumentException("To access instructions you need to enable creation before routing");

        return instructions;
    }

    private void check( String method )
    {
        if (hasErrors())
        {
            throw new RuntimeException("You cannot call " + method + " if response contains errors. Check this with ghResponse.hasErrors(). "
                    + "Errors are: " + getErrors());
        }
    }

    /**
     * @return true if this alternative response contains one or more errors
     */
    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    public List<Throwable> getErrors()
    {
        return errors;
    }

    public PathWrapper addError( Throwable error )
    {
        errors.add(error);
        return this;
    }

    public PathWrapper addErrors( List<Throwable> errors )
    {
        this.errors.addAll(errors);
        return this;
    }
    
    /**
     * 
     * @param additionalFeatures k:feature - v:quantity
     * @param tr
     * @return 
     */
    public PathWrapper setDetailedOverview( HashMap<String, Integer> additionalFeatures, Translation tr )
    {
        String start = this.instructions.get(0).getName();
        if (start.isEmpty())
        {
            for (InstructionAnnotation ia : this.instructions.get(0).getAnnotations())
            {
                if (ia.getType().equals("wayType"))
                {
                    start = ia.getMessage();
                    break;
                }
            }
        }
        String finish = this.instructions.get(this.instructions.size() - 2).getName();
        if (finish.isEmpty())
        {
            for (InstructionAnnotation ia : this.instructions.get(this.instructions.size() - 2).getAnnotations())
            {
                if (ia.getType().equals("wayType"))
                {
                    finish = ia.getMessage();
                    break;
                }
            }
        }
        DecimalFormat df = new DecimalFormat("#");  // round distance to full meters
        this.detailedOverview = "The calculated route leads from "
                + start + " to "
                + finish + ". The route is " 
                + df.format(this.distance)
                + " meters long";
        
        String featurestring = "";
        boolean firstrun = true;
        for (String feature : additionalFeatures.keySet())
        {
            if (feature.equals("steps"))
            {
                if (!firstrun)
                    featurestring += " " + tr.tr("and") + " ";
                else
                    firstrun = false;
                int quantity = additionalFeatures.get(feature);
                featurestring += quantity + " stairs";
            }
            if (feature.equals("elevator"))
            {
                if (!firstrun)
                    featurestring += " " + tr.tr("and") + " ";
                else
                    firstrun = false;
                int quantity = additionalFeatures.get(feature);
                if (quantity > 1)
                    featurestring += quantity + feature + "s";
                else
                    featurestring += quantity + " " + feature;
            }
        }
        
        if (!featurestring.isEmpty())
            this.detailedOverview += " and contains " + featurestring;
        
        this.detailedOverview += ".";
        
        return this;
    }
    
    /**
     * 
     * @return Overview when this feature is enabled or an empty String otherwise.
     */
    public String getDetailedOverview()
    {
        return this.detailedOverview;
    }
}
