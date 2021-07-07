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
package net.logstash.logback.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.CompositeJsonFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;

@ExtendWith(MockitoExtension.class)
public class CompositeJsonLayoutTest {

    /**
     * create an implementation of the composite layout that format teh event using simply toString()
     */
    static class TesterCompositeJsonLayout extends CompositeJsonLayout<DeferredProcessingAware> {
        @Override
        protected CompositeJsonFormatter<DeferredProcessingAware> createFormatter() {
            CompositeJsonFormatter<DeferredProcessingAware> formatter = new CompositeJsonFormatter<DeferredProcessingAware>(this) {};
            formatter.getProviders().addProvider(new AbstractJsonProvider<DeferredProcessingAware>() {
                @Override
                public void writeTo(JsonGenerator generator, DeferredProcessingAware event) throws IOException {
                    generator.writeRaw("event");
                }
                @Override
                public void prepareForDeferredProcessing(DeferredProcessingAware event) {
                    super.prepareForDeferredProcessing(event);
                }
            });
            return formatter;
        }
    }

    private Layout<DeferredProcessingAware> prefixLayout = new LayoutBase<DeferredProcessingAware>() {
        @Override
        public String doLayout(DeferredProcessingAware event) {
            return "prefix:";
        }  
    };

    private Layout<DeferredProcessingAware> suffixLayout = new LayoutBase<DeferredProcessingAware>() {
        public String doLayout(DeferredProcessingAware event) {
            return ":suffix";
        };
    };

    @Mock(lenient = true)
    private DeferredProcessingAware event;
    

    @Test
    public void testDoLayoutWithoutPrefixSuffix()  {
        CompositeJsonLayout<DeferredProcessingAware> layout = new TesterCompositeJsonLayout();
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("{event}");

    }

    @Test
    public void testDoLayoutWithPrefixWithoutSuffix() {
        CompositeJsonLayout<DeferredProcessingAware> layout = new TesterCompositeJsonLayout();
        layout.setPrefix(prefixLayout);
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("prefix:{event}");

    }


    @Test
    public void testDoLayoutWithoutPrefixWithSuffix() {
        CompositeJsonLayout<DeferredProcessingAware> layout = new TesterCompositeJsonLayout();
        layout.setSuffix(suffixLayout);
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("{event}:suffix");

    }


    @Test
    public void testDoLayoutWithPrefixWithSuffix() {
        CompositeJsonLayout<DeferredProcessingAware> layout = new TesterCompositeJsonLayout();
        layout.setPrefix(prefixLayout);
        layout.setSuffix(suffixLayout);
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("prefix:{event}:suffix");

    }

    @Test
    public void testDoLayoutWithPrefixWithLineSeparator() {
        CompositeJsonLayout<DeferredProcessingAware> layout = new TesterCompositeJsonLayout();
        layout.setLineSeparator("SYSTEM");
        layout.start();

        String layoutResult = layout.doLayout(event);

        assertThat(layoutResult).isEqualTo("{event}" + System.lineSeparator());
    }

    @Test
    public void notStarted() {
        CompositeJsonLayout<DeferredProcessingAware> layout = new TesterCompositeJsonLayout();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> layout.doLayout(event))
                .withMessage("Layout is not started");
    }

}