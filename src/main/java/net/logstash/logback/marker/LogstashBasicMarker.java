/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logback.marker;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Marker;

/* Copy of {@link org.slf4j.helpers.BasicMarker} from slf4j-api v1.7.31, with minor changes:
 * 1. make the constructor public so that it can be extended in other packages
 * 2. add getReferences() method

 * slf4j-api, {@link org.slf4j.helpers.BasicMarker}, and the portions
 * of this class that have been copied from BasicMarker are provided under
 * the MIT License copied here:
 * 
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
/**
 * A simple implementation of the {@link Marker} interface.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Joern Huxhorn
*/
@SuppressWarnings("serial")
public class LogstashBasicMarker implements Marker {

    private static final String OPEN = "[ ";
    private static final String CLOSE = " ]";
    private static final String SEP = ", ";
    
    private final String name;
    private final List<Marker> referenceList = new CopyOnWriteArrayList<>();
    
    /*
     * BEGIN Modification in logstash-logback-encoder to make this constructor public
     */
    public LogstashBasicMarker(String name) {
    /*
     * END Modification in logstash-logback-encoder to make this constructor public
     */
        if (name == null) {
            throw new IllegalArgumentException("A marker name cannot be null");
        }
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Marker reference) {
        if (reference == null) {
            throw new IllegalArgumentException("A null value cannot be added to a Marker as reference.");
        }

        // no point in adding the reference multiple times
        if (this.contains(reference)) {
            return;
        }
        if (reference.contains(this)) { // avoid recursion, a potential reference should not hold its future "parent" as a reference
            return;
        }
        
        referenceList.add(reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasReferences() {
        return !referenceList.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public boolean hasChildren() {
        return hasReferences();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Marker> iterator() {
        return referenceList.iterator();
    }

    /*
     * BEGIN Modification in logstash-logback-encoder to add this method
     */
    protected List<Marker> getReferences() {
        return Collections.unmodifiableList(referenceList);
    }
    /*
     * END Modification in logstash-logback-encoder to add this method
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Marker referenceToRemove) {
        return referenceList.remove(referenceToRemove);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Marker other) {
        if (other == null) {
            throw new IllegalArgumentException("Other cannot be null");
        }

        if (this.equals(other)) {
            return true;
        }

        if (hasReferences()) {
            for (Marker ref : referenceList) {
                if (ref.contains(other)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Other cannot be null");
        }

        if (this.name.equals(name)) {
            return true;
        }

        if (hasReferences()) {
            for (Marker ref : referenceList) {
                if (ref.contains(name)) {
                    return true;
                }
            }
        }
        
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Marker)) {
            return false;
        }

        final Marker other = (Marker) obj;
        return name.equals(other.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        if (!this.hasReferences()) {
            return this.getName();
        }
        StringBuilder sb = new StringBuilder(this.getName())
                .append(' ')
                .append(OPEN);
        Iterator<Marker> it = this.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getName());
            if (it.hasNext()) {
                sb.append(SEP);
            }
        }
        sb.append(CLOSE);

        return sb.toString();
    }
}
