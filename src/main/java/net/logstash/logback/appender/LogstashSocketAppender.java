/**
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
package net.logstash.logback.appender;

import java.util.List;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.encoder.SeparatorParser;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import net.logstash.logback.layout.LogstashLayout;

/**
 * A {@link LogstashUdpSocketAppender} that uses a {@link LogstashLayout}
 * and allows direct configuration of the {@link LogstashLayout} properties,
 * rather than configuring a {@link LogstashLayout} separately.
 *
 * @deprecated Prefer using {@link LogstashUdpSocketAppender} with a {@link LogstashLayout} instead.
 */
@Deprecated
public class LogstashSocketAppender extends LogstashUdpSocketAppender {

    public LogstashSocketAppender() {
        super.setLayout(new LogstashLayout());
    }

    @Override
    public LogstashLayout getLayout() {
        return (LogstashLayout) super.getLayout();
    }

    @Override
    public void setLayout(Layout<ILoggingEvent> layout) {
        addWarn("The layout of a LogstashSocketAppender cannot be set directly. Use LogstashUdpSocketAppender if you would like to use a custom layout when sending over UDP.");
    }

    public void addProvider(JsonProvider<ILoggingEvent> provider) {
        getLayout().addProvider(provider);
    }
    
    public void setCustomFields(String customFields) {
        getLayout().setCustomFields(customFields);
    }
    
    public String getCustomFields() {
        return getLayout().getCustomFields().toString();
    }
    
    public boolean isIncludeCallerData() {
        return getLayout().isIncludeCallerData();
    }
    
    public void setIncludeCallerData(boolean includeCallerData) {
        getLayout().setIncludeCallerData(includeCallerData);
    }
    
    /**
     * @deprecated use {@link #isIncludeCallerData()} (to use the same name that logback uses) 
     */
    @Deprecated
    public boolean isIncludeCallerInfo() {
        return getLayout().isIncludeCallerInfo();
    }
    
    /**
     * @deprecated use {@link #setIncludeCallerData(boolean)} (to use the same name that logback uses)
     */
    @Deprecated
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        getLayout().setIncludeCallerInfo(includeCallerInfo);
    }
    
    public LogstashFieldNames getFieldNames() {
        return getLayout().getFieldNames();
    }
    
    public void setFieldNames(LogstashFieldNames fieldNames) {
        getLayout().setFieldNames(fieldNames);
    }
    
    public boolean isIncludeMdc() {
        return getLayout().isIncludeMdc();
    }
    
    public void setIncludeMdc(boolean includeMdc) {
        getLayout().setIncludeMdc(includeMdc);
    }
    
    public List<String> getIncludeMdcKeyNames() {
        return getLayout().getIncludeMdcKeyNames();
    }

    public void addIncludeMdcKeyName(String includedMdcKeyName) {
        getLayout().addIncludeMdcKeyName(includedMdcKeyName);
    }

    public void setIncludeMdcKeyNames(List<String> includeMdcKeyNames) {
        getLayout().setIncludeMdcKeyNames(includeMdcKeyNames);
    }

    public List<String> getExcludeMdcKeyNames() {
        return getLayout().getExcludeMdcKeyNames();
    }

    public void addExcludeMdcKeyName(String excludedMdcKeyName) {
        getLayout().addExcludeMdcKeyName(excludedMdcKeyName);
    }

    public void setExcludeMdcKeyNames(List<String> excludeMdcKeyNames) {
        getLayout().setExcludeMdcKeyNames(excludeMdcKeyNames);
    }
    
    public boolean isIncludeContext() {
        return getLayout().isIncludeContext();
    }
    
    public void setIncludeContext(boolean includeContext) {
        getLayout().setIncludeContext(includeContext);
    }

    public boolean isIncludeStructuredArguments() {
        return getLayout().isIncludeStructuredArguments();
    }

    public void setIncludeStructuredArguments(boolean includeStructuredArguments) {
        getLayout().setIncludeStructuredArguments(includeStructuredArguments);
    }
    
    public boolean isIncludeNonStructuredArguments() {
        return getLayout().isIncludeNonStructuredArguments();
    }

    public void setIncludeNonStructuredArguments(boolean includeNonStructuredArguments) {
        getLayout().setIncludeNonStructuredArguments(includeNonStructuredArguments);
    }
    
    public String getNonStructuredArgumentsFieldPrefix() {
        return getLayout().getNonStructuredArgumentsFieldPrefix();
    }

    public void setNonStructuredArgumentsFieldPrefix(String nonStructuredArgumentsFieldPrefix) {
        getLayout().setNonStructuredArgumentsFieldPrefix(nonStructuredArgumentsFieldPrefix);
    }

    public int getShortenedLoggerNameLength() {
        return getLayout().getShortenedLoggerNameLength();
    }

    public void setShortenedLoggerNameLength(int length) {
        getLayout().setShortenedLoggerNameLength(length);
    }
    
    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return getLayout().getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        getLayout().setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return getLayout().getJsonGeneratorDecorator();
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        getLayout().setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        getLayout().setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    public String getTimeZone() {
        return getLayout().getTimeZone();
    }

    public void setTimeZone(String timeZoneId) {
        getLayout().setTimeZone(timeZoneId);
    }

    public ThrowableHandlingConverter getThrowableConverter() {
        return getLayout().getThrowableConverter();
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        getLayout().setThrowableConverter(throwableConverter);
    }
    
    public Layout<ILoggingEvent> getPrefix() {
        return getLayout().getPrefix();
    }
    public void setPrefix(Layout<ILoggingEvent> prefix) {
        getLayout().setPrefix(prefix);
    }

    public Layout<ILoggingEvent> getSuffix() {
        return getLayout().getSuffix();
    }
    public void setSuffix(Layout<ILoggingEvent> suffix) {
        getLayout().setSuffix(suffix);
    }

    public String getLineSeparator() {
        return getLayout().getLineSeparator();
    }
    public void setLineSeparator(String lineSeparator) {
        getLayout().setLineSeparator(lineSeparator);
    }


}
