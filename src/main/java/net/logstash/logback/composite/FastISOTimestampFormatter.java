/*
 * Copyright 2013-2025 the original author or authors.
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Objects;

/**
 * A fast alternative to {@link DateTimeFormatter} when formatting {@link Instant} using an ISO format.
 * 
 * <p>This class is thread safe.
 * 
 * <p>Note: This class is for internal use only and subject to backward incompatible change
 * at any time.
 * 
 * @author brenuart
 */
class FastISOTimestampFormatter {
    /**
     * ThreadLocal with reusable {@link StringBuilder} instances.
     * Initialized with a size large enough to hold formats that do not include the Zone.
     * Will need to grow on first use otherwise.
     */
    private static ThreadLocal<StringBuilder> STRING_BUILDERS = ThreadLocal.withInitial(() -> new StringBuilder(40));

    /**
     * Nanosecond decimals constants
     */
    private static final int[] DECIMALS = {100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10 };

    /**
     * The actual DateTimeFormatter used to format the timestamp when the cached
     * value cannot be used
     */
    private final DateTimeFormatter formatter;
    
    /**
     * Holds the cached formatted value. Can be reused only when the timestamp to format
     * is in the same ZoneOffset as the cached value.
     * 
     * This class is immutable and a new instance is created when needed. Two concurrent threads may create two
     * identical instances but only one will eventually remain. This strategy is cheaper than a lock or a volatile
     * field.
     */
    private ZoneOffsetState zoneOffsetState;
    
