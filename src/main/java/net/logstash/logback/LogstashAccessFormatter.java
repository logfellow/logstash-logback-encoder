/*
 * Copyright 2013-2023 the original author or authors.
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
package net.logstash.logback;

import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.composite.accessevent.AccessEventCompositeJsonFormatter;
import net.logstash.logback.composite.accessevent.AccessEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.accessevent.AccessEventJsonProviders;
import net.logstash.logback.composite.accessevent.AccessEventPatternJsonProvider;
import net.logstash.logback.composite.accessevent.AccessMessageJsonProvider;
import net.logstash.logback.composite.accessevent.ContentLengthJsonProvider;
import net.logstash.logback.composite.accessevent.ElapsedTimeJsonProvider;
import net.logstash.logback.composite.accessevent.HeaderFilter;
import net.logstash.logback.composite.accessevent.IncludeExcludeHeaderFilter;
import net.logstash.logback.composite.accessevent.MethodJsonProvider;
import net.logstash.logback.composite.accessevent.ProtocolJsonProvider;
import net.logstash.logback.composite.accessevent.RemoteHostJsonProvider;
import net.logstash.logback.composite.accessevent.RemoteUserJsonProvider;
import net.logstash.logback.composite.accessevent.RequestHeadersJsonProvider;
import net.logstash.logback.composite.accessevent.RequestedUriJsonProvider;
import net.logstash.logback.composite.accessevent.RequestedUrlJsonProvider;
import net.logstash.logback.composite.accessevent.ResponseHeadersJsonProvider;
import net.logstash.logback.composite.accessevent.StatusCodeJsonProvider;
import net.logstash.logback.fieldnames.LogstashAccessFieldNames;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.spi.ContextAware;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A {@link AccessEventCompositeJsonFormatter} that contains a common
 * pre-defined set of {@link JsonProvider}s.
 *
 * The included providers are configured via properties on this
 * formatter, rather than configuring the providers directly.
 * This leads to a somewhat simpler configuration definitions.
 *
 * You cannot remove any of the pre-defined providers, but
 * you can add additional providers via {@link #addProvider(JsonProvider)}.
 *
 * If you would like full control over the providers, you
 * should instead use {@link AccessEventCompositeJsonFormatter} directly.
 */
public class LogstashAccessFormatter extends AccessEventCompositeJsonFormatter {

    /**
     * The field names to use when writing the access event fields
     */
    protected LogstashAccessFieldNames fieldNames = new LogstashAccessFieldNames();

    private final AccessEventFormattedTimestampJsonProvider timestampProvider = new AccessEventFormattedTimestampJsonProvider();
    private final LogstashVersionJsonProvider<IAccessEvent> versionProvider = new LogstashVersionJsonProvider<>();
    private final MethodJsonProvider methodProvider = new MethodJsonProvider();
    private final ProtocolJsonProvider protocolProvider = new ProtocolJsonProvider();
    private final StatusCodeJsonProvider statusCodeProvider = new StatusCodeJsonProvider();
    private final RequestedUrlJsonProvider requestedUrlProvider = new RequestedUrlJsonProvider();
    private final RequestedUriJsonProvider requestedUriProvider = new RequestedUriJsonProvider();
    private final RemoteHostJsonProvider remoteHostProvider = new RemoteHostJsonProvider();
    private final RemoteUserJsonProvider remoteUserProvider = new RemoteUserJsonProvider();
    private final ContentLengthJsonProvider contentLengthProvider = new ContentLengthJsonProvider();
    private final ElapsedTimeJsonProvider elapsedTimeProvider = new ElapsedTimeJsonProvider();
    private final RequestHeadersJsonProvider requestHeadersProvider = new RequestHeadersJsonProvider();
    private final ResponseHeadersJsonProvider responseHeadersProvider = new ResponseHeadersJsonProvider();
    private JsonProvider<IAccessEvent> messageProvider;
    private ContextJsonProvider<IAccessEvent> contextProvider = new ContextJsonProvider<>();
    private GlobalCustomFieldsJsonProvider<IAccessEvent> globalCustomFieldsProvider;

    private String messagePattern;

    public LogstashAccessFormatter(ContextAware declaredOrigin) {
        super(declaredOrigin);

        getProviders().addTimestamp(this.timestampProvider);
        getProviders().addVersion(this.versionProvider);
        getProviders().addMethod(this.methodProvider);
        getProviders().addProtocol(this.protocolProvider);
        getProviders().addStatusCode(this.statusCodeProvider);
        getProviders().addRequestedUrl(this.requestedUrlProvider);
        getProviders().addRequestedUri(this.requestedUriProvider);
        getProviders().addRemoteHost(this.remoteHostProvider);
        getProviders().addRemoteUser(this.remoteUserProvider);
        getProviders().addContentLength(this.contentLengthProvider);
        getProviders().addElapsedTime(this.elapsedTimeProvider);
        getProviders().addRequestHeaders(this.requestHeadersProvider);
        getProviders().addResponseHeaders(this.responseHeadersProvider);
        getProviders().addContext(this.contextProvider);
    }

    @Override
    public void start() {
        updateMessageProvider();
        configureProviderFieldNames();
        super.start();
    }

