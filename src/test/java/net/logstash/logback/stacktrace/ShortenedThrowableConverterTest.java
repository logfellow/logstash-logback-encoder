/*
 * Copyright 2013-2022 the original author or authors.
 *
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ShortenedThrowableConverterTest {
    
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
                throw new RuntimeException("wrapper", e);
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
    public void testDepthTruncation() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            
            /*
             * First get the un-truncated length
             */
            converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
            converter.start();
            String formatted = converter.convert(createEvent(e));
            int totalLines = countLines(formatted);
            
            /*
             * Now truncate and compare
             */
            converter.setMaxDepthPerThrowable(totalLines - 5);
            converter.start();

            formatted = converter.convert(createEvent(e));
            
            assertThat(countLines(formatted)).isEqualTo(totalLines - 3);
            assertThat(formatted).contains("4 frames truncated");
        }
    }
    
    @Test
    public void testLengthTruncation() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

            /*
             * First get the un-truncated length
             */
            converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
            converter.start();
            String formatted = converter.convert(createEvent(e));
            int totalLength = formatted.length();
            
            /*
             * Now truncate and compare
             */
            converter.setMaxLength(totalLength - 10);
            converter.start();
            formatted = converter.convert(createEvent(e));
            
            assertThat(formatted)
                .hasSize(totalLength - 10)
                .endsWith("..." + System.getProperty("line.separator"));
        }
    }
    
    @Test
    public void testExclusion_consecutive() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.addExclude("one");
            converter.addExclude("two");
            converter.addExclude("four");
            converter.addExclude("five");
            converter.addExclude("six");
            converter.setMaxDepthPerThrowable(8);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            assertThat(formatted)
                .containsSubsequence("3 frames excluded", "2 frames excluded");
            assertThat(countLines(formatted)).isEqualTo(12);
        }
    }
    
    @Test
    public void testExclusion_noConsecutive() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setExcludes(Collections.singletonList("one"));
            converter.setMaxDepthPerThrowable(8);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            assertThat(formatted).doesNotContain("frames excluded");
            assertThat(countLines(formatted)).isEqualTo(10);
        }
    }

    @Test
    public void testExclusion_atEnd() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            
            /*
             * First get the un-truncated stacktrace
             */
            converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
            converter.start();
            String formatted = converter.convert(createEvent(e));
            
            /*
             * Find the last two frames
             */
            
            List<String> lines = getLines(formatted);
            
            /*
             * Now truncate and compare
             */
            converter.addExclude(extractClassAndMethod(lines.get(lines.size() - 2)) + "$");
            converter.addExclude(extractClassAndMethod(lines.get(lines.size() - 1)) + "$");
            converter.start();
            formatted = converter.convert(createEvent(e));
            assertThat(formatted).contains("2 frames excluded");
            assertThat(countLines(formatted)).isEqualTo(lines.size() - 1);
        }
    }

    @Test
    public void testCausedBy() {
        
        try {
            StackTraceElementGenerator.generateCausedBy();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setMaxDepthPerThrowable(8);
            converter.start();

            String formatted = converter.convert(createEvent(e));
            assertThat(formatted)
                .containsSubsequence("wrapper", "Caused by", "message", "common frames omitted");
        }
    }

    @Test
    public void testRootCauseFirst() {
        
        try {
            StackTraceElementGenerator.generateCausedBy();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setRootCauseFirst(true);
            converter.setMaxDepthPerThrowable(8);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            assertThat(formatted)
                .containsSubsequence("message", "common frames omitted", "Wrapped by", "wrapper");
        }
    }

    @Test
    public void testEvaluator() throws EvaluationException {
        
        try {
            StackTraceElementGenerator.generateCausedBy();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            
            @SuppressWarnings("unchecked")
            EventEvaluator<ILoggingEvent> evaluator = mock(EventEvaluator.class);
            when(evaluator.evaluate(any(ILoggingEvent.class))).thenReturn(true);
            converter.addEvaluator(evaluator);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            assertThat(formatted).isEmpty();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testOptions() throws EvaluationException {
        
        EventEvaluator evaluator = mock(EventEvaluator.class);
        Map<String, EventEvaluator> evaluatorMap = new HashMap<String, EventEvaluator>();
        evaluatorMap.put("evaluator", evaluator);
        
        Context context = mock(Context.class);
        when(context.getObject(CoreConstants.EVALUATOR_MAP)).thenReturn(evaluatorMap);
        
        ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
        converter.setContext(context);
        
        // test full values
        converter.setOptionList(Arrays.asList("full", "full", "full", "rootFirst", "inlineHash", "evaluator", "regex"));
        converter.start();
        
        assertThat(converter.getMaxDepthPerThrowable()).isEqualTo(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
        assertThat(converter.getShortenedClassNameLength()).isEqualTo(ShortenedThrowableConverter.FULL_CLASS_NAME_LENGTH);
        assertThat(converter.getMaxLength()).isEqualTo(ShortenedThrowableConverter.FULL_MAX_LENGTH);
        assertThat(converter.isRootCauseFirst()).isTrue();
        assertThat(converter.getEvaluators().get(0)).isEqualTo(evaluator);
        assertThat(converter.getExcludes().get(0)).isEqualTo("regex");
        
        // test short values
        converter.setOptionList(Arrays.asList("short", "short", "short", "rootFirst", "inlineHash", "evaluator", "regex"));
        converter.start();
        
        assertThat(converter.getMaxDepthPerThrowable()).isEqualTo(ShortenedThrowableConverter.SHORT_MAX_DEPTH_PER_THROWABLE);
        assertThat(converter.getShortenedClassNameLength()).isEqualTo(ShortenedThrowableConverter.SHORT_CLASS_NAME_LENGTH);
        assertThat(converter.getMaxLength()).isEqualTo(ShortenedThrowableConverter.SHORT_MAX_LENGTH);
        
        // test numeric values
        converter.setOptionList(Arrays.asList("1", "2", "3"));
        converter.start();
        assertThat(converter.getMaxDepthPerThrowable()).isEqualTo(1);
        assertThat(converter.getShortenedClassNameLength()).isEqualTo(2);
        assertThat(converter.getMaxLength()).isEqualTo(3);
    }

    @Test
    public void testSuppressed() {
        
        try {
            StackTraceElementGenerator.generateSuppressed();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setMaxDepthPerThrowable(8);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            assertThat(formatted)
                .contains("Suppressed")
                .contains("common frames omitted");
        }
    }

    @Test
    public void testShortenedName() {
        
        try {
            StackTraceElementGenerator.generateSingle();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
            converter.setShortenedClassNameLength(10);
            converter.start();
            
            String formatted = converter.convert(createEvent(e));
            assertThat(formatted)
                .doesNotContain(getClass().getPackage().getName())
                .contains("n.l.l.s.");
        }
    }

    @Test
    public void test_inline_hash() {
        try {
            StackTraceElementGenerator.generateCausedBy();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher mockedHasher = Mockito.mock(StackHasher.class);
            List<String> expectedHashes = Arrays.asList("11111111", "22222222");
            Mockito.when(mockedHasher.hexHashes(any(Throwable.class))).thenReturn(new ArrayDeque<String>(expectedHashes));
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setInlineHash(true);
            converter.start();
            converter.setStackHasher(mockedHasher);

            // WHEN
            String formatted = converter.convert(createEvent(e));

            // THEN
            // verify we have expected stack hashes inlined
            List<String> actualHashes = extractStackHashes(formatted);
            assertThat(actualHashes).containsExactlyElementsOf(expectedHashes);
        }
    }
    
    @Test
    public void test_inline_hash_root_cause_first() {
        try {
            StackTraceElementGenerator.generateCausedBy();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher mockedHasher = Mockito.mock(StackHasher.class);
            List<String> expectedHashes = Arrays.asList("11111111", "22222222");
            Mockito.when(mockedHasher.hexHashes(any(Throwable.class))).thenReturn(new ArrayDeque<String>(expectedHashes));
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setInlineHash(true);
            converter.setRootCauseFirst(true);
            converter.start();
            converter.setStackHasher(mockedHasher);

            // WHEN
            String formatted = converter.convert(createEvent(e));

            // THEN
            // verify we have expected stack hashes inlined
            List<String> actualHashes = extractStackHashes(formatted);
            List<String> expectedHashesInReverseOrder = new ArrayList<String>(expectedHashes);
            Collections.reverse(expectedHashesInReverseOrder);
            assertThat(actualHashes).containsExactlyElementsOf(expectedHashesInReverseOrder);
        }
    }

    @Test
    public void test_inline_hash_with_suppressed() {
        try {
            StackTraceElementGenerator.generateSuppressed();
            fail("Exception must have been thrown");
        } catch (RuntimeException e) {
            // GIVEN
            StackHasher mockedHasher = Mockito.mock(StackHasher.class);
            List<String> expectedHashes = Arrays.asList("11111111"); // only one exception, no cause
            Mockito.when(mockedHasher.hexHashes(any(Throwable.class))).thenReturn(new ArrayDeque<String>(expectedHashes));
            ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
            converter.setInlineHash(true);
            converter.start();
            converter.setStackHasher(mockedHasher);

            // WHEN
            String formatted = converter.convert(createEvent(e));

            // THEN
            // verify we have expected stack hashes inlined
            List<String> actualHashes = extractStackHashes(formatted);
            assertThat(actualHashes).containsExactlyElementsOf(expectedHashes);
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

    private ILoggingEvent createEvent(RuntimeException e) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(e));
        return event;
    }
    
    private List<String> extractStackHashes(String formattedStackTrace) {
        Pattern hashPattern = Pattern.compile("<#([0-9abcdef]{8})>");
        Matcher matcher = hashPattern.matcher(formattedStackTrace);
        List<String> hashes = new ArrayList<String>();
        while (matcher.find()) {
            hashes.add(matcher.group(1));
        }
        return hashes;
    }
}
