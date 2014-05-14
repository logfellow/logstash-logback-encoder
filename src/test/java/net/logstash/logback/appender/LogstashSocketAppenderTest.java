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

import org.junit.Test;

public class LogstashSocketAppenderTest {

    @Test
    public void testNoNullPointerWithNoCustomFields() throws Exception {
        //The JSON Parser has been throwing a NPE if no custom field value is specified
        LogstashSocketAppender appender = new LogstashSocketAppender();
        appender.setHost("foo.com");
        appender.buildLayout();
    }

    @Test
    public void testNoNullPointerWithCustomFields() throws Exception {
        LogstashSocketAppender appender = new LogstashSocketAppender();
        appender.setHost("foo.com");
        appender.setCustomFields("");
        appender.buildLayout();
    }
}