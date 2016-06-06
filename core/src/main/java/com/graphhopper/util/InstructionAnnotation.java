/*
 *  Licensed to Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  Peter Karich licenses this file to you under the Apache License,
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
package com.graphhopper.util;

/**
 * @author Peter Karich
 * @author Philipp Thöricht
 */
public class InstructionAnnotation implements Comparable<InstructionAnnotation>
{
    public final static InstructionAnnotation EMPTY = new InstructionAnnotation();
    private boolean empty;
    private int importance;
    private String type;
    private String message;

    private InstructionAnnotation()
    {
        setEmpty();
    }

    /**
     * 
     * @param importance
     * @param type the type of this annotation
     * @param message 
     */
    public InstructionAnnotation( int importance, String type, String message )
    {
        if (type.isEmpty() && message.isEmpty() && importance == 0)
        {
            setEmpty();
        } else
        {
            this.empty = false;
            this.importance = importance;
            this.type = type;
            this.message = message;
        }
    }

    private void setEmpty()
    {
        this.empty = true;
        this.importance = 0;
        this.type = "";
        this.message = "";
    }

    public boolean isEmpty()
    {
        return empty;
    }

    public int getImportance()
    {
        return importance;
    }
    
    public String getType()
    {
        return type;
    }

    public String getMessage()
    {
        return message;
    }

    @Override
    public String toString()
    {
        return importance + ": " + getMessage();
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 83 * hash + this.importance;
        hash = 83 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 83 * hash + (this.message != null ? this.message.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final InstructionAnnotation other = (InstructionAnnotation) obj;
        if (this.importance != other.importance)
            return false;
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type))
            return false;
        if ((this.message == null) ? (other.message != null) : !this.message.equals(other.message))
            return false;
        return true;
    }

    /**
     * Compare annotations by their importance.
     * @param t
     * @return 1 if this annotation is more important than the other, 0 if importance of both is 
     * equal and -1 if this annotation is less important than the other.
     */
    @Override
    public int compareTo( InstructionAnnotation t )
    {
        if (this.importance > t.importance)
            return 1;
        else if (this.importance == t.importance)
            return 0;
        else
            return -1;
    }
}