    private void updateMessageProvider() {
        getProviders().removeProvider(this.messageProvider);
        if (this.messagePattern != null) {
            // Build JSON object of the form:
            //
            //    { "fieldName": "messagePattern" }
            //
            String accessEventPattern = new StringBuilder("{\"")
                    .append(this.fieldNames.getMessage())
                    .append("\": \"")
                    .append(escapeJson(this.messagePattern))
                    .append("\"}")
                    .toString();
            
            AccessEventPatternJsonProvider messagePatternProvider = new AccessEventPatternJsonProvider();
            messagePatternProvider.setPattern(accessEventPattern);
            this.messageProvider = messagePatternProvider;

        } else {
            AccessMessageJsonProvider accessMessageJsonProvider = new AccessMessageJsonProvider();
            accessMessageJsonProvider.setTimeZone(this.timestampProvider.getTimeZone());
            this.messageProvider = accessMessageJsonProvider;
        }
        getProviders().addProvider(this.messageProvider);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void configureProviderFieldNames() {
        for (JsonProvider<IAccessEvent> provider : getProviders().getProviders()) {
            if (provider instanceof FieldNamesAware) {
                ((FieldNamesAware) provider).setFieldNames(fieldNames);
            }
        }
    }

    public void addProvider(JsonProvider<IAccessEvent> provider) {
        getProviders().addProvider(provider);
    }

    @Override
    public AccessEventJsonProviders getProviders() {
        return (AccessEventJsonProviders) super.getProviders();
    }

    public LogstashAccessFieldNames getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(LogstashAccessFieldNames fieldNames) {
        this.fieldNames = fieldNames;
    }

    public String getTimeZone() {
        return timestampProvider.getTimeZone();
    }
    public void setTimeZone(String timeZoneId) {
        this.timestampProvider.setTimeZone(timeZoneId);
    }
    public String getTimestampPattern() {
        return timestampProvider.getPattern();
    }
    public void setTimestampPattern(String pattern) {
        timestampProvider.setPattern(pattern);
    }

    public String getCustomFieldsAsString() {
        return globalCustomFieldsProvider == null
                ? null
                : globalCustomFieldsProvider.getCustomFields();
    }

    public void setCustomFieldsFromString(String customFields) {
        if (customFields == null || customFields.length() == 0) {
            getProviders().removeProvider(globalCustomFieldsProvider);
            globalCustomFieldsProvider = null;
        } else {
            if (globalCustomFieldsProvider == null) {
                globalCustomFieldsProvider = new GlobalCustomFieldsJsonProvider<>();
                getProviders().addGlobalCustomFields(globalCustomFieldsProvider);
            }
            globalCustomFieldsProvider.setCustomFields(customFields);
        }
    }

    public void setCustomFields(JsonNode customFields) {
        if (customFields == null) {
            getProviders().removeProvider(globalCustomFieldsProvider);
            globalCustomFieldsProvider = null;
        } else {
            if (globalCustomFieldsProvider == null) {
                globalCustomFieldsProvider = new GlobalCustomFieldsJsonProvider<>();
                getProviders().addGlobalCustomFields(globalCustomFieldsProvider);
            }
            globalCustomFieldsProvider.setCustomFieldsNode(customFields);
        }
    }

    public JsonNode getCustomFields() {
        return globalCustomFieldsProvider == null
                ? null
                : globalCustomFieldsProvider.getCustomFieldsNode();
    }

    public boolean getLowerCaseHeaderNames() {
        return this.requestHeadersProvider.getLowerCaseHeaderNames();
    }

    /**
     * When true, names of headers will be written to JSON output in lower case.
     * 
     * @param lowerCaseHeaderNames When true, names of headers will be written to JSON output in lower case.
     */
    public void setLowerCaseHeaderNames(boolean lowerCaseHeaderNames) {
        this.requestHeadersProvider.setLowerCaseHeaderNames(lowerCaseHeaderNames);
        this.responseHeadersProvider.setLowerCaseHeaderNames(lowerCaseHeaderNames);
    }

    public HeaderFilter getRequestHeaderFilter() {
        return this.requestHeadersProvider.getFilter();
    }

    @DefaultClass(IncludeExcludeHeaderFilter.class)
    public void setRequestHeaderFilter(HeaderFilter filter) {
        this.requestHeadersProvider.setFilter(filter);
    }

    public HeaderFilter getResponseHeaderFilter() {
        return this.responseHeadersProvider.getFilter();
    }

    @DefaultClass(IncludeExcludeHeaderFilter.class)
    public void setResponseHeaderFilter(HeaderFilter filter) {
        this.responseHeadersProvider.setFilter(filter);
    }

    public boolean isIncludeContext() {
        return contextProvider != null;
    }

    public void setIncludeContext(boolean includeContext) {
        if (isIncludeContext() != includeContext) {
            getProviders().removeProvider(contextProvider);
            if (includeContext) {
                contextProvider = new ContextJsonProvider<>();
                getProviders().addContext(contextProvider);
            } else {
                contextProvider = null;
            }
        }
    }

    public String getMessagePattern() {
        return messagePattern;
    }

    public void setMessagePattern(String messagePattern) {
        this.messagePattern = messagePattern;
    }

    public String getVersion() {
        return this.versionProvider.getVersion();
    }
    public void setVersion(String version) {
        this.versionProvider.setVersion(version);
    }

    public boolean isWriteVersionAsInteger() {
        return this.versionProvider.isWriteAsInteger();
    }
    public void setWriteVersionAsInteger(boolean writeVersionAsInteger) {
        this.versionProvider.setWriteAsInteger(writeVersionAsInteger);
    }


    @Override
    public void setProviders(JsonProviders<IAccessEvent> jsonProviders) {
        if (super.getProviders() != null && !super.getProviders().getProviders().isEmpty()) {
            addError("Unable to set providers when using predefined composites.");
        } else {
            super.setProviders(jsonProviders);
        }
    }
    
    private static String escapeJson(String str) {
        return str.replace("\"", "\\\"");
    }
}
