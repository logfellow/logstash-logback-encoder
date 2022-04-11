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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;

/**
 * A fast alternative to DateTimeFormatter when formatting millis using an ISO format.
 * 
 * <p>This class is thread safe.
 * 
 * <p>Note: This class is for internal use only and subject to backward incompatible change
 * at any time.
 * 
 * @author brenuart
 */
class FastISOTimestampFormatter {
    private static final long MILLISECONDS_PER_MINUTE = 60_000;
    
    /**
     * ThreadLocal with reusable {@link StringBuilder} instances.
     * Initialized with a size large enough to hold formats that do not include the Zone.
     * Will need to grow on first use otherwise.
     */
    private static ThreadLocal<StringBuilder> STRING_BUILDERS = ThreadLocal.withInitial(() -> new StringBuilder(30));

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
     * Whether trailing zero should be trimmed from the millis part
     */
    private final boolean trimMillis;
    
    
    /* Visible for testing */
    FastISOTimestampFormatter(DateTimeFormatter formatter, boolean trimMillis) {
        this.formatter = formatter;
        this.trimMillis = trimMillis;
        this.zoneOffsetState = new ZoneOffsetState(System.currentTimeMillis());
    }
    
    
    /**
     * Format the {@code timestampInMillis} millis.
     * 
     * @param timestampInMillis the millis to format
     * @return the formatted result
     */
    public String format(long timestampInMillis) {
        ZoneOffsetState current = this.zoneOffsetState;
        
        if (!current.canFormat(timestampInMillis)) {
            current = new ZoneOffsetState(timestampInMillis);
            this.zoneOffsetState = current;
        }

        return current.format(timestampInMillis);
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
        private final long zoneTransitionStart;
        private final long zoneTransitionStop;
        private final boolean cachingEnabled;
        private TimestampPeriod cachedTimestampPeriod;
        
        ZoneOffsetState(long timestampInMillis) {
            // Determine how long we can cache the previous result.
            // ZoneOffsets are usually expressed in hour:minutes but the Java time API accepts ZoneOffset with
            // a resolution up to the second:
            // - If the minutes/seconds parts of the ZoneOffset are zero, then we can cache for as long as one hour.
            // - If the ZoneOffset is expressed in "minutes", then we can cache the date/hour/minute part for as long as a minute.
            // - If the ZoneOffset is expressed in seconds, then we can cache only for one second.
            //
            // Also, take care of ZoneOffset transition (e.g. day light saving time).
            
            ZoneId zoneId = formatter.getZone();
            if (zoneId == null) {
                throw new IllegalArgumentException("formatter must be configured with a Zone override to format millis");
            }
            
            Instant now = Instant.ofEpochMilli(timestampInMillis);
            ZoneOffset zoneOffset;

            ZoneRules rules = zoneId.getRules();
            /*
             * The Zone has a fixed offset that will never change.
             */
            if (rules.isFixedOffset()) {
                zoneOffset = rules.getOffset(now);
                this.zoneTransitionStart = 0;
                this.zoneTransitionStop = Long.MAX_VALUE;
            }
            /*
             * The Zone has multiple offsets. Determine the one applicable at the given timestamp
             * and how long it is valid.
             */
            else {
                ZoneOffsetTransition zoneOffsetTransition = rules.nextTransition(now);
                if (zoneOffsetTransition == null) {
                    List<ZoneOffsetTransition> transitions = rules.getTransitions();
                    zoneOffsetTransition = transitions.get(transitions.size() - 1);
                }

                this.zoneTransitionStart = timestampInMillis;
                this.zoneTransitionStop = zoneOffsetTransition.toEpochSecond() * 1_000;
                zoneOffset = zoneOffsetTransition.getOffsetBefore();
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
            int offsetSeconds = zoneOffset.getTotalSeconds();
            this.cachingEnabled = (offsetSeconds % 60 == 0);
        }
        
        
        /**
         * Check whether the given timestamp is within the ZoneOffset represented by this state.
         * 
         * @param timestampInMillis the timestamp millis
         * @return {@code true} if the timestamp is within the ZoneOffset represented by this state
         */
        public boolean canFormat(long timestampInMillis) {
            return timestampInMillis >= this.zoneTransitionStart && timestampInMillis < this.zoneTransitionStop;
        }
        
        
        /**
         * Format a timestamp expressed in millis.
         * Note: you must first invoke {@link #canFormat(long)} to check that the state can be used to format the timestamp.
         * 
         * @param timestampInMillis the timestamp milis to format
         * @return the formatted timestamp
         */
        public String format(long timestampInMillis) {
            // If caching is disabled...
            //
            if (!this.cachingEnabled) {
                return buildFromFormatter(timestampInMillis);
            }
            
            // If tstamp is within the caching period...
            //
            TimestampPeriod currentTimestampPeriod = this.cachedTimestampPeriod;
            if (currentTimestampPeriod != null && currentTimestampPeriod.canFormat(timestampInMillis)) {
                return buildFromCache(currentTimestampPeriod, timestampInMillis);
            }
            
            // ... otherwise, use the formatter and cache the formatted value
            //
            String formatted = buildFromFormatter(timestampInMillis);
            cachedTimestampPeriod = createNewCache(timestampInMillis, formatted);
            return formatted;
        }
        
        
        private TimestampPeriod createNewCache(long timestampInMillis, String formatted) {
            // Examples of the supported formats:
            //
            //   ISO_OFFSET_DATE_TIME   2020-01-01T10:20:30.123+01:00
            //   ISO_ZONED_DATE_TIME    2020-01-01T10:20:30.123+01:00[Europe/Brussels]
            //   ISO_LOCAL_DATE_TIME    2020-01-01T10:20:30.123
            //   ISO_DATE_TIME          2020-01-01T10:20:30.123+01:00[Europe/Brussels]
            //   ISO_INSTANT            2020-01-01T09:20:30.123Z
            //                          +---------------+      +---------------------+
            //                               prefix                    suffix
            //
            // Seconds start at position 17 and are two digits long.
            // Millis are optional.
            
            
            // The part up to the minutes (included)
            String prefix = formatted.substring(0, 17);
            

            // The part of after the millis (i.e. the timezone)
            int pos = formatted.indexOf('+', 17);
            if (pos == -1) {
                pos = formatted.indexOf('-', 17);
            }
            if (pos == -1 && formatted.charAt(formatted.length() - 1) == 'Z') {
                pos = formatted.length() - 1;
            }
            String suffix = pos == -1 ? "" : formatted.substring(pos);
            
            // Determine how long we can use this cache
            long timstampInMinutes = timestampInMillis / MILLISECONDS_PER_MINUTE;
            long minuteStartInMillis = timstampInMinutes * MILLISECONDS_PER_MINUTE;
            long minuteStopInMillis = (timstampInMinutes + 1) * MILLISECONDS_PER_MINUTE;

            // Store in cache
            return new TimestampPeriod(minuteStartInMillis, minuteStopInMillis, prefix, suffix);
        }
        
        
        private String buildFromCache(TimestampPeriod cache, long timestampInMillis) {
            return cache.format(timestampInMillis);
        }
    }
    
    /* visible for testing */
    String buildFromFormatter(long timestampInMillis) {
        return FastISOTimestampFormatter.this.formatter.format(Instant.ofEpochMilli(timestampInMillis));
    }
    
    private class TimestampPeriod {
        private final long periodStartInMillis;
        private final long periodStopInMillis;
        private final String suffix;
        private final String prefix;

        TimestampPeriod(long periodStartInMillis, long periodStopInMillis, String prefix, String suffix) {
            this.periodStartInMillis = periodStartInMillis;
            this.periodStopInMillis = periodStopInMillis;
            this.prefix = prefix;
            this.suffix = suffix;
        }
        
        public boolean canFormat(long timestampInMillis) {
            return timestampInMillis >= this.periodStartInMillis && timestampInMillis < this.periodStopInMillis;
        }
        
        public String format(long timestampInMillis) {
            StringBuilder sb = STRING_BUILDERS.get();
            sb.setLength(0);
            sb.append(prefix);
            
            int millisSincePeriodStart = (int) (timestampInMillis - this.periodStartInMillis); // seconds & millis
            int seconds = millisSincePeriodStart / 1_000;
            int millis = millisSincePeriodStart % 1000;

            // seconds are always TWO digits...
            //
            if (seconds < 10) {
                sb.append('0');
            }
            sb.append(seconds);
            
            
            // millis are optional, max 3 significant digits (trailing 0 removed)
            //
            if (millis > 0) {
                int dotPos = sb.length();
                sb.append('.');
                
                // add leading 0...
                if (millis < 100) {
                    sb.append('0');
                }
                if (millis < 10) {
                    sb.append('0');
                }
                
                // add millis value...
                sb.append(millis);
                
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
            }
            
            // suffix...
            //
            sb.append(this.suffix);
            
            return sb.toString();
        }
    }
}
