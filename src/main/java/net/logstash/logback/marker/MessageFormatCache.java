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
package net.logstash.logback.marker;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache for {@link MessageFormat} objects.
 * <p>
 *
 * Since only a small subset of {@link MessageFormat}s are generally used by
 * {@link SingleFieldAppendingMarker}, the {@link MessageFormatCache} will
 * cache them (per thread) so that they can be reused.
 * <p>
 *
 * This is a performance optimization to save {@link MessageFormat} construction and parsing time
 * for each argument/marker.
 *
 */
public class MessageFormatCache {

    public static final MessageFormatCache INSTANCE = new MessageFormatCache();

    /**
     * Use a {@link ThreadLocal} cache, since {@link MessageFormat}s are not threadsafe.
     */
    private ThreadLocal<Map<String, MessageFormat>> messageFormats = new ThreadLocal<Map<String, MessageFormat>>() {
        protected Map<String, MessageFormat> initialValue() {
            return new HashMap<String, MessageFormat>();
        };
    };

    public MessageFormat getMessageFormat(String formatPattern) {
        Map<String, MessageFormat> messageFormatsForCurrentThread = messageFormats.get();
        MessageFormat messageFormat = messageFormatsForCurrentThread.get(formatPattern);
        if (messageFormat == null) {
            messageFormat = new MessageFormat(formatPattern);
            messageFormatsForCurrentThread.put(formatPattern, messageFormat);
        }
        return messageFormat;
    }
}
