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
package net.logstash.logback.mask;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonStreamContext;
import org.atteo.classindex.ClassIndex;


/**
 * A FieldMasker for use by {@link net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder}.
 * It will examine all the classes annotated with {@link LogMaskFields} for all the fields annotated with {@link LogMask}
 * under the start package and mask them.
 */
public class AnnotatedFieldMasker implements FieldMasker {

    final Map<String, String> fieldMasks = new HashMap<>();

    public AnnotatedFieldMasker() {
        ClassIndex.getAnnotated(LogMaskFields.class).forEach(this::examineClass);
    }

    private void examineClass(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            LogMask logMask = field.getAnnotation(LogMask.class);
            if (null != logMask) {
                final String fieldName = field.getName();
                fieldMasks.put(null == logMask.fieldName() || logMask.fieldName().trim().isEmpty() ? fieldName : logMask.fieldName(), logMask.maskValue());
            }
        }
    }

    @Override
    public Object mask(JsonStreamContext context) {
        return context.hasCurrentName() ? fieldMasks.get(context.getCurrentName()) : null;
    }
}