    /**
     * Whether trailing zero should be trimmed from the millis/nanos part.
     * When {@code false}, millis/nanos are output in group of 3 digits.
     */
    private final boolean trimMillis;
    
    
    /* Visible for testing */
    FastISOTimestampFormatter(DateTimeFormatter formatter, boolean trimMillis) {
        this.formatter = Objects.requireNonNull(formatter);
        this.trimMillis = trimMillis;

        if (formatter.getZone() == null) {
            throw new IllegalArgumentException("formatter must be configured with a Zone override to format Instant");
        }
    }
    
    
    /**
     * Format the {@code tstamp} timestamp.
     * 
     * @param tstamp the timestamp to format
     * @return the formatted result
     */
    public String format(Instant tstamp) {
        ZoneOffsetState current = this.zoneOffsetState;
        
        if (current == null || !current.canFormat(tstamp)) {
            current = new ZoneOffsetState(tstamp);
            this.zoneOffsetState = current;
        }

        return current.format(tstamp);
    }

    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoOffsetDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId), true);
    }

    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoZonedDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(zoneId), true);
    }
    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoLocalDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId), true);
    }
    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_DATE_TIME.withZone(zoneId), true);
    }
    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_INSTANT}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoInstant(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_INSTANT.withZone(zoneId), false);
    }
    
    
    /**
     * State valid during a zone transition.
     * Does not change frequently, typically before/after day-light transition.
     */
    private class ZoneOffsetState {
        private final Instant zoneTransitionStart;
        private final Instant zoneTransitionStop;
        private final boolean cachingEnabled;
        private TimestampPeriod cachedTimestampPeriod;
        
        ZoneOffsetState(Instant tstamp) {
            // Determine how long we can cache the previous result.
            // ZoneOffsets are usually expressed in hour:minutes but the Java time API accepts ZoneOffset with
            // a resolution up to the second:
            // - If the minutes/seconds parts of the ZoneOffset are zero, then we can cache for as long as one hour.
            // - If the ZoneOffset is expressed in "minutes", then we can cache the date/hour/minute part for as long as a minute.
            // - If the ZoneOffset is expressed in "seconds", then we can cache only for one second.
            //
            // Also, take care of ZoneOffset transition (e.g. day light saving time).
            
            ZoneRules rules = formatter.getZone().getRules();

            /*
             * The Zone has a fixed offset that will never change.
             */
            if (rules.isFixedOffset()) {
                this.zoneTransitionStart = Instant.MIN;
                this.zoneTransitionStop = Instant.MAX;
            }
            /*
             * The Zone has multiple offsets. Find the offset for the given timestamp
             * and determine how long it is valid.
             */
            else {
                ZoneOffsetTransition previousZoneOffsetTransition = rules.previousTransition(tstamp);
                if (previousZoneOffsetTransition == null) {
                    this.zoneTransitionStart = Instant.MIN;
                }
                else {
                    this.zoneTransitionStart = previousZoneOffsetTransition.getInstant();
                }
                
                ZoneOffsetTransition zoneOffsetTransition = rules.nextTransition(tstamp);
                if (zoneOffsetTransition == null) {
                    this.zoneTransitionStop = Instant.MAX;
                }
                else {
                    this.zoneTransitionStop = zoneOffsetTransition.getInstant();
                }
            }

            /*
             * Determine the precision of the zone offset.
             * 
             * If the offset is expressed with HH:mm without seconds, then the date/time part remains constant during
             * one minute and is not affected by the zone offset. This means we can safely deduce the second and millis
             * from a long timestamp.
             * 
             * The same applies for the minutes part if the offset contains hours only. In this case, the date/time part
             * remains constant for a complete hour increasing the time we can reuse that part. However, tests have shown
             * that the extra computation required to extract the minutes from the timestamp during rendering negatively
             * impact the overall performance.
             * 
             * Caching is therefore limited to one minute which is good enough for our usage.
             */
            int offsetSeconds = rules.getOffset(tstamp).getTotalSeconds();
            this.cachingEnabled = (offsetSeconds % 60 == 0);
        }
        
        
        /**
         * Check whether the given timestamp is within the ZoneOffset represented by this state.
         * 
         * @param tstamp the timestamp to format
         * @return {@code true} if the timestamp is within the ZoneOffset represented by this state
         */
        public boolean canFormat(Instant tstamp) {
            return tstamp.compareTo(this.zoneTransitionStart) >= 0 && tstamp.isBefore(this.zoneTransitionStop);
        }
        
        
        /**
         * Format a timestamp.
         * Note: you must first invoke {@link #canFormat(long)} to check that the state can be used to format the timestamp.
         * 
         * @param tstamp the timestamp to format
         * @return the formatted timestamp
         */
        public String format(Instant tstamp) {
            // If caching is disabled...
            //
            if (!this.cachingEnabled) {
                return buildFromFormatter(tstamp);
            }
            
            // If tstamp is within the caching period...
            //
            TimestampPeriod currentTimestampPeriod = this.cachedTimestampPeriod;
            if (currentTimestampPeriod != null && currentTimestampPeriod.canFormat(tstamp)) {
                return buildFromCache(currentTimestampPeriod, tstamp);
            }
            
            // ... otherwise, use the formatter and cache the formatted value
            //
            String formatted = buildFromFormatter(tstamp);
            cachedTimestampPeriod = createNewCache(tstamp, formatted);
            return formatted;
        }
        
        
        private TimestampPeriod createNewCache(Instant tstamp, String formatted) {
            // Examples of the supported formats:
            //
            //   ISO_OFFSET_DATE_TIME   2020-01-01T10:20:30.123+01:00
            //   ISO_ZONED_DATE_TIME    2020-01-01T10:20:30.123+01:00[Europe/Brussels]
            //                          2020-01-01T10:20:30.123Z[UTC]
            //   ISO_LOCAL_DATE_TIME    2020-01-01T10:20:30.123
            //   ISO_DATE_TIME          2020-01-01T10:20:30.123+01:00[Europe/Brussels]
            //                          2020-01-01T10:20:30.123Z[UTC]
            //   ISO_INSTANT            2020-01-01T09:20:30.123Z
            //                          +---------------+      +---------------------+
            //                               prefix                    suffix
            //
            // Seconds start at position 17 and are two digits long.
            // Millis/Nanos are optional.
            
            
            // The part up to the minutes (included)
            String prefix = formatted.substring(0, 17);

            // The part of after the millis (i.e. the timezone)
            String suffix = findSuffix(formatted, 17);
            
            // Determine how long we can use this cache -> cache is valid only during the current minute
            Instant cacheStart = tstamp.truncatedTo(ChronoUnit.MINUTES);
            Instant cacheStop = cacheStart.plus(1, ChronoUnit.MINUTES);

            // Store in cache
            return new TimestampPeriod(cacheStart, cacheStop, prefix, suffix);
        }
        
        private String findSuffix(String formatted, int beginIndex) {
            boolean dotFound = false;
            int pos = beginIndex;
            
            while (pos < formatted.length()) {
                char c = formatted.charAt(pos);
                
                // Allow for a single dot...
                if (c == '.') {
                    if (dotFound) {
                        break;
                    }
                    else {
                        dotFound = true;
                    }
                }
                else if (!Character.isDigit(c)) {
                    break;
                }
                
                pos++;
            }
            
            if (pos < formatted.length()) {
                return formatted.substring(pos);
            }
            else {
                return "";
            }
        }
        
        private String buildFromCache(TimestampPeriod cache, Instant tstamp) {
            return cache.format(tstamp);
        }
    }
    
    /* visible for testing */
    String buildFromFormatter(Instant tstamp) {
        return FastISOTimestampFormatter.this.formatter.format(tstamp);
    }
    
    private class TimestampPeriod {
        private final Instant periodStart;
        private final Instant periodStop;
        private final String suffix;
        private final String prefix;

        TimestampPeriod(Instant periodStart, Instant periodStop, String prefix, String suffix) {
            this.periodStart = periodStart;
            this.periodStop = periodStop;
            this.prefix = prefix;
            this.suffix = suffix;
        }
        
        public boolean canFormat(Instant tstamp) {
            return tstamp.compareTo(this.periodStart) >= 0 && tstamp.isBefore(this.periodStop);
        }
        
        public String format(Instant tstamp) {
            StringBuilder sb = STRING_BUILDERS.get();
            sb.setLength(0);
            sb.append(prefix);
            
            int nanos = tstamp.getNano();
            int seconds = (int) (tstamp.getEpochSecond() - this.periodStart.getEpochSecond());


            // seconds are always TWO digits...
            //
            if (seconds < 10) {
                sb.append('0');
            }
            sb.append(seconds);
            
            
            // millis/nanos are optional, max 9 significant digits
            //
            if (nanos > 0) {
                int dotPos = sb.length();
                sb.append('.');
                
                // add leading 0...
                for (int i = 0; i < DECIMALS.length; i++) {
                    if (nanos < DECIMALS[i]) {
                        sb.append('0');
                    }
                    else {
                        break;
                    }
                }
                
                // add millis/nanos value...
                sb.append(nanos);
                
                // remove trailing 0...
                if (FastISOTimestampFormatter.this.trimMillis) {
                    while (sb.length() > dotPos) {
                        if (sb.charAt(sb.length() - 1) == '0') {
                            sb.setLength(sb.length() - 1);
                        }
                        else {
                            break;
                        }
                    }
                }
                // ... if not trimming, keep them by group of 3 (i.e. 3, 6 or 9 digits)
                else {
                    while (sb.length() > dotPos) {
                        if (sb.charAt(sb.length() - 1) == '0'
                            && sb.charAt(sb.length() - 2) == '0'
                            && sb.charAt(sb.length() - 3) == '0'
                        ) {
                            sb.setLength(sb.length() - 3);
                        }
                        else {
                            break;
                        }
                    }
                }
            }
            
            // suffix...
            //
            sb.append(this.suffix);
            
            return sb.toString();
        }
    }
}
