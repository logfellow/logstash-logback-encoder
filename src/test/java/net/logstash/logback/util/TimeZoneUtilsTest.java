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
package net.logstash.logback.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author brenuart
 *
 */
public class TimeZoneUtilsTest {

    @Test
    public void standardTimeZones() {
        assertThat(TimeZoneUtils.parseTimeZone("GMT").getID()).isEqualTo("GMT");
        assertThat(TimeZoneUtils.parseTimeZone("Europe/Brussels").getID()).isEqualTo("Europe/Brussels");
    }
    
    
    @Test
    public void customTimeZones() {
        assertThat(TimeZoneUtils.parseTimeZone("GMT+00").getID()).isEqualTo("GMT+00:00");
        assertThat(TimeZoneUtils.parseTimeZone("GMT+00:00").getID()).isEqualTo("GMT+00:00");

        assertThat(TimeZoneUtils.parseTimeZone("GMT-00").getID()).isEqualTo("GMT-00:00");
        assertThat(TimeZoneUtils.parseTimeZone("GMT-00:00").getID()).isEqualTo("GMT-00:00");

        assertThat(TimeZoneUtils.parseTimeZone("GMT+01:12").getID()).isEqualTo("GMT+01:12");
    }
    
    
    @Test
    public void invalidTimeZone() {
        assertThatThrownBy(() -> TimeZoneUtils.parseTimeZone("foo")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimeZoneUtils.parseTimeZone("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimeZoneUtils.parseTimeZone(null)).isInstanceOf(NullPointerException.class);
    }
}
