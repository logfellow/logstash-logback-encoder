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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.StatusManager;

/**
 * Logback {@link Context} implementation that delegate calls to another wrapped instance.
 * 
 * @author brenuart
 */
public class DelegatingContext implements Context {

    private final Context delegate;
    
    public DelegatingContext(Context delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    public StatusManager getStatusManager() {
        return delegate.getStatusManager();
    }

    public Object getObject(String key) {
        return delegate.getObject(key);
    }

    public void putObject(String key, Object value) {
        delegate.putObject(key, value);
    }

    public String getProperty(String key) {
        return delegate.getProperty(key);
    }

    public void putProperty(String key, String value) {
        delegate.putProperty(key, value);
    }

    public Map<String, String> getCopyOfPropertyMap() {
        return delegate.getCopyOfPropertyMap();
    }

    public String getName() {
        return delegate.getName();
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public long getBirthTime() {
        return delegate.getBirthTime();
    }

    public Object getConfigurationLock() {
        return delegate.getConfigurationLock();
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return delegate.getScheduledExecutorService();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public ExecutorService getExecutorService() {
        return delegate.getExecutorService();
    }

    public void register(LifeCycle component) {
        delegate.register(component);
    }

    public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        delegate.addScheduledFuture(scheduledFuture);
    }
}
