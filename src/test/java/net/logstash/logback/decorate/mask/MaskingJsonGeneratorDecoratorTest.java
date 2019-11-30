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
package net.logstash.logback.decorate.mask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;


public class MaskingJsonGeneratorDecoratorTest {

    private static final JsonFactory FACTORY = new MappingJsonFactory();

    @Test
    public void maskedAndUnmaskedField() throws IOException {
        testMaskByPath(
                "{\"fieldA\":\"valueA\",\"fieldB\":\"valueB\",\"fieldC\":\"valueC\"}",
                "{\"fieldA\":\"valueA\",\"fieldB\":\"****\",\"fieldC\":\"valueC\"}",
                "fieldB");
        testMaskByPath(
                "{\"fieldA\":\"valueA\",\"fieldB\":\"valueB\",\"fieldC\":\"valueC\"}",
                "{\"fieldA\":\"valueA\",\"fieldB\":\"****\",\"fieldC\":\"valueC\"}",
                "/fieldB");
        testMaskByPath(
                "{\"fieldA\":\"valueA\",\"fieldB\":\"valueB\",\"fieldC\":\"valueC\"}",
                "{\"fieldA\":\"valueA\",\"fieldB\":\"valueB\",\"fieldC\":\"valueC\"}",
                "foo/fieldB");
        testMaskByValue(
                "{\"fieldA\":\"valueA\",\"fieldB\":\"valueB\",\"fieldC\":\"valueC\"}",
                "{\"fieldA\":\"valueA\",\"fieldB\":\"****\",\"fieldC\":\"valueC\"}",
                "valueB");
        testMaskByValue(
                "{\"fieldA\":\"valueA\",\"fieldB\":\"valueB\",\"fieldC\":\"valueC\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"****\",\"fieldC\":\"****\"}",
                "value.");
    }

    @Test
    public void onlyMaskedField() throws IOException {
        testMaskByPath(
                "{\"fieldA\":\"valueA\"}",
                "{\"fieldA\":\"****\"}",
                "fieldA");
        testMaskByPath(
                "{\"fieldA\":\"valueA\"}",
                "{\"fieldA\":\"****\"}",
                "/fieldA");
        testMaskByPath(
                "{\"fieldA\":\"valueA\"}",
                "{\"fieldA\":\"valueA\"}",
                "foo/fieldA");
        testMaskByValue(
                "{\"fieldA\":\"valueA\"}",
                "{\"fieldA\":\"****\"}",
                "valueA");
    }

    @Test
    public void escapedPath() throws IOException {
        testMaskByPath(
                "{\"fieldA\":\"valueA\",\"field~/B\":\"valueB\",\"fieldC\":\"valueC\"}",
                "{\"fieldA\":\"valueA\",\"field~/B\":\"****\",\"fieldC\":\"valueC\"}",
                "field~0~1B");
    }

