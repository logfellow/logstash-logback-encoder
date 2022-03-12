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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.pattern.PropertyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;

/**
 * Variation of the Logback {@link PropertyConverter} with the option to specify a default
 * value to use when the property does not exist instead of returning {@code null} as does
 * the original Logback implementation.
 * 
 * <p>The default value is optional and can be specified using the <code>:-</code> operator as
 * in Bash shell. For example, assuming the property "foo" is not defined, <code>%property{foo:-bar}</code>
 * will return <code>bar</code>.
 * If no optional value is declared, the converter returns an empty string instead of {@code null}
 * if the property is not defined.
 * 
 * <p>The property resolution mechanism is the same as the Logback implementation. The property is
 * first looked up in the context associated with the logging event. If not found, the property is
 * searched in the System environment.
 * 
 * 
 * @author brenuart
 */
public class EnhancedPropertyConverter extends ClassicConverter {
    /**
     * Regex pattern used to extract the optional default value from the key name (split
     * at the first :-).
     */
    private static final Pattern PATTERN = Pattern.compile("(.+?):-(.*)");
    
    /**
     * The property name.
     */
    private String propertyName;
    
    /**
     * The default value to use when the property is not defined.
     */
    private String defaultValue = "";
    
    public void start() {
        String optStr = getFirstOption();
        if (optStr != null) {
            propertyName = optStr;
            super.start();
        }
        if (propertyName == null) {
            throw new IllegalStateException("Property name is not specified");
        }
        
        Matcher matcher = PATTERN.matcher(propertyName);
        if (matcher.matches()) {
            propertyName = matcher.group(1);
            defaultValue = matcher.group(2);
        }
    }

    @Override
    public String convert(ILoggingEvent event) {
        LoggerContextVO lcvo = event.getLoggerContextVO();
        Map<String, String> map = lcvo.getPropertyMap();
        String val = map.get(propertyName);
        
        if (val == null) {
            val = System.getProperty(propertyName);
        }

        if (val == null) {
            val = defaultValue;
        }
        
        return val;
    }
}
