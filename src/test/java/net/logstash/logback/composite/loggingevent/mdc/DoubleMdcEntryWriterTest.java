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
class DoubleMdcEntryWriterTest {

    private final MdcEntryWriter mdcEntryWriter = new DoubleMdcEntryWriter();

    @Mock
    private JsonGenerator generator;

    @ParameterizedTest
    @ValueSource(strings = {
            "-3.14159265",
            "+314.59265e-2",
            "4711",
            "0",
            "-0.0",
            "2.71827f",
            "2.71827d",
            "2.2250738585072014E-308",
            "1.7976931348623157E+308"
    })
    void valid(String value) throws IOException {
        boolean result = mdcEntryWriter.writeMdcEntry(generator, "otherName", "name", value);

        assertThat(result).isTrue();
        verify(generator).writeFieldName("otherName");
        verify(generator).writeNumber(Double.parseDouble(value));
        verifyNoMoreInteractions(generator);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2.71827*3",
            "2.71827g",
            "text",
            "NaN",
            "Infinity",
            "-Infinity",
            ""
    })
    void invalid(String value) throws IOException {
        boolean result = mdcEntryWriter.writeMdcEntry(generator, "otherName", "name", value);

        assertThat(result).isFalse();
        verifyNoInteractions(generator);
    }

}
