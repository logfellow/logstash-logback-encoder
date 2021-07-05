/**
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

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Marker;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A {@link Marker} that is known and understood by the logstash logback encoder.
 * <p>
 * In particular these markers are used to write data into the logstash json event via {@link #writeTo(JsonGenerator)}.
 */
@SuppressWarnings("serial")
public abstract class LogstashMarker extends LogstashBasicMarker implements Iterable<Marker> {

    public static final String MARKER_NAME_PREFIX = "LS_";

    public LogstashMarker(String name) {
        super(name);
    }

    /**
     * Adds the given marker as a reference, and returns this marker.
     * <p>
     * This can be used to chain markers together fluently on a log line. For example:
     *
     * <pre>
     * {@code
     * import static net.logstash.logback.marker.Markers.*
     *
     * logger.info(append("name1", "value1).and(append("name2", "value2")), "log message");
     * }
     * </pre>
     *
     * @param <T> subtype of LogstashMarker
     * @param reference The marker to add
     * @return A marker with this marker and the given marker
     */
    @SuppressWarnings("unchecked")
    public <T extends LogstashMarker> T and(Marker reference) {
        add(reference);
        return (T) this;
    }

    /**
     * @param <T> subtype of LogstashMarker
     * @param reference The marker to add
     * @deprecated Use {@link #and(Marker)} instead
     * @see #and(Marker)
     * @return A marker with this marker and the given marker
     */
    @Deprecated
    public <T extends LogstashMarker> T with(Marker reference) {
        return and(reference);
    }

    /**
     * Writes the data associated with this marker to the given {@link JsonGenerator}.
     *
     * @param generator the generator to which to write the output of this marker.
     * @throws IOException if there was an error writing to the generator
     */
    public abstract void writeTo(JsonGenerator generator) throws IOException;

    @Override
    public synchronized void add(Marker reference) {
        if (reference instanceof EmptyLogstashMarker) {
            for (Marker m : (EmptyLogstashMarker) reference) {
                add(m);
            }
        } else {
            super.add(reference);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + this.getReferences().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LogstashMarker)) {
            return false;
        }

        LogstashMarker other = (LogstashMarker) obj;
        return Objects.equals(this.getReferences(), other.getReferences());
    }

    /**
     * Returns a String in the form of
     * <pre>
     *     self, reference1, reference2, ...
     * </pre>
     *
     * <p>Where <code>self</code> is the value returned by {@link #toStringSelf()},
     * and <code>reference*</code> are the <code>toString()</code> values of any references.</p>
     *
     * <p>It is recommended that subclasses only override {@link #toStringSelf()},
     * so that references are automatically included in the value returned from {@link #toString()}.</p>
     *
     * @return a string representation of the object, which includes references
     */
    @Override
    public String toString() {
        String self = toStringSelf();
        if (!hasReferences()) {
            return self;
        }

        StringBuilder sb = new StringBuilder(self);
        boolean appendSeparator = !self.isEmpty();
        for (Marker marker : this) {
            if (appendSeparator) {
                sb.append(", ");
            }
            String referenceToString = marker.toString();
            sb.append(referenceToString);
            appendSeparator = !referenceToString.isEmpty();
        }

        return sb.toString();
    }

    /**
     * Returns a string representation of this object, without including any references.
     *
     * <p>Subclasses should override {@link #toStringSelf()} instead of {@link #toString()},
     * since {@link #toString()} will automatically include the {@link #toStringSelf()} and references.</p>
     *
     * @return a string representation of this object, without including any references.
     */
    protected String toStringSelf() {
        return getName();
    }


}
