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

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import junit.framework.TestCase;
import net.logstash.logback.composite.CompositeJsonFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CompositeJsonLayoutTest extends TestCase {

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

    @Mock
    DeferredProcessingAware event;

    @Mock
    Layout prefixLayout;

    @Mock
    Layout suffixLayout;

    @Before
    public void setup() {
        when(event.toString()).thenReturn("event");
        when(prefixLayout.doLayout(event)).thenReturn("prefix:");
        when(suffixLayout.doLayout(event)).thenReturn(":suffix");
    }

    @Test
    public void testDoLayoutWithoutPrefixSuffix()  {

        CompositeJsonLayout layout = new TesterCompositeJsonLayout();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("event");

    }

    @Test
    public void testDoLayoutWithPrefixWithoutSuffix() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setPrefix(prefixLayout);

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("prefix:event");

    }


    @Test
    public void testDoLayoutWithoutPrefixWithSuffix() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setSuffix(suffixLayout);

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("event:suffix");

    }


    @Test
    public void testDoLayoutWithPrefixWithSuffix() {
        CompositeJsonLayout layout = new TesterCompositeJsonLayout();
        layout.setPrefix(prefixLayout);
        layout.setSuffix(suffixLayout);

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("prefix:event:suffix");

    }
}