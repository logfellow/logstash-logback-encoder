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

import java.io.IOException;
import java.nio.charset.Charset;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 
 * Extend {@link LogstashEncoder} adding encoding support and the new line
 * termination. <br/>
 * The new parameter name are:<br/>
 * 
 * <p>
 * <b>&lt;ENCODING/&gt;</b> Define the used encoding as defined in {@link Charset}. The default value is UTF-8
 * <p/>
 * 
 * <p>
 * <b>&lt;NEWLINE/&gt;</b> Define the new line. Any defined character is used as new line terminator.<br/>
 * The set of pre-defined values are: <br/>
 * 
 * <i>UNIX</i> \n character (default). <br/>
 * 
 * <i> NULL </i> no new line. <br/>
 * 
 * <i> SYSTEM </i> operating system new line. <br/>
 * 
 * <i> WINDOWS </i> the \r\n combination
 * </p>
 * 
 * @author <a href="mailto:mirko.bernardoni@gmail.com">Mirko Bernardoni</a>
 * @since 11 Jun 2014 (creation date)
 */
public class LogstashTcpEncoder extends LogstashEncoder {
    
    private String encoding = "UTF-8";
    
    private String newLine = System.getProperty("line.separator");
    
    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        
        String log = getFormatter().writeEventAsString(event);
        outputStream.write(log.getBytes(encoding));
        if (newLine != null) {
            outputStream.write(newLine.getBytes(Charset.forName(encoding)));
        }
        
        if (isImmediateFlush()) {
            outputStream.flush();
        }
        
    }
    
    @Override
    public void close() throws IOException {
        if (newLine != null) {
            outputStream.write(newLine.getBytes(Charset.forName(encoding)));
        }
    }
    
    /**
     * Define the encoding used for sending the logs
     * 
     * @param encoding
     *            the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }
    
    /**
     * Define the new line. <br/>
     * NULL no new line. <br/>
     * SYSTEM operating system new line. <br/>
     * UNIX is the \n combination (default). <br/>
     * WINDOWS is \r\n combination
     * 
     * @param newLine
     *            the newLine to set
     */
    public void setNewLine(String newLine) {
        if (newLine == null || newLine.isEmpty()) {
            this.newLine = null;
        } else if (newLine.equalsIgnoreCase("SYSTEM")) {
            this.newLine = System.getProperty("line.separator");
        } else if (newLine.equalsIgnoreCase("UNIX")) {
            this.newLine = "\n";
        } else if (newLine.equalsIgnoreCase("WINDOWS")) {
            this.newLine = "\r\n";
        } else {
            this.newLine = newLine;
        }
    }
    
    /**
     * @return the newLine
     */
    public String getNewLine() {
        return newLine;
    }
    
}