/*
 * Copyright 2013-2022 the original author or authors.
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
package net.logstash.logback.status;

import ch.qos.logback.core.status.OnPrintStreamStatusListenerBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * A {@link DelegatingStatusListener} that filters {@link Status} events based on their level
 * (e.g. {@link Status#INFO}, {@link Status#WARN}, {@link Status#ERROR})
 * before forwarding the status event to the delegate.
 *
 * <p>Only those {@link Status} events whose level is greater than or equal to
 * the configured level will be forwarded to the delegate {@link StatusListener}.
 * For example if level is {@link Status#WARN}, then status' whose level is
 * {@link Status#WARN} or {@link Status#ERROR} will be forwarded.</p>
 */
public class LevelFilteringStatusListener extends DelegatingStatusListener {

    private int statusLevel = Status.INFO;

    @Override
    public void addStatusEvent(Status status) {
        if (status.getEffectiveLevel() >= statusLevel) {
            super.addStatusEvent(status);
        }
    }

    public void setLevel(String level) {
        if (level.trim().equalsIgnoreCase("INFO")) {
            this.statusLevel = Status.INFO;
        } else if (level.trim().equalsIgnoreCase("WARN")) {
            this.statusLevel = Status.WARN;
        } else if (level.trim().equalsIgnoreCase("ERROR")) {
            this.statusLevel = Status.ERROR;
        } else {
            throw new IllegalArgumentException(String.format("Unknown level: %s. Must be one of INFO, WARN, or ERROR.", level));
        }
    }

    public int getLevelValue() {
        return statusLevel;
    }

    public void setLevelValue(int levelValue) {
        if (levelValue < Status.INFO || levelValue > Status.ERROR) {
            throw new IllegalArgumentException(String.format("Unknown level: %d. Must be between %d and %d, inclusive", levelValue, Status.INFO, Status.ERROR));
        }
        statusLevel = levelValue;
    }

    @Override
    public void setDelegate(StatusListener delegate) {
        super.setDelegate(delegate);
        if (delegate instanceof OnPrintStreamStatusListenerBase) {
            /*
             * Set retrospective to 0, since we don't have a hook into
             * filtering the retrospective status messages from the status manager.
             */
            ((OnPrintStreamStatusListenerBase) delegate).setRetrospective(0L);
        }
    }
}
