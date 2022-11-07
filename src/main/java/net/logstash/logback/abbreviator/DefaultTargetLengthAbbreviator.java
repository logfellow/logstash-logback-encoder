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
package net.logstash.logback.abbreviator;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.ClassNameOnlyAbbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Abbreviates class names in a way similar to the Logback layout feature (see https://logback.qos.ch/manual/layouts.html#logger).
 *
 * <p>The algorithm will shorten the full class name and attempt to reduce its size to a maximum of {@code length} characters.
 * It does so by reducing the package elements to their first letter and gradually expand them starting from the right until
 * the maximum size is reached.
 * Setting the length to zero constitutes an exception and returns the "simple" class name without package.
 *
 * <p>The next table provides examples of the abbreviation algorithm in action.
 *
 * <pre>
 * LENGTH   CLASSNAME                 SHORTENED
 * -------------------------------------------------------------
 * 0        org.company.package.Bar   Bar
 * 5        org.company.package.Bar   o.c.p.Bar
 * 15       org.company.package.Bar   o.c.package.Bar
 * 21       org.company.package.Bar   o.company.package.Bar
 * 24       org.company.package.Bar   org.company.package.Bar
 * </pre>
 *
 * @author brenuart
 */
public class DefaultTargetLengthAbbreviator implements Abbreviator, LifeCycle {

    /**
     * Whether the component is started or not
     */
    private boolean started;

    /**
     * The desired target length, or {@code -1} to disable abbreviation
     */
    private int targetLength = -1;
    
    /**
     * The actual {@link Abbreviator} to delegate
     */
    private Abbreviator delegate;
    
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    @Override
    public void start() {
        if (!isStarted()) {
            this.delegate = createAbbreviator();
            this.started = true;
        }
    }
    
    private Abbreviator createAbbreviator() {
        if (this.targetLength < 0 || this.targetLength == Integer.MAX_VALUE) {
            return NullAbbreviator.INSTANCE;
        }
        
        Abbreviator abbreviator;
        if (this.targetLength == 0) {
            abbreviator = new ClassNameOnlyAbbreviator();
        }
        else {
            abbreviator = new TargetLengthBasedClassNameAbbreviator(this.targetLength);
        }
        return new CachingAbbreviator(abbreviator);
    }
    
    @Override
    public void stop() {
        if (isStarted()) {
            this.started = false;
            this.delegate = null;
        }
    }

    @Override
    public String abbreviate(String in) {
        if (!isStarted()) {
            start();
        }
        assertStarted();
        return delegate.abbreviate(in);
    }
    
    public void setTargetLength(int targetLength) {
        this.targetLength = targetLength;
    }
    
    public int getTargetLength() {
        return targetLength;
    }
    
    protected void assertStarted() {
        if (!isStarted()) {
            throw new IllegalStateException("Component is not started");
        }
    }
}
