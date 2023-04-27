package net.logstash.logback.composite;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import static ch.qos.logback.classic.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

class KeyValueAttributeLogstashJsonProviderTest {
    private KeyValueAttributeLogstashJsonProvider provider = new KeyValueAttributeLogstashJsonProvider();

    private ByteArrayOutputStream resultStream;
    private JsonGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        resultStream = new ByteArrayOutputStream();
        generator = new JsonFactory().createGenerator(resultStream);
    }

    @Test
    void writesSingleKeyValuePairWithStringValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", "some string value"));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":\"some string value\"}");
    }

    @Test
    void writesTwoKeyValuePairsWithStringValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key1", "first string value"));
        event.getKeyValuePairs().add(new KeyValuePair("key2", "second string value"));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key1\":\"first string value\",\"key2\":\"second string value\"}");
    }

    @Test
    void writesSingleKeyValuePairWithBooleanValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", true));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":true}");
    }

    @Test
    void writesSingleKeyValuePairWithIntegerValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", 2147483646));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":2147483646}");
    }

    @Test
    void writesSingleKeyValuePairWithLongValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", 9223372036854775806L));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":9223372036854775806}");
    }

    @Test
    void writesSingleKeyValuePairWithShortValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", 32766));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":32766}");
    }

    @Test
    void writesSingleKeyValuePairWithFloatValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", Float.MAX_VALUE));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":3.4028235E38}");
    }

    @Test
    void writesSingleKeyValuePairWithDoubleValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", Double.MAX_VALUE));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":1.7976931348623157E308}");
    }

    @Test
    void writesSingleKeyValuePairWithBigDecimalValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(
                new KeyValuePair(
                        "key",
                        BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE).add(BigDecimal.valueOf(0.5))
                )
        );

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":9223372036854775808.5}");
    }

    @Test
    void writesSingleKeyValuePairWithBigIntegerValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)));

        write(event);

        assertThat(resultStream.toString())
                .isEqualTo("{\"key\":9223372036854775808}");
    }

    @Test
    void writesSingleKeyValuePairWithRegularClassInstanceThatHasPropertiesAsValue() throws Exception {
        LoggingEvent event = loggingEvent(INFO, "loggerName");
        event.getKeyValuePairs().add(new KeyValuePair("key", new RegularValue(0, "value", "other value")));

        generator.setCodec(new ObjectMapper());

        write(event);

        assertThat(resultStream.toString())
                .describedAs("should write publicly accessible values")
                .isEqualTo("{\"key\":{\"id\":0,\"value\":\"value\"}}");
    }

    private void write(LoggingEvent event) throws Exception {
        generator.writeStartObject();
        provider.writeTo(generator, event);
        generator.writeEndObject();
        generator.flush();
    }

    private static LoggingEvent loggingEvent(Level level, String message) throws Exception {
        LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setLoggerName("someLoggerName");
        loggingEvent.setLevel(level);
        loggingEvent.setMessage(message);
        loggingEvent.setArgumentArray(new Object[]{});
        loggingEvent.setKeyValuePairs(new ArrayList<>(4));

        return loggingEvent;
    }

    private class RegularValue {
        private final int id;
        private final String value;
        private final String nonPublicValue;

        public RegularValue(int id, String value, String nonPublicValue) {
            this.id = id;
            this.value = value;
            this.nonPublicValue = nonPublicValue;
        }

        public int getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }
}
