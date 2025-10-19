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
package net.logstash.logback.mask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;

import net.logstash.logback.composite.JsonReadingUtils;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import net.logstash.logback.encoder.CompositeJsonEncoder;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.LifeCycle;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;


public class MaskingJsonGeneratorDecoratorTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    public static class TestFieldMasker implements FieldMasker {

        @Override
        public Object mask(TokenStreamContext context) {
            return context.hasCurrentName() && context.currentName().equals("testfield")
                    ? "[maskedtestfield]"
                    : null;
        }
    }
    public static class TestValueMasker implements ValueMasker {

        @Override
        public Object mask(TokenStreamContext context, Object value) {
            return "testvalue".equals(value)
                    ? "[maskedtestvalue]"
                    : null;
        }
    }

    public static class TestFieldMaskSupplier implements MaskingJsonGeneratorDecorator.PathMaskSupplier {

        @Override
        public MaskingJsonGeneratorDecorator.PathMask get() {
            return new MaskingJsonGeneratorDecorator.PathMask("fieldF");
        }
    }

    public static class TestValueMaskSupplier implements MaskingJsonGeneratorDecorator.ValueMaskSupplier {

        @Override
        public MaskingJsonGeneratorDecorator.ValueMask get() {
            return new MaskingJsonGeneratorDecorator.ValueMask("value6");
        }
    }

    private JsonGeneratorDecorator configure(String file) {
        LoggerContext loggerContext = new LoggerContext();
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(loggerContext);
        try (InputStream is = getClass().getResourceAsStream("/" + getClass().getName().replaceAll("\\.", "/") + "-" + file + ".xml")) {
            jc.doConfigure(is);
        } catch (IOException | JoranException e) {
            throw new RuntimeException(e);
        }
        Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) logger.getAppender("APPENDER");
        CompositeJsonEncoder<ILoggingEvent> encoder = (CompositeJsonEncoder<ILoggingEvent>) appender.getEncoder();
        return encoder.getJsonGeneratorDecorator();
    }

    @Test
    public void maskedAndUnmaskedField() {
        testMaskByPath(
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC'}",
                "{'fieldA':'valueA','fieldB':'****',  'fieldC':'valueC'}",
                "fieldB");
        testMaskByPath(
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC'}",
                "{'fieldA':'valueA','fieldB':'****',  'fieldC':'valueC'}",
                "/fieldB");
        testMaskByPath(
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC'}",
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC'}",
                "foo/fieldB");
        testMaskByValue(
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC'}",
                "{'fieldA':'valueA','fieldB':'****',  'fieldC':'valueC'}",
                "valueB");
        testMaskByValue(
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC'}",
                "{'fieldA':'****'  ,'fieldB':'****',  'fieldC':'****'  }",
                "value.");
    }

    @Test
    public void maskedSubstrings() {
        testMaskByValue(
                "{'fieldA':'tomask1','fieldB':'tomask2','fieldC':' tomask1-tomask2 '}",
                "{'fieldA':'****',   'fieldB':'****',   'fieldC':' ****-**** '      }",
                "tomask1", "tomask2");
    }

    @Test
    public void onlyMaskedField() {
        testMaskByPath(
                "{'fieldA':'valueA'}",
                "{'fieldA':'****'  }",
                "fieldA");
        testMaskByPath(
                "{'fieldA':'valueA'}",
                "{'fieldA':'****'  }",
                "/fieldA");
        testMaskByPath(
                "{'fieldA':'valueA'}",
                "{'fieldA':'valueA'}",
                "foo/fieldA");
        testMaskByValue(
                "{'fieldA':'valueA'}",
                "{'fieldA':'****'  }",
                "valueA");
    }

    @Test
    public void escapedPath() {
        testMaskByPath(
                "{'fieldA':'valueA', 'field~/B':'valueB', 'fieldC':'valueC'}",
                "{'fieldA':'valueA', 'field~/B':'****',   'fieldC':'valueC'}",
                "field~0~1B");
    }

    @Test
    public void maskEverything() {
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]}, 'fieldB':'valueB'}",
                "{'fieldA':'****',                                                                'fieldB':'****'}",
                "*");
    }

    @Test
    public void maskNothing() {
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}");
    }

    @Test
    public void configuration() {
        JsonGeneratorDecorator noMasks = configure("noMasks");
        test(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                noMasks);


        JsonGeneratorDecorator masks = configure("masks");
        test(
                "{'fieldA':'valueA','fieldB':'valueB','fieldC':'valueC','fieldD':'valueD','fieldE':'valueE',  'fieldF':'valueF','testfield':'valueE'           }",
                "{'fieldA':'****',  'fieldB':'****',  'fieldC':'****',  'fieldD':'****',  'fieldE':'[masked]','fieldF':'****',  'testfield':'[maskedtestfield]'}",
                masks);
        test(
                "{'field0':'value0','field1':'value1','field2':'value2','field3':'value3','field-Z':'value-Z',     'field4':'value4','field5':'testvalue',        'field6':'value6','field7':'nestedvalue1-nestedvalue1'}",
                "{'field0':'****',  'field1':'****',  'field2':'****',  'field3':'****',  'field-Z':'value-masked','field4':'value4','field5':'[maskedtestvalue]','field6':'****',  'field7':'****-****'                }",
                masks);
    }

    @Test
    public void maskAllStringValues() {
        testMaskByValue(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB','fieldC':1}",
                "{'fieldA':{'fieldAA':'****',   'fieldAB':{'fieldABA':'****'    },'fieldAC':[1]},'fieldB':'****',  'fieldC':1}",
                "^.*$");
    }

    @Test
    public void maskedSubObject() {
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB','fieldC':{'fieldA':''    }}",
                "{'fieldA':'****',                                                               'fieldB':'valueB','fieldC':{'fieldA':'****'}}",
                "fieldA");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB','fieldC':{'fieldA':''}}",
                "{'fieldA':'****',                                                               'fieldB':'valueB','fieldC':{'fieldA':''}}",
                "/fieldA");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'****'    },'fieldAC':[1]},'fieldB':'valueB'}",
                "fieldA/*/fieldABA");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "*/fieldA");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':'****',                 'fieldAC':[1]},'fieldB':'valueB'}",
                "*/fieldAB");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':{'fieldABA':'valueABA'},'fieldAC':[1]},'fieldB':'valueB'}",
                "foo/fieldA");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA'}}",
                "{'fieldA':{'fieldAA':'****'   }}",
                "fieldAA");
    }

    @Test
    public void subObjectWithMaskedField() {
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'},'fieldAB':0}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':'****',  'fieldAC':'valueAC'},'fieldAB':'****'}",
                "fieldAB");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'},'fieldAB':0}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'},'fieldAB':'****'}",
                "/fieldAB");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'},'fieldAB':0}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':'****',  'fieldAC':'valueAC'},'fieldAB':0}",
                "fieldA/fieldAB");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'},'fieldAB':0}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':'****',  'fieldAC':'valueAC'},'fieldAB':0}",
                "/fieldA/fieldAB");
        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'},'fieldAB':0}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':'****',  'fieldAC':'valueAC'},'fieldAB':0}",
                "/*/fieldAB");

        testMaskByPath(
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'}}",
                "{'fieldA':{'fieldAA':'valueAA','fieldAB':12345678,'fieldAC':'valueAC'}}",
                "foo/fieldA/fieldAB");
    }

    @Test
    public void maskedArray() {
        testMaskByPath(
                "{'fieldA':['valueA0','valueA1'],'fieldB':'valueB'}",
                "{'fieldA':'****',               'fieldB':'valueB'}",
                "fieldA");
        testMaskByPath(
                "{'fieldA':['valueA0','valueA1'],'fieldB':'valueB'}",
                "{'fieldA':'****',               'fieldB':'valueB'}",
                "/fieldA");
        testMaskByPath(
                "{'fieldA':['valueA0','valueA1'],'fieldB':'valueB'}",
                "{'fieldA':['valueA0','valueA1'],'fieldB':'valueB'}",
                "foo/fieldA");
    }

    @Test
    public void masedArrayByIndex() {
        testMaskByPath(
                "{ 'array':[{'foo':'bar' },{'a':'b'}] }",
                "{ 'array':[{'foo':'****'},{'a':'b'}] }",
                "/array/0/foo"
                );

        testMaskByPath(
                "{ 'array':[{'foo':'bar' },{'a':'b'   }] }",
                "{ 'array':[{'foo':'bar' },{'a':'****'}] }",
                "/array/1/a"
                );
        
        
        // Failed tests - see issue #735
        
        assertThatThrownBy(() ->
            testMaskByPath(
                    "{ 'array':['a','b',   'c'] }",
                    "{ 'array':['a','****','c'] }",
                    "/array/1"
                    )).isInstanceOf(AssertionFailedError.class);
        
        assertThatThrownBy(() ->
            testMaskByPath(
                    "{ 'array':[{'foo':'bar'},{'a':'b'}] }",
                    "{ 'array':['****'       ,{'a':'b'}] }",
                    "/array/0"
                    )).isInstanceOf(AssertionFailedError.class);
    }
    
    @Test
    public void maskedArrayOfObjects() {
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A'},{'fieldA1A':'valueA1A'}],'fieldB':'valueB'}",
                "{'fieldA':'****',                                           'fieldB':'valueB'}",
                "fieldA");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A'},{'fieldA1A':'valueA1A'}],'fieldB':'valueB'}",
                "{'fieldA':'****',                                           'fieldB':'valueB'}",
                "/fieldA");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A'},{'fieldA1A':'valueA1A'}],'fieldB':'valueB'}",
                "{'fieldA':[{'fieldA0A':'valueA0A'},{'fieldA1A':'valueA1A'}],'fieldB':'valueB'}",
                "foo/fieldA");
    }

    @Test
    public void arrayOfObjectWithMaskedField() {
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':'****',  'fieldA0C':'valueA0C'}]}",
                "fieldA0B");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':'****',  'fieldA0C':'valueA0C'}]}",
                "0/fieldA0B");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':'****',  'fieldA0C':'valueA0C'}]}",
                "fieldA/0/fieldA0B");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':'****',  'fieldA0C':'valueA0C'}]}",
                "/fieldA/0/fieldA0B");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':'****',  'fieldA0C':'valueA0C'}]}",
                "/fieldA/*/fieldA0B");
        testMaskByPath(
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "{'fieldA':[{'fieldA0A':'valueA0A','fieldA0B':12345678,'fieldA0C':'valueA0C'}]}",
                "foo/fieldA/0/fieldA0B");
    }

    @Test
    public void arrayOfArrayOfObjectWithMaskedFields() {
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "0/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "0/0/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "fieldA/0/0/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "fieldA/0/*/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "*/0/*/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "/fieldA/0/0/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "/fieldA/*/0/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,  'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':'****','fieldA00C':'valueA00C'}]]}",
                "/*/*/0/fieldA00B");
        testMaskByPath(
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,'fieldA00C':'valueA00C'}]]}",
                "{'fieldA':[[{'fieldA00A':'valueA00A','fieldA00B':true,'fieldA00C':'valueA00C'}]]}",
                "foo/fieldA/0/0/fieldA00B");
    }

    private void testMaskByPath(String unmasked, String masked, String... pathsToMask) {
        MaskingJsonGeneratorDecorator decoratorByPath = new MaskingJsonGeneratorDecorator();
        Arrays.stream(pathsToMask).forEach(decoratorByPath::addPath);
        test(unmasked, masked, decoratorByPath);

        MaskingJsonGeneratorDecorator decoratorByPaths = new MaskingJsonGeneratorDecorator();
        decoratorByPaths.addPaths(String.join(",", pathsToMask));
        test(unmasked, masked, decoratorByPaths);

        MaskingJsonGeneratorDecorator decoratorByPathWithDifferentDefault = new MaskingJsonGeneratorDecorator();
        decoratorByPathWithDifferentDefault.setDefaultMask("[masked]");
        Arrays.stream(pathsToMask).forEach(decoratorByPathWithDifferentDefault::addPath);
        test(unmasked, masked.replace(MaskingJsonGenerator.MASK, "[masked]"), decoratorByPathWithDifferentDefault);

        MaskingJsonGeneratorDecorator decoratorByPathMask = new MaskingJsonGeneratorDecorator();
        decoratorByPathMask.setDefaultMask("foo");
        Arrays.stream(pathsToMask).forEach(pathToMask -> decoratorByPathMask.addPathMask(new MaskingJsonGeneratorDecorator.PathMask(pathToMask, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByPathMask);

        MaskingJsonGeneratorDecorator decoratorByPathMaskProvider = new MaskingJsonGeneratorDecorator();
        decoratorByPathMaskProvider.setDefaultMask("foo");
        Arrays.stream(pathsToMask).forEach(pathToMask -> decoratorByPathMaskProvider.addPathMaskSupplier(() -> new MaskingJsonGeneratorDecorator.PathMask(pathToMask, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByPathMaskProvider);

        MaskingJsonGeneratorDecorator decoratorByPathMasker = new MaskingJsonGeneratorDecorator();
        decoratorByPathMasker.setDefaultMask("foo");
        Arrays.stream(pathsToMask).forEach(pathToMask -> decoratorByPathMasker.addFieldMasker(new PathBasedFieldMasker(pathToMask, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByPathMasker);

    }

    private void testMaskByValue(String unmasked, String masked, String... valuesToMask) {
        MaskingJsonGeneratorDecorator decoratorByValue = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(decoratorByValue::addValue);
        test(unmasked, masked, decoratorByValue);

        MaskingJsonGeneratorDecorator decoratorByValues = new MaskingJsonGeneratorDecorator();
        decoratorByValues.addValues(String.join(",", valuesToMask));
        test(unmasked, masked, decoratorByValues);

        MaskingJsonGeneratorDecorator decoratorByValueWithDifferentDefault = new MaskingJsonGeneratorDecorator();
        decoratorByValueWithDifferentDefault.setDefaultMask("[masked]");
        Arrays.stream(valuesToMask).forEach(decoratorByValueWithDifferentDefault::addValue);
        test(unmasked, masked.replace(MaskingJsonGenerator.MASK, "[masked]"), decoratorByValueWithDifferentDefault);

        MaskingJsonGeneratorDecorator decoratorByValueMask = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(value -> decoratorByValueMask.addValueMask(new MaskingJsonGeneratorDecorator.ValueMask(value, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByValueMask);

        MaskingJsonGeneratorDecorator decoratorByValueMaskProvider = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(value -> decoratorByValueMaskProvider.addValueMaskSupplier(() -> new MaskingJsonGeneratorDecorator.ValueMask(value, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByValueMaskProvider);

        MaskingJsonGeneratorDecorator decoratorByValueMasker = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(value -> decoratorByValueMasker.addValueMasker(new RegexValueMasker(value, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByValueMasker);
    }

    @Test
    public void testReplacementGroup() {
        MaskingJsonGeneratorDecorator decorator = new MaskingJsonGeneratorDecorator();
        decorator.addValueMask(new MaskingJsonGeneratorDecorator.ValueMask("(hello)? world", "$1 bob"));
        test("{'field':'hello world'}", "{'field':'hello bob'}", decorator);
    }

    private void test(String unmasked, String masked, JsonGeneratorDecorator decorator) {
        if (decorator instanceof LifeCycle) {
            ((LifeCycle) decorator).start();
        }

        unmasked = toJson(unmasked);
        masked = toJson(masked);
        
        StringWriter maskedWriter = new StringWriter();
        JsonGenerator maskingGenerator = decorator.decorate(MAPPER.createGenerator(maskedWriter));

        /*
         * Read through the unmasked string, while writing to the maskedWriter
         * Note: replay read/parser events directly on the generator to simulate calls to the generator methods
         */
        try (JsonParser parser = MAPPER.createParser(unmasked)) {
            while (parser.nextToken() != null) {
                maskingGenerator.copyCurrentEvent(parser);
            }
            maskingGenerator.flush();
        }
        
        /*
         * Input strings may be formatted with extra blanks for convenience in the test.
         * Better to convert them into ObjectNodes to compare their actual JSON structure irrespective of the "pretty printing".
         */
        ObjectNode expected = JsonReadingUtils.readFullyAsObjectNode(MAPPER, masked);
        ObjectNode actual   = JsonReadingUtils.readFullyAsObjectNode(MAPPER, maskedWriter.toString());
        assertThat(actual).isEqualTo(expected);
    }
    
    
    private static String toJson(String str) {
        return str.replace("'", "\"");
    }
}
