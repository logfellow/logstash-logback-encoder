package net.logstash.logback.composite;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.event.KeyValuePair;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class KeyValueAttributeLogstashJsonProvider extends AbstractJsonProvider<ILoggingEvent> {
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        for (KeyValuePair keyValuePair : event.getKeyValuePairs()) {
            String key = keyValuePair.key;
            Object value = keyValuePair.value;
            if (value == null) generator.writeNullField(key);
            else if (value instanceof String) generator.writeStringField(key, (String) value);
            else if (value instanceof Boolean) generator.writeBooleanField(key, (Boolean) value);
            else if (value instanceof Integer) generator.writeNumberField(key, (Integer) value);
            else if (value instanceof Long) generator.writeNumberField(key, (Long) value);
            else if (value instanceof Short) generator.writeNumberField(key, (Short) value);
            else if (value instanceof Float) generator.writeNumberField(key, (Float) value);
            else if (value instanceof Double) generator.writeNumberField(key, (Double) value);
            else if (value instanceof BigDecimal) generator.writeNumberField(key, (BigDecimal) value);
            else if (value instanceof BigInteger) generator.writeNumberField(key, (BigInteger) value);
            else generator.writeObjectField(key, value);
        }
    }
}
