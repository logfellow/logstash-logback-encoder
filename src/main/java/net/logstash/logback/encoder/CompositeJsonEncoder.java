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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import net.logstash.logback.LifeCycleManager;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public abstract class CompositeJsonEncoder<Event extends DeferredProcessingAware>
        extends EncoderBase<Event> {

    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * The minimum size of the byte array buffer used when
     * encoding events in logback versions greater than or equal to 1.2.0.
     *
     * The actual buffer size will be the {@link #minBufferSize}
     * plus the prefix, suffix, and line separators sizes.
     */
    private int minBufferSize = 1024;

    private Encoder<Event> prefix;
    private Encoder<Event> suffix;

    private final CompositeJsonFormatter<Event> formatter;

    private String lineSeparator = System.lineSeparator();

    private byte[] lineSeparatorBytes;

    private Charset charset;

    /**
     * Manages the lifecycle of subcomponents
     */
    private final LifeCycleManager lifecycleManager = new LifeCycleManager();

    public CompositeJsonEncoder() {
        super();
        this.formatter = createFormatter();
    }

    protected abstract CompositeJsonFormatter<Event> createFormatter();

    @Override
    public byte[] encode(Event event) {
        if (!isStarted()) {
            throw new IllegalStateException("Encoder is not started");
        }
        byte[] prefixBytes = doEncodeWrappedToBytes(prefix, event);
        byte[] suffixBytes = doEncodeWrappedToBytes(suffix, event);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
                minBufferSize
                + (prefixBytes == null ? 0 : prefixBytes.length)
                + (suffixBytes == null ? 0 : suffixBytes.length)
                + lineSeparatorBytes.length);
        try {
            if (prefixBytes != null) {
                outputStream.write(prefixBytes);
            }

            formatter.writeEventToOutputStream(event, outputStream);

            if (suffixBytes != null) {
                outputStream.write(suffixBytes);
            }

            outputStream.write(lineSeparatorBytes);

            return outputStream.toByteArray();
        } catch (IOException e) {
            addWarn("Error encountered while encoding log event. "
                    + "Event: " + event, e);
            return EMPTY_BYTES;
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private byte[] doEncodeWrappedToBytes(Encoder<Event> wrapped, Event event) {
        if (wrapped != null) {
            return wrapped.encode(event);
        }
        return EMPTY_BYTES;
    }

    @Override
    public void start() {
        super.start();
        formatter.setContext(getContext());
        lifecycleManager.start(formatter);
        charset = Charset.forName(formatter.getEncoding());
        lineSeparatorBytes = this.lineSeparator == null
                ? EMPTY_BYTES
                : this.lineSeparator.getBytes(charset);
        startWrapped(prefix);
        startWrapped(suffix);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void startWrapped(Encoder<Event> wrapped) {
        if (wrapped == null) {
            return;
        }
        if (wrapped instanceof LayoutWrappingEncoder) {
            /*
             * Convenience hack to ensure the same charset is used in most cases.
             *
             * The charset for other encoders must be configured
             * on the wrapped encoder configuration.
             */
            LayoutWrappingEncoder<Event> layoutWrappedEncoder = (LayoutWrappingEncoder<Event>) wrapped;
            layoutWrappedEncoder.setCharset(charset);

            if (layoutWrappedEncoder.getLayout() instanceof PatternLayoutBase) {
                /*
                 * Don't ensure exception output (for ILoggingEvents)
                 * or line separation (for IAccessEvents)
                 */
                PatternLayoutBase layout = (PatternLayoutBase) layoutWrappedEncoder.getLayout();
                layout.setPostCompileProcessor(null);
                /*
                 * The pattern will be re-parsed during start.
                 * Needed so that the pattern is re-parsed without
                 * the postCompileProcessor.
                 */
                layout.start();
            }
        }
        lifecycleManager.start(wrapped);
    }

    @Override
    public void stop() {
        super.stop();
        lifecycleManager.stop(formatter);
        stopWrapped(prefix);
        stopWrapped(suffix);
    }

    private void stopWrapped(Encoder<Event> wrapped) {
        if (wrapped == null) {
            return;
        }
        if (wrapped instanceof LayoutWrappingEncoder) {
            LayoutWrappingEncoder<Event> layoutWrappedEncoder = (LayoutWrappingEncoder<Event>) wrapped;
            if (layoutWrappedEncoder.getLayout() instanceof PatternLayoutBase) {
                PatternLayoutBase layout = (PatternLayoutBase) layoutWrappedEncoder.getLayout();
                lifecycleManager.stop(layout);
            }
        }
        lifecycleManager.stop(wrapped);
    }

    @Override
    public byte[] headerBytes() {
        return EMPTY_BYTES;
    }

    @Override
    public byte[] footerBytes() {
        return EMPTY_BYTES;
    }

    public JsonProviders<Event> getProviders() {
        return formatter.getProviders();
    }

    public void setProviders(JsonProviders<Event> jsonProviders) {
        formatter.setProviders(jsonProviders);
    }

    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return formatter.getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        formatter.setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return formatter.getJsonGeneratorDecorator();
    }

    public String getEncoding() {
        return formatter.getEncoding();
    }

    /**
     * The character encoding to use (default = "<tt>UTF-8</tt>").
     * Must an encoding supported by {@link com.fasterxml.jackson.core.JsonEncoding}
     */
    public void setEncoding(String encodingName) {
        formatter.setEncoding(encodingName);
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        formatter.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        formatter.setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets which lineSeparator to use between events.
     * <p>
     *
     * The following values have special meaning:
     * <ul>
     * <li><tt>null</tt> or empty string = no new line.</li>
     * <li>"<tt>SYSTEM</tt>" = operating system new line (default).</li>
     * <li>"<tt>UNIX</tt>" = unix line ending (\n).</li>
     * <li>"<tt>WINDOWS</tt>" = windows line ending (\r\n).</li>
     * </ul>
     * <p>
     * Any other value will be used as given as the lineSeparator.
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = SeparatorParser.parseSeparator(lineSeparator);
    }

    public int getMinBufferSize() {
        return minBufferSize;
    }
    /**
     * Sets the minimum size of the byte array buffer used when
     * encoding events in logback versions greater than or equal to 1.2.0.
     *
     * The actual buffer size will be the {@link #minBufferSize}
     * plus the prefix, suffix, and line separators sizes.
     */
    public void setMinBufferSize(int minBufferSize) {
        this.minBufferSize = minBufferSize;
    }

    protected CompositeJsonFormatter<Event> getFormatter() {
        return formatter;
    }

    public Encoder<Event> getPrefix() {
        return prefix;
    }
    public void setPrefix(Encoder<Event> prefix) {
        this.prefix = prefix;
    }

    public Encoder<Event> getSuffix() {
        return suffix;
    }
    public void setSuffix(Encoder<Event> suffix) {
        this.suffix = suffix;
    }

}
