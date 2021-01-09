/**
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
package net.logstash.logback.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.io.IOException;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import net.logstash.logback.composite.CompositeJsonFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CompositeJsonLayoutTest {

    /**
     * create an implementation of the composite layout that format teh event using simply toString()
     */
    static class TesterCompositeJsonLayout extends CompositeJsonLayout {
        @Override
        protected CompositeJsonFormatter createFormatter() {
            return new CompositeJsonFormatter(this){
                @Override
                public String writeEventAsString(DeferredProcessingAware deferredProcessingAware) throws IOException {
                    return deferredProcessingAware.toString();
                }
            };
        }
    }

    @Mock(lenient = true)
    DeferredProcessingAware event;

    @Mock(lenient = true)
    Layout prefixLayout;

    @Mock(lenient = true)
    Layout suffixLayout;

    @BeforeEach
    public void setup() {
        when(event.toString()).thenReturn("event");
        when(prefixLayout.doLayout(event)).thenReturn("prefix:");
        when(suffixLayout.doLayout(event)).thenReturn(":suffix");
    }

    @Test
    public void testDoLayoutWithoutPrefixSuffix()  {

        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("event");

    }

    @Test
    public void testDoLayoutWithPrefixWithoutSuffix() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setPrefix(prefixLayout);
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("prefix:event");

    }


    @Test
    public void testDoLayoutWithoutPrefixWithSuffix() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setSuffix(suffixLayout);
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("event:suffix");

    }


    @Test
    public void testDoLayoutWithPrefixWithSuffix() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setPrefix(prefixLayout);
        layout.setSuffix(suffixLayout);
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("prefix:event:suffix");

    }

    @Test
    public void testDoLayoutWithPrefixWithLineSeparator() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setLineSeparator("SYSTEM");
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("event" + System.lineSeparator());
    }

    @Test
    public void notStarted() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> layout.doLayout(event))
                .withMessage("Layout is not started");
    }

}