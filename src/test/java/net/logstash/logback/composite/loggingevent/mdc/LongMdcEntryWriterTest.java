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
package net.logstash.logback.composite.loggingevent.mdc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LongMdcEntryWriterTest {

    private final MdcEntryWriter mdcEntryWriter = new LongMdcEntryWriter();

    @Mock
    private JsonGenerator generator;

    @ParameterizedTest
    @ValueSource(strings = {
            "4711",
            "-0815",
            "+1234",
            "0",
            "-9223372036854775808",
            "9223372036854775807"
    })
    void valid(String value) throws IOException {
        boolean result = mdcEntryWriter.writeMdcEntry(generator, "otherName", "name", value);

        assertThat(result).isTrue();
        verify(generator).writeFieldName("otherName");
        verify(generator).writeNumber(Long.parseLong(value));
        verifyNoMoreInteractions(generator);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4711L",
            "0xBAD",
            "471.1",
            "0.0",
            "-9223372036854775808=9",
            "9223372036854775808",
            "text",
            "-",
            ""
    })
    void invalid(String value) throws IOException {
        boolean result = mdcEntryWriter.writeMdcEntry(generator, "otherName", "name", value);

        assertThat(result).isFalse();
        verifyNoInteractions(generator);
    }

}
