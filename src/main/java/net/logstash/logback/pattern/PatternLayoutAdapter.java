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
package net.logstash.logback.pattern;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.logstash.logback.util.StringUtils;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.PostCompileProcessor;


/**
 * Adapter around a {@link PatternLayoutBase} to allow writing the pattern into a supplied {@link StringBuilder}
 * instead of returning a String.
 * 
 * The adapter also throws an {@link IllegalArgumentException} upon start when the configured pattern is not a
 * valid pattern layout instead of simply emitting an ERROR status.
 * 
 * @author brenuart
 */
public class PatternLayoutAdapter<E> {
    /**
     * Regex pattern matching error place holders inserted by PatternLayout when it
     * fails to parse the pattern
     */
    private static final Pattern ERROR_PATTERN = Pattern.compile("%PARSER_ERROR\\[(.*)\\]");

    /**
     * The wrapped pattern layout instance
     */
    private final PatternLayoutBase<E> layout;
    
    /**
     * The "head" converter of the pattern.
     * Initialized when the pattern is started.
     * Can stay null after the PatternLayout is started when the pattern is empty
     * or a parse error occurred.
     */
    private Converter<E> head;
    private boolean headCaptured;
    
    
    public PatternLayoutAdapter(PatternLayoutBase<E> layout) {
        this.layout = Objects.requireNonNull(layout);
    }
    
    /**
     * Set the {@link Context}
     * 
     * @param context the context
     */
    public void setContext(Context context) {
        this.layout.setContext(context);
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
        
        /*
         * Start layout...
         */
        layout.setPostCompileProcessor(new HeadConverterCapture());
        layout.start();

        /*
         * PostCompileProcessor is not invoked when parser fails to parse the
         * input or when the pattern is empty...
         */
        if (!headCaptured && StringUtils.isEmpty(layout.getPattern())) {
            throw new IllegalArgumentException("Failed to parse PatternLayout. See previous error statuses for more information.");
        }
        
        /*
         * Detect %PARSER_ERROR[]...
         */
        StringBuilder sb = new StringBuilder();
        Converter<E> c = this.head;
        while (c != null) {
            if (isConstantConverter(c)) {
                c.write(sb, null);
                
                Matcher matcher = ERROR_PATTERN.matcher(sb.toString());
                if (matcher.matches()) {
                    String conversionWord = matcher.group(1);
                    throw new IllegalArgumentException("Failed to interpret '%" + conversionWord + "' conversion word. See previous error statuses for more information.");
                }
                
                sb.setLength(0);
            }
            c = c.getNext();
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
     * Indicate whether the {@link PatternLayoutBase} always generates the same constant value regardless of
     * the event it is given.
     * 
     * @return <em>true</em> if the pattern is constant
     */
    public boolean isConstant() {
        if (this.head == null) {
            return true;
        }
        
        Converter<E> c = this.head;
        while (c != null) {
            if (!isConstantConverter(c)) {
                return false;
            }
            c = c.getNext();
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
            PatternLayoutAdapter.this.headCaptured = true;
        }
    }
}
