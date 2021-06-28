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
package net.logstash.logback;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.core.spi.LifeCycle;

/**
 * Manages the lifecycle of subcomponents.
 *
 * <p>Specifically:</p>
 *
 * <ul>
 *     <li>Only starts a subcomponent if the subcomponent is not already started.</li>
 *     <li>Only stops a subcomponent if this lifecycle manager started the subcomponent.</li>
 * </ul>
 */
public class LifeCycleManager {

    private final Set<LifeCycle> started = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Starts the given lifecycle component if and only if it is not already started.
     *
     * @param lifeCycle the component to start
     * @return true if this method execution started the component
     */
    public boolean start(LifeCycle lifeCycle) {
        if (lifeCycle.isStarted()) {
            return false;
        }
        lifeCycle.start();
        started.add(lifeCycle);
        return true;
    }

    /**
     * Stops the given lifecycle component if and only if it is currently started,
     * AND was started by this lifecycle manager via {@link #start(LifeCycle)}.
     *
     * @param lifeCycle the component to stop
     * @return true if this method execution stopped the component
     */
    public boolean stop(LifeCycle lifeCycle) {
        if (!lifeCycle.isStarted()) {
            return false;
        }
        if (!started.remove(lifeCycle)) {
            return false;
        }
        lifeCycle.stop();
        return true;
    }

    /**
     * Stops all of the lifecycle components that were started by {@link #start(LifeCycle)}
     * and are currently started.
     *
     * @return the lifecycle components that this method execution stopped
     */
    public Set<LifeCycle> stopAll() {
        Set<LifeCycle> stopped = new HashSet<>();

        for (Iterator<LifeCycle> iterator = started.iterator(); iterator.hasNext();) {
            LifeCycle lifeCycle = iterator.next();
            if (lifeCycle.isStarted()) {
                lifeCycle.stop();
                stopped.add(lifeCycle);
            }
            iterator.remove();
        }
        return Collections.unmodifiableSet(stopped);
    }


}
