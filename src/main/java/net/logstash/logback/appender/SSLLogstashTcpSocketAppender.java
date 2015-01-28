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

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class extends {@link net.logstash.logback.appender.LogstashTcpSocketAppender} 
 * to set the {@link SocketFactory} to the {@link javax.net.ssl.SSLSocketFactory#getDefault()},
 * instead of the {@link javax.net.SocketFactory#getDefault()}.
 *
 * @author <a href="mailto:behar@veliqi.de">Behar Veliqi</a>
 * @since 23 Oct 2014 (creation date)
 */
public class SSLLogstashTcpSocketAppender extends LogstashTcpSocketAppender {
    

    public SSLLogstashTcpSocketAppender() {
        super();
        setSocketFactory(SSLSocketFactory.getDefault());
    }

}
