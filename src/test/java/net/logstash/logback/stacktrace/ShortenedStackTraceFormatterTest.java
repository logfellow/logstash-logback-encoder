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
package net.logstash.logback.stacktrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.qos.logback.classic.spi.ThrowableProxy;

public class ShortenedStackTraceFormatterTest {
    
    private static class StackTraceElementGenerator {
        public static void generateSingle() {
            oneSingle();
        }
        public static void oneSingle() {
            twoSingle();
        }
        private static void twoSingle() {
            threeSingle();
        }
        private static void threeSingle() {
            four();
        }
        private static void four() {
            five();
        }
        private static void five() {
            six();
        }
        private static void six() {
            seven();
        }
        private static void seven() {
            eight();
        }
        private static void eight() {
            throw new RuntimeException("message");
        }
        public static void generateCausedBy() {
            oneCausedBy();
        }
        private static void oneCausedBy() {
            twoCausedBy();
        }
        private static void twoCausedBy() {
            try {
                threeSingle();
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
        public static void generateSuppressed() {
            oneSuppressed();
        }
        private static void oneSuppressed() {
            twoSuppressed();
        }
        private static void twoSuppressed() {
            try {
                threeSingle();
            } catch (RuntimeException e) {
                RuntimeException newException = new RuntimeException();
                newException.addSuppressed(e);
                throw newException;
            }
        }
    }
    
    @Test
    public void testTruncation() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            
            /*
             * First get the un-truncated length
             */
            formatter.setMaxStackTraceElementsPerThrowable(ShortenedStackTraceFormatter.UNLIMITED_STACK_TRACE_ELEMENTS);
            String formatted = formatter.format(new ThrowableProxy(e));
            int totalLines = countLines(formatted);
            
            /*
             * Now truncate and compare
             */
            formatter.setMaxStackTraceElementsPerThrowable(totalLines - 5);
            formatted = formatter.format(new ThrowableProxy(e));
            
            Assert.assertEquals(totalLines - 3, countLines(formatted));
            Assert.assertTrue(formatted.contains("4 frames truncated"));
            
        }
    }
    @Test
    public void testExclusion_consecutive() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            formatter.addExclude("one");
            formatter.addExclude("two");
            formatter.addExclude("four");
            formatter.addExclude("five");
            formatter.addExclude("six");
            formatter.setMaxStackTraceElementsPerThrowable(8);
            String formatted = formatter.format(new ThrowableProxy(e));
            Assert.assertTrue(formatted.contains("2 frames excluded"));
            Assert.assertTrue(formatted.contains("3 frames excluded"));
            Assert.assertEquals(12, countLines(formatted));
        }
    }
    @Test
    public void testExclusion_noConsecutive() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            formatter.setExcludes(Collections.singletonList("one"));
            formatter.setMaxStackTraceElementsPerThrowable(8);
            String formatted = formatter.format(new ThrowableProxy(e));
            Assert.assertFalse(formatted.contains("frames excluded"));
            Assert.assertEquals(10, countLines(formatted));
        }
    }

    @Test
    public void testExclusion_atEnd() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            
            /*
             * First get the un-truncated stacktrace
             */
            formatter.setMaxStackTraceElementsPerThrowable(ShortenedStackTraceFormatter.UNLIMITED_STACK_TRACE_ELEMENTS);
            String formatted = formatter.format(new ThrowableProxy(e));
            
            /*
             * Find the last two frames
             */
            
            List<String> lines = getLines(formatted);
            
            /*
             * Now truncate and compare
             */
            formatter.addExclude(extractClassAndMethod(lines.get(lines.size() - 2)) + "$");
            formatter.addExclude(extractClassAndMethod(lines.get(lines.size() - 1)) + "$");
            formatted = formatter.format(new ThrowableProxy(e));
            
            Assert.assertEquals(lines.size() - 1, countLines(formatted));
            Assert.assertTrue(formatted.contains("2 frames excluded"));
            
        }
    }

    @Test
    public void testCausedBy() {
        
        try {
            StackTraceElementGenerator.generateCausedBy();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            formatter.setMaxStackTraceElementsPerThrowable(8);
            String formatted = formatter.format(new ThrowableProxy(e));
            Assert.assertTrue(formatted.contains("Caused by"));
            Assert.assertTrue(formatted.contains("common frames omitted"));
        }
    }

    @Test
    public void testSuppressed() {
        
        try {
            StackTraceElementGenerator.generateSuppressed();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            formatter.setMaxStackTraceElementsPerThrowable(8);
            String formatted = formatter.format(new ThrowableProxy(e));
            Assert.assertTrue(formatted.contains("Suppressed"));
            Assert.assertTrue(formatted.contains("common frames omitted"));
        }
    }

    @Test
    public void testShortenedName() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            Assert.fail();
        } catch (RuntimeException e) {
            ShortenedStackTraceFormatter formatter = new ShortenedStackTraceFormatter();
            formatter.setMaxStackTraceElementsPerThrowable(ShortenedStackTraceFormatter.UNLIMITED_STACK_TRACE_ELEMENTS);
            formatter.setShortenedClassNameLength(10);
            String formatted = formatter.format(new ThrowableProxy(e));
            Assert.assertFalse(formatted.contains(getClass().getPackage().getName()));
            Assert.assertTrue(formatted.contains("n.l.l.s."));
        }
    }

    private String extractClassAndMethod(String string) {
        int atIndex = string.indexOf("at ");
        int endIndex = string.indexOf('(');
        return string.substring(atIndex + 3, endIndex);
    }

    private List<String> getLines(String formatted) {
        List<String> lines = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new StringReader(formatted));
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int countLines(String formatted) {
        return getLines(formatted).size();
    }

}
