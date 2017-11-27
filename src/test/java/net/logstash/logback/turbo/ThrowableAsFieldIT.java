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
package net.logstash.logback.turbo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.*;
import org.slf4j.LoggerFactory;

public class ThrowableAsFieldIT {
    private ByteArrayOutputStream logOutput;
    private PrintStream actualSystemOut;

    @Before
    public void setUp() {
        actualSystemOut = System.out;
        logOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(logOutput));
    }

    @After
    public void cleanUp() {
        System.setOut(actualSystemOut);
        System.out.println(logOutput.toString());
    }

    @Test
    public void test() throws InterruptedException {
        LoggerFactory.getLogger(this.getClass()).info("message without exception");
        LoggerFactory.getLogger(this.getClass()).info("message with exception", new IllegalArgumentException());
        LoggerFactory.getLogger(this.getClass()).info("message with nested exception",
                new RuntimeException(new IOException(new IllegalArgumentException())));

        assertThat(logOutput.toString(), containsString("[] message without exception"));
        assertThat(logOutput.toString(),
                containsString("[throwable_class=IllegalArgumentException] message with exception"));
        assertThat(logOutput.toString(), containsString(
                "[throwable_class=RuntimeException, throwable_cause_class=IllegalArgumentException] message with nested exception"));
    }
}
