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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.PostCompileProcessor;

/**
 * Adapter around a {@link PatternLayoutBase} to allow writing the pattern into a supplied {@link StringBuilder}
 * instead of returning a String.
 * 
 * @author brenuart
 */
public class PatternLayoutAdapter<E> {
    /**
     * The "head" converter of the pattern.
     * Initialized when the pattern is started.
     * Can stay null after the pattern is started when the pattern is empty or invalid.
     */
    private Converter<E> head;

    public PatternLayoutAdapter(PatternLayoutBase<E> layout) {
        layout.setPostCompileProcessor(new HeadConverterCapture());
        layout.start();
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
}
