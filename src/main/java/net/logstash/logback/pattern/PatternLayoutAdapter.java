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
package net.logstash.logback.pattern;

import java.util.Objects;

import net.logstash.logback.util.DelegatingContext;
import net.logstash.logback.util.DelegatingStatusManager;
import net.logstash.logback.util.NoopStatusManager;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.PostCompileProcessor;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;

/**
 * Adapter around a {@link PatternLayoutBase} to allow writing the pattern into a supplied {@link StringBuilder}
 * instead of returning a String.
 * 
 * The adapter also throws an {@link IllegalArgumentException} when started when the configured pattern is not a
 * valid pattern layout instead of simply emitting an ERROR status.
 * 
 * @author brenuart
 */
public class PatternLayoutAdapter<E> {
    /**
     * The wrapped pattern layout instance
     */
    private final PatternLayoutBase<E> layout;
    
    /**
     * The "head" converter of the pattern.
     * Initialized when the pattern is started.
     * Can stay null after the pattern is started when the pattern is empty or invalid.
     */
    private Converter<E> head;

    /**
     * The Logback context given to the adapted {@link PatternLayoutBase} instrumented to throw an
     * exception when an ERROR status is logged.
     */
    private ErrorCaptureContext errorThrowingContext;
    
    
    public PatternLayoutAdapter(PatternLayoutBase<E> layout) {
        this.layout = Objects.requireNonNull(layout);
        
        if (layout.getContext() != null) {
            setContext(layout.getContext());
        }
    }
    
    /**
     * Set the {@link Context}
     * 
     * @param context the context
     */
    public void setContext(Context context) {
        this.errorThrowingContext = new ErrorCaptureContext(Objects.requireNonNull(context));
        this.layout.setContext(this.errorThrowingContext);
    }
    
    /**
     * Set the layout pattern
     * 
     * @param pattern the layout pattern
     */
    public void setPattern(String pattern) {
        this.layout.setPattern(pattern);
    }
    
    
    /**
     * Start the underlying PatternLayoutBase and throw an {@link IllegalArgumentException} if the
     * configured pattern is not a valid PatternLayout.
     * 
     * @throws IllegalArgumentException thrown when the configured pattern is not a valid PatternLayout
     */
    public void start() throws IllegalArgumentException {
        if (layout.isStarted()) {
            throw new IllegalStateException("Layout is already started");
        }
        if (errorThrowingContext == null) {
            throw new IllegalStateException("Context has not been set");
        }
        
        try {
            errorThrowingContext.setThrowException(true);

            layout.setPostCompileProcessor(new HeadConverterCapture());
            layout.start();
        } finally {
            errorThrowingContext.setThrowException(false);
        }
    }
    
    
    /**
     * Apply the PatternLayout to the <em>event</em> and write result into the supplied {@link StringBuilder}.
     * 
     * @param strBuilder the {@link StringBuilder} to write the result of applying the PatternLayout on the event
     * @param event the event to apply the pattern to
     */
    public void writeTo(StringBuilder strBuilder, E event) {
        Converter<E> c = this.head;
        while (c != null) {
            c.write(strBuilder, event);
            c = c.getNext();
        }
    }
    
    /**
     * Indicate whether the {@link PatternLayoutBase} always generates the same constant value independent of
     * the event it is given.
     * 
     * @return <em>true</em> if the pattern is constant
     */
    public boolean isConstant() {
        if (this.head == null) {
            return true;
        }
        
        Converter<E> current = this.head;
        while (current != null) {
            if (!isConstantConverter(current)) {
                return false;
            }
            current = current.getNext();
        }
        return true;
    }
    
    /**
     * Get the constant value of the pattern or throw an {@link IllegalStateException} if the pattern
     * is not constant.
     * 
     * @return the constant value of the pattern
     * @see #isConstant()
     */
    public String getConstantValue() {
        if (!isConstant()) {
            throw new IllegalStateException("Pattern is not a constant value");
        }
        
        StringBuilder builder = new StringBuilder();
        writeTo(builder, null);
        return builder.toString();
    }
    
    
    private boolean isConstantConverter(Converter<E> converter) {
        return converter instanceof LiteralConverter;
    }
    
    private class HeadConverterCapture implements PostCompileProcessor<E> {
        @Override
        public void process(Context context, Converter<E> head) {
            PatternLayoutAdapter.this.head = head;
        }
    }
    
    
    /**
     * A {@link DelegatingContext} throwing an {@link IllegalArgumentException} exception when an
     * ERROR status is emitted.
     */
    private static class ErrorCaptureContext extends DelegatingContext {
        private ExceptionThrowingStatusManager exceptionThrowingStatusManager;
        
        ErrorCaptureContext(Context delegate) {
            super(delegate);
        }
        
        public void setThrowException(boolean throwException) {
            if (throwException) {
                StatusManager sm = super.getStatusManager();
                if (sm == null) {
                    sm = NoopStatusManager.INSTANCE;
                }
                this.exceptionThrowingStatusManager = new ExceptionThrowingStatusManager(sm);
            } else {
                this.exceptionThrowingStatusManager = null;
            }
        }
        
        @Override
        public StatusManager getStatusManager() {
            if (exceptionThrowingStatusManager != null) {
                return exceptionThrowingStatusManager;
            } else {
                return super.getStatusManager();
            }
        }
    }

    private static class ExceptionThrowingStatusManager extends DelegatingStatusManager {
        ExceptionThrowingStatusManager(StatusManager delegate) {
            super(delegate);
        }
        
        @Override
        public void add(Status status) {
            super.add(status);
            if (status.getLevel() == Status.ERROR) {
                throw new IllegalArgumentException(status.getMessage(), status.getThrowable());
            }
        }
    }
}
