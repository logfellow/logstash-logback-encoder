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

import ch.qos.logback.core.net.ssl.SSLConfiguration;

/**
 * This class extends {@link net.logstash.logback.appender.LogstashTcpSocketAppender}
 * to enable ssl by default.
 * <p>
 * New users should just use {@link LogstashTcpSocketAppender} directly,
 * and configure ssl on it, rather than using this class.
 * <p>
 * This class remains available for backwards compatibility only.
 *
 * @author <a href="mailto:behar@veliqi.de">Behar Veliqi</a>
 * @since 23 Oct 2014 (creation date)
 * @deprecated SSL capability has been added to AbstractLogstashTcpSocketAppender.
 *             Just use LogstashTcpSocketAppender and configure ssl on it.
 */
@Deprecated
public class SSLLogstashTcpSocketAppender extends LogstashTcpSocketAppender {


    public SSLLogstashTcpSocketAppender() {
        super();
        setSsl(new SSLConfiguration());
    }

}
