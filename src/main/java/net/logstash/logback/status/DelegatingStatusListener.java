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
package net.logstash.logback.status;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * A {@link StatusListener} that delegates to another {@link StatusListener}
 */
public class DelegatingStatusListener extends ContextAwareBase implements StatusListener, LifeCycle {

    private StatusListener delegate;

    private volatile boolean started;

    @Override
    public void start() {
        if (delegate == null) {
            addError("delegate must be configured");
            return;
        }
        if (delegate instanceof ContextAware) {
            ((ContextAware) delegate).setContext(context);
        }
        if (delegate instanceof LifeCycle) {
            ((LifeCycle) delegate).start();
        }
        started = true;
    }

    @Override
    public void stop() {
        if (delegate instanceof LifeCycle) {
            ((LifeCycle) delegate).stop();
        }
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void addStatusEvent(Status status) {
        delegate.addStatusEvent(status);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }

    public StatusListener getDelegate() {
        return delegate;
    }

    public void setDelegate(StatusListener delegate) {
        this.delegate = delegate;
    }
}
