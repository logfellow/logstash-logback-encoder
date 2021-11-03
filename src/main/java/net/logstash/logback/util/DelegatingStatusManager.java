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

import java.util.List;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;

/**
 * {@link StatusManager} implementation that delegates calls to another {@link StatusManager}.
 * 
 * @author brenuart
 */
public class DelegatingStatusManager implements StatusManager {

    private final StatusManager delegate;
    
    public DelegatingStatusManager(StatusManager delegate) {
        this.delegate = delegate;
    }

    public void add(Status status) {
        delegate.add(status);
    }

    public List<Status> getCopyOfStatusList() {
        return delegate.getCopyOfStatusList();
    }

    public int getCount() {
        return delegate.getCount();
    }

    public boolean add(StatusListener listener) {
        return delegate.add(listener);
    }

    public void remove(StatusListener listener) {
        delegate.remove(listener);
    }

    public void clear() {
        delegate.clear();
    }

    public List<StatusListener> getCopyOfStatusListenerList() {
        return delegate.getCopyOfStatusListenerList();
    }
}
