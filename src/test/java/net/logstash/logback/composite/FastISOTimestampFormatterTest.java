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
package net.logstash.logback.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;


/**
 * @author brenuart
 *
 */
public class FastISOTimestampFormatterTest {
    // ===========================================================================================================
    //
    //                                              !! WARNING !!
    //
    // Remember the FastISOTimestampFormatter "caches" part of the formatted string during 1 minute.
    // When the minute is over (and cache discarded) a new formatted string is built using a DateTimeFormatter.
    // If you test the class by formatting a single value, the caching feature will not be used and the formatted
    // string will come directly from the DateTimeFormatter...
    //
    // See #invokeTwice(...) below.
    //
    // ===========================================================================================================
    
    private ZoneId zone = ZoneId.of("Europe/Brussels");

    
    /*
     * The DateTimeFormatter must have the "zone" overridden or an IllegalArgumentException is thrown
     */
    @Test
    public void zoneMustBeOverridden() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> FastISOTimestampFormatter.isoOffsetDateTime(null));
    }
    

    /*
     * Check that caching of previous values works as expected
     */
    @Test
    public void checkCaching() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);
        FastISOTimestampFormatter fast = spy(new FastISOTimestampFormatter(formatter, true));
        
        ZonedDateTime now = ZonedDateTime.of(2020, 01, 01, 10, 20, 30, 0, zone);

        // Cache is empty - first call comes from the DateTimeFormatter
        assertThat(isNotFromCache(fast, now.toInstant())).isEqualTo(formatter.format(now));

        
        //
        // Stay within the same minute (i.e. within cache validity)
        //
        
        // Second call with same date/time comes from the cache
        assertThat(isFromCache(fast, now.toInstant())).isEqualTo(formatter.format(now));
        
        // Next calls within the same minute come from the cache as well
        ZonedDateTime current = now.plusSeconds(29);
        assertThat(isFromCache(fast, current.toInstant())).isEqualTo(formatter.format(current));

        
        //
        // Move forward to next minute
        //
        current = now.plusMinutes(1);
        assertThat(isNotFromCache(fast, current.toInstant())).isEqualTo(formatter.format(current));
        
        current = current.plusSeconds(29);
        assertThat(isFromCache(fast, current.toInstant())).isEqualTo(formatter.format(current));
        
        
        //
        // Go backwards
        //
        current = now.minusMinutes(1);
        assertThat(isNotFromCache(fast, current.toInstant())).isEqualTo(formatter.format(current));
    }
    
    
    /*
     * Check that millis are expressed with the minimal required number of digits
     */
    @TestFactory
    public Collection<DynamicTest> checkMillisDigits() {
        return doForAllSupportedFormats(zone, (formatter, fast) -> {
            // Warm the cache
            Instant instant = ZonedDateTime.of(2020, 01, 01, 10, 20, 0, 0, zone).toInstant();
            assertThat(fast.format(instant.toEpochMilli())).isEqualTo(formatter.format(instant));

            // millis: 0
            instant = ZonedDateTime.of(2020, 01, 01, 10, 20, 30, 0, zone).toInstant();
            assertThat(fast.format(instant.toEpochMilli())).isEqualTo(formatter.format(instant));

            // millis: .001 (3 decimals)
            instant = ZonedDateTime.of(2020, 01, 01, 10, 20, 30, (int) TimeUnit.MILLISECONDS.toNanos(1), zone).toInstant();
            assertThat(fast.format(instant.toEpochMilli())).isEqualTo(formatter.format(instant));
            
            // millis: .01 (2 decimals)
            instant = ZonedDateTime.of(2020, 01, 01, 10, 20, 30, (int) TimeUnit.MILLISECONDS.toNanos(10), zone).toInstant();
            assertThat(fast.format(instant.toEpochMilli())).isEqualTo(formatter.format(instant));
            
            // millis: .1 (1 decimals)
            instant = ZonedDateTime.of(2020, 01, 01, 10, 20, 30, (int) TimeUnit.MILLISECONDS.toNanos(100), zone).toInstant();
            assertThat(fast.format(instant.toEpochMilli())).isEqualTo(formatter.format(instant));
            
            // millis: .123 (3 decimals)
            instant = ZonedDateTime.of(2020, 01, 01, 10, 20, 30, (int) TimeUnit.MILLISECONDS.toNanos(123), zone).toInstant();
            assertThat(fast.format(instant.toEpochMilli())).isEqualTo(formatter.format(instant));
        });
    }
    
    
    /*
     * Assert day-light transitions are OK
     */
    @TestFactory
    public Collection<DynamicTest> daylightTransition() {
        // [Europe/Brussels] has day light transition
        //
        ZoneId zoneId = ZoneId.of("Europe/Brussels");
        
        Instant now = Instant.now();
        ZoneOffsetTransition zoneOffsetTransition = zoneId.getRules().nextTransition(now);
        
       
        return doForAllSupportedFormats(zoneId, (formatter, fast) -> {
            // Before the transition
            ZonedDateTime before = zoneOffsetTransition.getDateTimeBefore().atZone(zoneId).minusMinutes(1);
            assertThat(invokeTwice(fast, before.toInstant())).isEqualTo(formatter.format(before));
                
            // After the transition
            ZonedDateTime after = zoneOffsetTransition.getDateTimeAfter().atZone(zoneId).plusMinutes(1);
            assertThat(invokeTwice(fast, after.toInstant())).isEqualTo(formatter.format(after));
        });
    }
    
    
    /*
     * Create a custom TimeZone with two rules and offsets expressed with different precisions:
     * - winter has an offset in HOURS
     * - summer has an offset in HOURS:MINUTES
     * 
     * The "fast" algorithm will be disabled during "summer" because of the "seconds" precision.
     */
    @TestFactory
    public Collection<DynamicTest> customTimeZone() {
        //
        // Create the custom TimeZone
        //
        final String customZoneId = "Custom-TimeZone";

        final ZoneOffset winterTimeOffset = ZoneOffset.ofHoursMinutes(1, 30);
        final ZoneOffset summerTimeOffset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 15);
        // At least one transition is required
        ZoneOffsetTransition initialTransition = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, 1, 1, 3, 0), summerTimeOffset, winterTimeOffset);
        List<ZoneOffsetTransition> transitionList = Arrays.asList(initialTransition);

        // Winter->Summer: switch around Jan 1st @ 2AM
        ZoneOffsetTransitionRule springRule =
                ZoneOffsetTransitionRule.of(Month.JANUARY, 1, DayOfWeek.MONDAY, LocalTime.of(2, 0),
                        false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, winterTimeOffset,
                        winterTimeOffset, summerTimeOffset);
        // Summer->Winter: switch around Oct 1st @ 2AM
        ZoneOffsetTransitionRule fallRule =
                ZoneOffsetTransitionRule.of(Month.OCTOBER, -1, DayOfWeek.SUNDAY, LocalTime.of(2, 0),
                        false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, winterTimeOffset,
                        summerTimeOffset, winterTimeOffset);
        ZoneRules rules = ZoneRules.of(winterTimeOffset, winterTimeOffset,
                transitionList, transitionList, Arrays.asList(springRule, fallRule));

        // Register the new custom rule - the heart of the magic: the ZoneRulesProvider
        Set<String> zoneIds = new HashSet<>();
        zoneIds.add(customZoneId);
        
        ZoneRulesProvider customProvider = new ZoneRulesProvider() {
            @Override
            protected Set<String> provideZoneIds() {
                return zoneIds;
            }

            @Override
            protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
                TreeMap<String, ZoneRules> map = new TreeMap<>();
                map.put(customZoneId, rules);
                return map;
            }

            @Override
            protected ZoneRules provideRules(String zoneId, boolean forCaching) {
                return rules;
            }
        };

        // Registering the ZoneRulesProvider is the key to ZoneId using it
        ZoneRulesProvider.registerProvider(customProvider);
        
        
        
        //
        // TestCase:
        //
        // Render times during summer and winter
        //
        ZoneId zoneId = ZoneId.of(customZoneId);
        
        ZonedDateTime duringSummer = ZonedDateTime.of(2022,  1, 31, 0, 0, 0, 0, zoneId);
        ZonedDateTime duringWinter = ZonedDateTime.of(2022, 10, 31, 0, 0, 0, 0, zoneId);
        
        return doForAllSupportedFormats(zoneId, (formatter, fast2) -> {
            FastISOTimestampFormatter fast = spy(fast2);
            
            // Caching is DISABLED during summer because of the offset expressed with minutes precision
            //
            assertThat(invokeTwice(fast, duringSummer.toInstant())).isEqualTo(formatter.format(duringSummer));
            verify(fast, times(2)).buildFromFormatter(duringSummer.toInstant().toEpochMilli());
            
            // Caching is ENABLED during winter
            //
            Mockito.reset(fast);
            assertThat(invokeTwice(fast, duringWinter.toInstant())).isEqualTo(formatter.format(duringWinter));
            verify(fast, times(1)).buildFromFormatter(duringWinter.toInstant().toEpochMilli());
        });
    }


    /*
     * "Asia/Bangkok" has a single transition 1 Apr 1920.
     * 
     * Assert that rendering a timestamp before/after the transition works as expected (and does
     * not throw any exception).
     */
    @TestFactory
    public Collection<DynamicTest> afterTheLastTransition() {
        ZoneId zoneId = ZoneId.of("Asia/Bangkok");
        
        ZonedDateTime before = ZonedDateTime.of(1919, 1, 1, 0, 0, 0, 0, zoneId);
        ZonedDateTime after  = ZonedDateTime.of(1921, 1, 1, 0, 0, 0, 0, zoneId);
        
        return doForAllSupportedFormats(zoneId, (formatter, fast) -> {
            assertThat(invokeTwice(fast, before.toInstant())).isEqualTo(formatter.format(before));
            assertThat(invokeTwice(fast, after.toInstant())).isEqualTo(formatter.format(after));
        });
    }
    
    
    /*
     * Invoke same formatter from concurrent threads. Force invalidation of the cache by making
     * one thread going forward and the other backwards.
     * 
     * Note that this test is not an accurate proof that everything is OK in multi-threaded
     * environment... but it is better than nothing.
     */
    @Test
    public void threadSafety() {
        
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(zone);
        final FastISOTimestampFormatter fast = FastISOTimestampFormatter.isoZonedDateTime(zone);
        
        ZonedDateTime now = ZonedDateTime.of(2020, 01, 01, 10, 20, 0, 0, zone);
        
        ExecutorService execService = Executors.newCachedThreadPool();

        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            
            Future<Void> result1 = execService.submit(() -> {
                    barrier.await();
                    
                    ZonedDateTime current = now;
                    for (int i = 0; i < 3600; i++) {
                        assertThat(fast.format(current.toInstant().toEpochMilli())).isEqualTo(formatter.format(current));
                        current = current.plusSeconds(1);
                    }
                    
                    return null;
                });
            
            Future<Void> result2 = execService.submit(() -> {
                barrier.await();
                
                ZonedDateTime current = now;
                for (int i = 0; i < 3600; i++) {
                    assertThat(fast.format(current.toInstant().toEpochMilli())).isEqualTo(formatter.format(current));
                    current = current.minusSeconds(1);
                }
                
                return null;
            });
            
            assertThatCode(() -> result1.get()).doesNotThrowAnyException();
            assertThatCode(() -> result2.get()).doesNotThrowAnyException();
        }
        finally {
            execService.shutdownNow();
        }
    }
    
    
    // --------------------------------------------------------------------------------------------
    
    
    @FunctionalInterface
    interface TestCase {
        void execute(DateTimeFormatter formatter, FastISOTimestampFormatter fast);
    }
    
    
    /*
     * Create a collection of DynamicTests, one for each supported time format
     */
    private static Collection<DynamicTest> doForAllSupportedFormats(ZoneId zone, TestCase testCase) {
        return Arrays.asList(
            DynamicTest.dynamicTest("ISO_OFFSET_DATE_TIME", () ->
                testCase.execute(
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone),
                    FastISOTimestampFormatter.isoOffsetDateTime(zone))),
            
            DynamicTest.dynamicTest("ISO_ZONED_DATE_TIME", () ->
                testCase.execute(
                    DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(zone),
                    FastISOTimestampFormatter.isoZonedDateTime(zone))),
            
            DynamicTest.dynamicTest("ISO_LOCAL_DATE_TIME", () ->
                testCase.execute(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zone),
                    FastISOTimestampFormatter.isoLocalDateTime(zone))),
            
            DynamicTest.dynamicTest("ISO_DATE_TIME", () ->
                testCase.execute(
                    DateTimeFormatter.ISO_DATE_TIME.withZone(zone),
                    FastISOTimestampFormatter.isoDateTime(zone))),

            DynamicTest.dynamicTest("ISO_INSTANT", () ->
                testCase.execute(
                    DateTimeFormatter.ISO_INSTANT.withZone(zone),
                    FastISOTimestampFormatter.isoInstant(zone))));
    }

    
    /**
     * Invoke the FastISOTimestampFormatter twice to force the result to come out of the
     * cache if enabled.
     * 
     * @param fast the fast formatter
     * @param instant the instant to format
     * @return the formatted result
     */
    private static String invokeTwice(FastISOTimestampFormatter fast, Instant instant) {
        long millis = instant.toEpochMilli();
        
        // Format a first time to "warm" the cache
        fast.format(millis);
        
        // Format a second time - value should come out of the cache when enabled
        return fast.format(millis);
    }

    
    /**
     * Format the {@code instant} using the supplied {@link FastISOTimestampFormatter} {@code fast}
     * formatter and assert that the result is generated from the internal cache instead of the
     * enclosed DateTimeFormatter.
     * 
     * @param fast the fast formatter
     * @param instant the instant to format
     * @return the formatted result
     */
    private static String isFromCache(FastISOTimestampFormatter fast, Instant instant) {
        Mockito.clearInvocations(fast);
        
        long millis = instant.toEpochMilli();
        String formattedValue = fast.format(millis);
        
        verify(fast, times(0)).buildFromFormatter(millis);
        
        return formattedValue;
    }

    /**
     * Format the {@code instant} using the supplied {@link FastISOTimestampFormatter} {@code fast}
     * formatter and assert that the result is generated from the enclosed DateTimeFormatter and does
     * not come from the internal cache.
     * 
     * @param fast the fast formatter
     * @param instant the instant to format
     * @return the formatted result
     */
    private static String isNotFromCache(FastISOTimestampFormatter fast, Instant instant) {
        Mockito.clearInvocations(fast);
        
        long millis = instant.toEpochMilli();
        String formattedValue = fast.format(millis);
        
        verify(fast, times(1)).buildFromFormatter(millis);
        
        return formattedValue;
    }
}
