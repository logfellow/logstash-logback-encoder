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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by {@link AnnotatedFieldMasker} to mark a field for masking
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LogMask {
    /**
     * The field name to mask.  Leave blank unless changing the value of the field through @JsonProperty annotation.
     * @return the name of the field to mask
     */
    String fieldName() default "";


    /**
     * The string that will be substituted when the field name appears stream
     * @return the value that will be used in the  masked result
     */
    String maskValue() default "REDACTED";
}
