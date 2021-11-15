/*
 * Copyright 2013-2021 the original author or authors.
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
    /**
     * ThreadLocal with reusable {@link StringBuilder} instances
     */
    private static ThreadLocal<StringBuilder> STRING_BUILDERS = ThreadLocal.withInitial(StringBuilder::new);

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
    private boolean trimMillis = true;
    
    
    /* Visible for testing */
    FastISOTimestampFormatter(DateTimeFormatter formatter) {
        this.formatter = formatter;
        this.zoneOffsetState = new ZoneOffsetState(System.currentTimeMillis());
    }
    
    
    /* Visible for testing */
    private void setTrimMillis(boolean trimMillis) {
        this.trimMillis = trimMillis;
    }
    
    
    /**
     * Format the {@code timestamp} millis.
     * 
     * @param timestamp the millis to format
     * @return the formatted result
     */
    public String format(long timestamp) {
        ZoneOffsetState current = this.zoneOffsetState;
        
        if (!current.isApplicable(timestamp)) {
            current = new ZoneOffsetState(timestamp);
            this.zoneOffsetState = current;
        }

        return current.format(timestamp);
    }

    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoOffsetDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId));
    }

    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoZonedDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(zoneId));
    }
    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoLocalDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId));
    }
    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_DATE_TIME}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoDateTime(ZoneId zoneId) {
        return new FastISOTimestampFormatter(DateTimeFormatter.ISO_DATE_TIME.withZone(zoneId));
    }
    
    /**
     * Create a fast formatter using the same format as {@link DateTimeFormatter#ISO_INSTANT}.
     * 
     * @param zoneId the zone override
     * @return a fast formatter
     */
    public static FastISOTimestampFormatter isoInstant(ZoneId zoneId) {
        FastISOTimestampFormatter fast = new FastISOTimestampFormatter(DateTimeFormatter.ISO_INSTANT.withZone(zoneId));
        fast.setTrimMillis(false);
        return fast;
    }
    
    
    /**
     * State valid during a zone transition.
     * Does not change frequently, typically before/after day-light transition.
     */
    private class ZoneOffsetState {
        private final long zoneTransitionStart;
        private final long zoneTransitionStop;
        private final boolean cachingEnabled;
        private TimestampCache cache;
        
        ZoneOffsetState(long tstamp) {
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
            
            Instant now = Instant.ofEpochMilli(tstamp);
            ZoneOffset zoneOffset;
            
            /*
             * The Zone has a fixed offset that will never change.
             */
            if (zoneId.getRules().isFixedOffset()) {
                zoneOffset = zoneId.getRules().getOffset(now);
                this.zoneTransitionStart = 0;
                this.zoneTransitionStop = Long.MAX_VALUE;
            }
            /*
             * The Zone has multiple offsets. Determine the one applicable at the given timestamp
             * and how long it is valid.
             */
            else {
                ZoneOffsetTransition zoneOffsetTransition = zoneId.getRules().nextTransition(now);
                this.zoneTransitionStart = tstamp;
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
         * Check whether this state is applicable for the given timestamp.
         * 
         * @param tstamp the timestamp millis
         * @return {@code true} if this state is applicable to the given timestamp
         */
        public boolean isApplicable(long tstamp) {
            return tstamp >= this.zoneTransitionStart && tstamp < this.zoneTransitionStop;
        }
        
        
        /**
         * Format the timestamp millis.
         * Note: you must first invoke {@link #isApplicable(long)} to check that the state can be used to format the timestamp.
         * 
         * @param tstamp the timestamp milis to format
         * @return the formatted timestamp
         */
        public String format(long tstamp) {
            // If caching is disabled...
            //
            if (!this.cachingEnabled) {
                return buildFromFormatter(tstamp);
            }
            
            // If tstamp is within the caching period...
            //
            TimestampCache currentCache = this.cache;
            if (currentCache != null && currentCache.isApplicable(tstamp)) {
                return buildFromCache(currentCache, tstamp);
            }
            
            // ... otherwise, use the formatter and cache the formatted value
            //
            String formatted = buildFromFormatter(tstamp);
            cache = createNewCache(tstamp, formatted);
            return formatted;
        }
        
        
        private TimestampCache createNewCache(long timestampInMillis, String formatted) {
            // The part up to the minutes (included)
            String prefix = formatted.substring(0, 17);
                
            // The part after the millis (i.e. the timezone)
            // Note:
            //   ISO_LOCAL_DATE_TIME has nothing after millis
            //   ISO_INSTANT has a 'Z'
            int pos = formatted.indexOf('+', 17);
            if (pos == -1) {
                pos = formatted.indexOf('-', 17);
            }
            if (pos == -1 && formatted.charAt(formatted.length() - 1) == 'Z') {
                pos = formatted.length() - 1;
            }
            String suffix = pos == -1 ? "" : formatted.substring(pos);
            
            // Determine how long we can use this cache
            long cachePeriod = timestampInMillis / 60_000;
            long cacheStart = cachePeriod * 60_000;
            long cacheStop = (cachePeriod + 1) * 60_000;

            // Store in cache
            return new TimestampCache(cacheStart, cacheStop, prefix, suffix);
        }
        
        
        private String buildFromCache(TimestampCache cache, long timestampInMillis) {
            return cache.format(timestampInMillis);
        }
    }
    
    String buildFromFormatter(long timestampInMillis) {
        return FastISOTimestampFormatter.this.formatter.format(Instant.ofEpochMilli(timestampInMillis));
    }
    
    private class TimestampCache {
        private final long cacheStart;
        private final long cacheStop;
        private final String suffix;
        private final String prefix;

        TimestampCache(long cacheStart, long cacheStop, String prefix, String suffix) {
            this.cacheStart = cacheStart;
            this.cacheStop = cacheStop;
            this.prefix = prefix;
            this.suffix = suffix;
        }
        
        public boolean isApplicable(long tstamp) {
            return tstamp >= this.cacheStart && tstamp < this.cacheStop;
        }
        
        public String format(long timestampInMillis) {
            StringBuilder sb = STRING_BUILDERS.get();
            sb.setLength(0);
            sb.append(prefix);
            
            int millisSinceCacheStart = (int) (timestampInMillis - this.cacheStart); // seconds & millis
            int seconds = millisSinceCacheStart / 1_000;
            int millis = millisSinceCacheStart % 1000;

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
