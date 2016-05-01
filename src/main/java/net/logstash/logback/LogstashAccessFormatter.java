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
package net.logstash.logback;

import com.fasterxml.jackson.databind.JsonNode;

import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.LogstashVersionJsonProvider;
import net.logstash.logback.composite.accessevent.AccessEventCompositeJsonFormatter;
import net.logstash.logback.composite.accessevent.AccessEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.accessevent.AccessEventJsonProviders;
import net.logstash.logback.composite.accessevent.AccessMessageJsonProvider;
import net.logstash.logback.composite.accessevent.ContentLengthJsonProvider;
import net.logstash.logback.composite.accessevent.ElapsedTimeJsonProvider;
import net.logstash.logback.composite.accessevent.HostnameJsonProvider;
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
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.spi.ContextAware;

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
    private final LogstashVersionJsonProvider<IAccessEvent> versionProvider = new LogstashVersionJsonProvider<IAccessEvent>();
    private final AccessMessageJsonProvider messageProvider = new AccessMessageJsonProvider();
    private final MethodJsonProvider methodProvider = new MethodJsonProvider();
    private final ProtocolJsonProvider protocolProvider = new ProtocolJsonProvider();
    private final StatusCodeJsonProvider statusCodeProvider = new StatusCodeJsonProvider();
    private final RequestedUrlJsonProvider requestedUrlProvider = new RequestedUrlJsonProvider();
    private final RequestedUriJsonProvider requestedUriProvider = new RequestedUriJsonProvider();
    private final RemoteHostJsonProvider remoteHostProvider = new RemoteHostJsonProvider();
    private final HostnameJsonProvider hostnameProvider = new HostnameJsonProvider();
    private final RemoteUserJsonProvider remoteUserProvider = new RemoteUserJsonProvider();
    private final ContentLengthJsonProvider contentLengthProvider = new ContentLengthJsonProvider();
    private final ElapsedTimeJsonProvider elapsedTimeProvider = new ElapsedTimeJsonProvider();
    private final RequestHeadersJsonProvider requestHeadersProvider = new RequestHeadersJsonProvider();
    private final ResponseHeadersJsonProvider responseHeadersProvider = new ResponseHeadersJsonProvider();
    private final ContextJsonProvider<IAccessEvent> contextProvider = new ContextJsonProvider<IAccessEvent>();
    private GlobalCustomFieldsJsonProvider<IAccessEvent> globalCustomFieldsProvider;
    
    public LogstashAccessFormatter(ContextAware declaredOrigin) {
        super(declaredOrigin);
        
        getProviders().addTimestamp(this.timestampProvider);
        getProviders().addVersion(this.versionProvider);
        getProviders().addAccessMessage(this.messageProvider);
        getProviders().addMethod(this.methodProvider);
        getProviders().addProtocol(this.protocolProvider);
        getProviders().addStatusCode(this.statusCodeProvider);
        getProviders().addRequestedUrl(this.requestedUrlProvider);
        getProviders().addRequestedUri(this.requestedUriProvider);
        getProviders().addRemoteHost(this.remoteHostProvider);
        getProviders().addHostname(this.hostnameProvider);
        getProviders().addRemoteUser(this.remoteUserProvider);
        getProviders().addContentLength(this.contentLengthProvider);
        getProviders().addElapsedTime(this.elapsedTimeProvider);
        getProviders().addRequestHeaders(this.requestHeadersProvider);
        getProviders().addResponseHeaders(this.responseHeadersProvider);
        getProviders().addContext(this.contextProvider);
    }
    
    @Override
    public void start() {
        configureProviderFieldNames();
        super.start();
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
        this.messageProvider.setTimeZone(timeZoneId);
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
                getProviders().addGlobalCustomFields(globalCustomFieldsProvider = new GlobalCustomFieldsJsonProvider<IAccessEvent>());
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
                getProviders().addGlobalCustomFields(globalCustomFieldsProvider = new GlobalCustomFieldsJsonProvider<IAccessEvent>());
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
     * When true, names of headers will be written to JSON output in lowercase. 
     */
    public void setLowerCaseHeaderNames(boolean lowerCaseHeaderNames) {
        this.requestHeadersProvider.setLowerCaseHeaderNames(lowerCaseHeaderNames);
        this.responseHeadersProvider.setLowerCaseHeaderNames(lowerCaseHeaderNames);
    }
    
    @Override
    public void setProviders(JsonProviders<IAccessEvent> jsonProviders) {
        if (super.getProviders() != null && !super.getProviders().getProviders().isEmpty()) {
            addError("Unable to set providers when using predefined composites.");
        } else {
            super.setProviders(jsonProviders);
        }
    }
}