    @Test
    public void maskEverything() throws IOException {
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"****\"}",
                "*");
    }

    @Test
    public void maskAllStringValues() throws IOException {
        testMaskByValue(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\",\"fieldC\":1}",
                "{\"fieldA\":{\"fieldAA\":\"****\",\"fieldAB\":{\"fieldABA\":\"****\"},\"fieldAC\":[1]},\"fieldB\":\"****\",\"fieldC\":1}",
                "^.*$");
    }

    @Test
    public void maskedSubObject() throws IOException {
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"valueB\"}",
                "fieldA");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"valueB\"}",
                "/fieldA");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"****\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "fieldA/*/fieldABA");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "*/fieldA");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":\"****\",\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "*/fieldAB");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":{\"fieldABA\":\"valueABA\"},\"fieldAC\":[1]},\"fieldB\":\"valueB\"}",
                "foo/fieldA");
    }

    @Test
    public void subObjectWithMaskedField() throws IOException {
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":12345678,\"fieldAC\":\"valueAC\"}}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":\"****\",\"fieldAC\":\"valueAC\"}}",
                "fieldAB");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":12345678,\"fieldAC\":\"valueAC\"}}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":\"****\",\"fieldAC\":\"valueAC\"}}",
                "fieldA/fieldAB");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":12345678,\"fieldAC\":\"valueAC\"}}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":\"****\",\"fieldAC\":\"valueAC\"}}",
                "/fieldA/fieldAB");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":12345678,\"fieldAC\":\"valueAC\"}}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":\"****\",\"fieldAC\":\"valueAC\"}}",
                "/*/fieldAB");
        testMaskByPath(
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":12345678,\"fieldAC\":\"valueAC\"}}",
                "{\"fieldA\":{\"fieldAA\":\"valueAA\",\"fieldAB\":12345678,\"fieldAC\":\"valueAC\"}}",
                "foo/fieldA/fieldAB");
    }

    @Test
    public void maskedArray() throws IOException {
        testMaskByPath(
                "{\"fieldA\":[\"valueA0\",\"valueA1\"],\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"valueB\"}",
                "fieldA");
        testMaskByPath(
                "{\"fieldA\":[\"valueA0\",\"valueA1\"],\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"valueB\"}",
                "/fieldA");
        testMaskByPath(
                "{\"fieldA\":[\"valueA0\",\"valueA1\"],\"fieldB\":\"valueB\"}",
                "{\"fieldA\":[\"valueA0\",\"valueA1\"],\"fieldB\":\"valueB\"}",
                "foo/fieldA");
    }

    @Test
    public void maskedArrayOfObjects() throws IOException {
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\"},{\"fieldA1A\":\"valueA1A\"}],\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"valueB\"}",
                "fieldA");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\"},{\"fieldA1A\":\"valueA1A\"}],\"fieldB\":\"valueB\"}",
                "{\"fieldA\":\"****\",\"fieldB\":\"valueB\"}",
                "/fieldA");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\"},{\"fieldA1A\":\"valueA1A\"}],\"fieldB\":\"valueB\"}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\"},{\"fieldA1A\":\"valueA1A\"}],\"fieldB\":\"valueB\"}",
                "foo/fieldA");
    }

    @Test
    public void arrayOfObjectWithMaskedField() throws IOException {
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":\"****\",\"fieldA0C\":\"valueA0C\"}]}",
                "fieldA0B");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":\"****\",\"fieldA0C\":\"valueA0C\"}]}",
                "0/fieldA0B");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":\"****\",\"fieldA0C\":\"valueA0C\"}]}",
                "fieldA/0/fieldA0B");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":\"****\",\"fieldA0C\":\"valueA0C\"}]}",
                "/fieldA/0/fieldA0B");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":\"****\",\"fieldA0C\":\"valueA0C\"}]}",
                "/fieldA/*/fieldA0B");
        testMaskByPath(
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "{\"fieldA\":[{\"fieldA0A\":\"valueA0A\",\"fieldA0B\":12345678,\"fieldA0C\":\"valueA0C\"}]}",
                "foo/fieldA/0/fieldA0B");
    }

    @Test
    public void arrayOfArrayOfObjectWithMaskedFields() throws IOException {
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "0/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "0/0/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "fieldA/0/0/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "fieldA/0/*/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "*/0/*/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "/fieldA/0/0/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "/fieldA/*/0/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":\"****\",\"fieldA00C\":\"valueA00C\"}]]}",
                "/*/*/0/fieldA00B");
        testMaskByPath(
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "{\"fieldA\":[[{\"fieldA00A\":\"valueA00A\",\"fieldA00B\":true,\"fieldA00C\":\"valueA00C\"}]]}",
                "foo/fieldA/0/0/fieldA00B");
    }

    private void testMaskByPath(String unmasked, String masked, String... pathsToMask) throws IOException  {
        MaskingJsonGeneratorDecorator decoratorByPath = new MaskingJsonGeneratorDecorator();
        Arrays.stream(pathsToMask).forEach(decoratorByPath::addPath);
        test(unmasked, masked, decoratorByPath);

        MaskingJsonGeneratorDecorator decoratorByPathWithDifferentDefault = new MaskingJsonGeneratorDecorator();
        decoratorByPathWithDifferentDefault.setDefaultmask("[masked]");
        Arrays.stream(pathsToMask).forEach(decoratorByPathWithDifferentDefault::addPath);
        test(unmasked, masked.replace(MaskingJsonGenerator.MASK, "[masked]"), decoratorByPathWithDifferentDefault);

        MaskingJsonGeneratorDecorator decoratorByPathMask = new MaskingJsonGeneratorDecorator();
        decoratorByPathMask.setDefaultmask("foo");
        Arrays.stream(pathsToMask).forEach(pathToMask -> decoratorByPathMask.addPathMask(new MaskingJsonGeneratorDecorator.PathMask(pathToMask, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByPathMask);

        MaskingJsonGeneratorDecorator decoratorByPathMasker = new MaskingJsonGeneratorDecorator();
        decoratorByPathMasker.setDefaultmask("foo");
        Arrays.stream(pathsToMask).forEach(pathToMask -> decoratorByPathMasker.addFieldMasker(new PathBasedFieldMasker(pathToMask, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByPathMasker);

    }

    private void testMaskByValue(String unmasked, String masked, String... valuesToMask) throws IOException  {
        MaskingJsonGeneratorDecorator decoratorByValue = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(decoratorByValue::addValue);
        test(unmasked, masked, decoratorByValue);

        MaskingJsonGeneratorDecorator decoratorByValueWithDifferentDefault = new MaskingJsonGeneratorDecorator();
        decoratorByValueWithDifferentDefault.setDefaultmask("[masked]");
        Arrays.stream(valuesToMask).forEach(decoratorByValueWithDifferentDefault::addValue);
        test(unmasked, masked.replace(MaskingJsonGenerator.MASK, "[masked]"), decoratorByValueWithDifferentDefault);

        MaskingJsonGeneratorDecorator decoratorByValueMask = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(value -> decoratorByValueMask.addValueMask(new MaskingJsonGeneratorDecorator.ValueMask(value, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByValueMask);

        MaskingJsonGeneratorDecorator decoratorByValueMasker = new MaskingJsonGeneratorDecorator();
        Arrays.stream(valuesToMask).forEach(value -> decoratorByValueMasker.addValueMasker(new RegexValueMasker(value, MaskingJsonGenerator.MASK)));
        test(unmasked, masked, decoratorByValueMasker);
    }

    private void test(String unmasked, String masked, MaskingJsonGeneratorDecorator decorator) throws IOException {
        decorator.start();

        StringWriter maskedWriter = new StringWriter();
        JsonGenerator maskingGenerator = decorator.decorate(FACTORY.createGenerator(maskedWriter));

        /*
         * Read through the unmasked string, while writing to the maskedWriter
         */
        JsonParser parser = FACTORY.createParser(new StringReader(unmasked));

        while (parser.nextToken() != null) {
            maskingGenerator.copyCurrentEvent(parser);
        }

        maskingGenerator.flush();

        assertThat(maskedWriter.toString()).isEqualTo(masked);
    }

}
