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

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;
import net.logstash.logback.layout.AbstractJsonPatternLayout;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.apache.commons.io.IOUtils.write;

/**
 * A simple encoder that just wraps an AbstractJsonPatternLayout.
 * Subclasses must implement <code>createLayout</code> method so it returns layout valid for a specified event class.
 *
 * @param <E> - type of the event (ILoggingEvent, IAccessEvent)
 *
 * @author <a href="mailto:dimas@dataart.com">Dmitry Andrianov</a>
 */
public abstract class AbstractJsonPatternLayoutEncoder<E> extends EncoderBase<E> {

    private Charset charset;

    private boolean immediateFlush = true;

    private AbstractJsonPatternLayout<E> layout;

    public AbstractJsonPatternLayoutEncoder() {
        layout = createLayout();
    }

    @Override
    public void doEncode(E event) throws IOException {

        String txt = layout.doLayout(event);
        if (txt == null) {
            return;
        }

        outputStream.write(convertToBytes(txt));

        write(CoreConstants.LINE_SEPARATOR, outputStream);

        if (immediateFlush) {
            outputStream.flush();
        }
    }

    @Override
    public void close() throws IOException {
    }

    private byte[] convertToBytes(String s) {
        if (charset == null) {
            return s.getBytes();
        } else {
            return s.getBytes(charset);
        }
    }

    @Override
    public void start() {
        layout.setContext(context);
        layout.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        layout.stop();
    }

    protected abstract AbstractJsonPatternLayout<E> createLayout();

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setPattern(String pattern) {
        layout.setPattern(pattern);
    }
}
