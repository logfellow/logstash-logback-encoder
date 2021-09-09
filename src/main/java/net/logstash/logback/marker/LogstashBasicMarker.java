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
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Marker;

/**
 * A simple implementation of the SLF4J {@link Marker} interface.
 */
@SuppressWarnings("serial")
class LogstashBasicMarker implements Marker {

    /**
     * The marker name
     */
    private final String name;
    
    /**
     * Referenced markers - initialized the first time a marker is added
     */
    private volatile List<Marker> referenceList;
    
    LogstashBasicMarker(String name) {
        this.name = Objects.requireNonNull(name);
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
        Objects.requireNonNull(reference);

        // no point in adding the reference multiple times
        if (this.contains(reference)) {
            return;
        }
        if (reference.contains(this)) { // avoid recursion, a potential reference should not hold its future "parent" as a reference
            return;
        }
        
        if (referenceList == null) {
            synchronized (this) {
                if (referenceList == null) {
                    referenceList = new CopyOnWriteArrayList<>();
                }
            }
        }
        referenceList.add(reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasReferences() {
        return referenceList != null && !referenceList.isEmpty();
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
        if (referenceList == null) {
            return Collections.emptyIterator();
        } else {
            return referenceList.iterator();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Marker referenceToRemove) {
        if (referenceList != null) {
            return referenceList.remove(referenceToRemove);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Marker other) {
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
                .append(" [ ");
        Iterator<Marker> it = this.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getName());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(" ]");

        return sb.toString();
    }
}
