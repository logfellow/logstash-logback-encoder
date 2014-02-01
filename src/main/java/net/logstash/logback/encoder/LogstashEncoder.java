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
package net.logstash.logback.encoder;

import static org.apache.commons.io.IOUtils.*;

import java.io.IOException;

import net.logstash.logback.LogstashFormatter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogstashEncoder extends EncoderBase<ILoggingEvent> {
    
    private boolean immediateFlush = true;
    
    /**
     * If true, the caller information is included in the logged data.
     * Note: calculating the caller data is an expensive operation.
     */
    private final LogstashFormatter formatter = new LogstashFormatter();
    
    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        
        write(formatter.writeValueAsBytes(event, getContext()), outputStream);
        write(CoreConstants.LINE_SEPARATOR, outputStream);
        
        if (immediateFlush) {
            outputStream.flush();
        }
        
    }
    
    @Override
    public void close() throws IOException {
        write(LINE_SEPARATOR, outputStream);
    }
    
    public static JsonNode parseCustomFields(String customFields) throws JsonParseException, JsonProcessingException, IOException {
        return new ObjectMapper().getFactory().createParser(customFields).readValueAsTree();
    }
    
    public boolean isImmediateFlush() {
        return immediateFlush;
    }
    
    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }
    
    public boolean isIncludeCallerInfo() {
        return formatter.isIncludeCallerInfo();
    }
    
    public void setIncludeCallerInfo(boolean includeCallerInfo) {
        formatter.setIncludeCallerInfo(includeCallerInfo);
    }
    
    public void setCustomFields(String customFields) {
        try {
            formatter.setCustomFields(parseCustomFields(customFields));
        } catch (JsonParseException e) {
            addError("Failed to parse custom fields [" + customFields + "]", e);
        } catch (JsonProcessingException e) {
            addError("Failed to parse custom fields [" + customFields + "]", e);
        } catch (IOException e) {
            addError("Failed to parse custom fields [" + customFields + "]", e);
        }
    }
    
    public JsonNode getCustomFields() {
        return formatter.getCustomFields();
    }
    
}
