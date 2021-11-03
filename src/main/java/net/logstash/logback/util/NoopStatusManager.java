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
package net.logstash.logback.util;

import java.util.Collections;
import java.util.List;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;

/**
 * Implementation of a {@link StatusManager} that does nothing.
 * 
 * @author brenuart
 */
public class NoopStatusManager implements StatusManager {

    public static final NoopStatusManager INSTANCE = new NoopStatusManager();
    
    @Override
    public void add(Status status) {
        // noop
    }

    @Override
    public List<Status> getCopyOfStatusList() {
        return Collections.emptyList();
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean add(StatusListener listener) {
        return false;
    }

    @Override
    public void remove(StatusListener listener) {
        // noop
    }

    @Override
    public void clear() {
        // noop
    }

    @Override
    public List<StatusListener> getCopyOfStatusListenerList() {
        return Collections.emptyList();
    }
}
