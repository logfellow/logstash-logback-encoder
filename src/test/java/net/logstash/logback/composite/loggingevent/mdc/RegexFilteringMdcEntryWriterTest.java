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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegexFilteringMdcEntryWriterTest {

    private final RegexFilteringMdcEntryWriter mdcEntryWriter = new RegexFilteringMdcEntryWriter();

    @Mock
    private JsonGenerator generator;
    @Mock
    private MdcEntryWriter mockedMdcEntryWriter;

    @Test
    void noIncludeAndNoExcludePattern() throws IOException {
        mockMdcEntryWriter();

        boolean result = mdcEntryWriter.writeMdcEntry(generator, "field", "key", "value");

        assertThat(result).isTrue();
        verify(mockedMdcEntryWriter).writeMdcEntry(generator, "field", "key", "value");
        verifyNoMoreInteractions(mockedMdcEntryWriter);
    }

    @Test
    void noMdcEntryWriter() throws IOException {
        boolean result = mdcEntryWriter.writeMdcEntry(generator, "field", "key", "value");

        assertThat(result).isFalse();
        verifyNoMoreInteractions(mockedMdcEntryWriter);
    }

    @ParameterizedTest
    @CsvSource({
            "include,excl.*,true",
            "include,.*lude,false",
            "exclude,excl.*,false",
            "other,excl.*,false"
    })
    void includeAndExcludePattern(String mdcKey, String excludePattern, boolean entryWritten) throws IOException {
        if (entryWritten) {
            mockMdcEntryWriter();
        }
        mdcEntryWriter.setIncludeMdcKeyPattern("incl.*");
        mdcEntryWriter.setExcludeMdcKeyPattern(excludePattern);

        boolean result = mdcEntryWriter.writeMdcEntry(generator, "field", mdcKey, "value");

        assertThat(result).isEqualTo(entryWritten);
        if (entryWritten) {
            verify(mockedMdcEntryWriter).writeMdcEntry(generator, "field", mdcKey, "value");
        }
        verifyNoMoreInteractions(mockedMdcEntryWriter);
    }

    private void mockMdcEntryWriter() throws IOException {
        when(mockedMdcEntryWriter.writeMdcEntry(eq(generator), anyString(), anyString(), anyString())).thenReturn(true);
        mdcEntryWriter.addMdcEntryWriter(mockedMdcEntryWriter);
    }

}
