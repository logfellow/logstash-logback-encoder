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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import tools.jackson.core.Base64Variant;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.util.JsonGeneratorDelegate;


/**
 * A {@link JsonGenerator} that can mask sensitive field values before delegating to a delegate {@link JsonGenerator}.
 *
 * <h2>Identifying field values to mask by <em>path</em></h2>
 *
 * The {@code MaskingJsonGenerator} uses a collection of {@link FieldMasker}s
 * to identify fields to mask by <em>JSON path</em>.
 *
 * <p>These field maskers are invoked after a new field name is written
 * or before an array element is written, to determine if the value should be masked.
 * If any masker returns a non-null value, then the returned value will
 * be written as the field's value (instead of the original field value).
 * Note that the masked value's type might differ from the fields original type.
 *
 * <p>Example {@link FieldMasker}s:
 *
 * <ul>
 *     <li>{@link FieldNameBasedFieldMasker}</li>
 *     <li>{@link PathBasedFieldMasker}</li>
 * </ul>
 *
 * <h2>Identifying field values to mask by <em>value</em></h2>
 *
 * The {@code MaskingJsonGenerator} uses a collection of {@link ValueMasker}s
 * to mask field values by <em>JSON path and field value</em>.
 *
 * <p>These value maskers are invoked each time a new number or string scalar value
 * is written to determine if the value should be masked.
 * If all maskers return null, then the value is written as-is (i.e. not masked).
 * If any masker returns non-null, then the returned value
 * will be written as the field value.
 *
 * <p>Raw values are NOT masked.
 *
 * <p>It is much more efficient to mask field values by <em>path</em>, rather than by <em>field value</em>.
 * Therefore, prefer using {@link FieldMasker}s instead of {@link ValueMasker}s whenever possible.
 *
 * <p>Example value maskers:
 *
 * <ul>
 *     <li>{@link RegexValueMasker}</li>
 * </ul>
 */
public class MaskingJsonGenerator extends JsonGeneratorDelegate {

    public static final String MASK = "****";

    private final Collection<FieldMasker> fieldMaskers;

    private final Collection<ValueMasker> valueMaskers;

    /**
     * Number of open containers within a masked value.
     */
    private int maskDepth = 0;

    /**
     * The replacement for the next field value has already been written.
     */
    private boolean maskNextValue;

    /**
     * @param delegate the generator to which to write potentially masked JSON
     * @param fieldMaskers {@link FieldMasker}s to mask fields
     * @param valueMaskers {@link ValueMasker}s to mask values
     */
    public MaskingJsonGenerator(
            JsonGenerator delegate,
            Collection<FieldMasker> fieldMaskers,
            Collection<ValueMasker> valueMaskers) {
        super(delegate, false);
        this.fieldMaskers = fieldMaskers == null ? Collections.emptyList() : fieldMaskers;
        this.valueMaskers = valueMaskers == null ? Collections.emptyList() : valueMaskers;
    }

    @Override
    public JsonGenerator writeArray(int[] array, int offset, int length) {
        /*
         * Delegate to writeArrayStart, writeNumber, and writeArrayEnd
         * so that masking can be performed.
         */

        if (array == null) {
            throw new IllegalArgumentException("null array");
        }
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset + length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
        return this;
    }

    @Override
    public JsonGenerator writeArray(long[] array, int offset, int length) {
        /*
         * Delegate to writeArrayStart, writeNumber, and writeArrayEnd
         * so that masking can be performed.
         */

        if (array == null) {
            throw new IllegalArgumentException("null array");
        }
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset + length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
        return this;
    }

    @Override
    public JsonGenerator writeArray(double[] array, int offset, int length) {
        /*
         * Delegate to writeArrayStart, writeNumber, and writeArrayEnd
         * so that masking can be performed.
         */

        if (array == null) {
            throw new IllegalArgumentException("null array");
        }
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset + length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
        return this;
    }

    @Override
    public JsonGenerator writeArray(String[] array, int offset, int length) {
        if (array == null) {
            throw new IllegalArgumentException("null array");
        }
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset + length; i < end; ++i) {
            writeString(array[i]);
        }
        writeEndArray();
        return this;
    }

    @Override
    public JsonGenerator writeName(SerializableString name) {
        return writeName(() -> super.writeName(name));
    }

    @Override
    public JsonGenerator writeName(String name) {
        return writeName(() -> super.writeName(name));
    }

    @Override
    public JsonGenerator writePropertyId(long id) {
        return writeName(() -> super.writePropertyId(id));
    }

    private JsonGenerator writeName(Runnable doWriteFieldName) {
        if (!maskingInProgress()) {
            doWriteFieldName.run();
            Object maskedValue = getMaskedValueForCurrentPath(streamWriteContext());
            if (maskedValue != null) {
                delegate.writePOJO(maskedValue);
                maskNextValue = true;
            }
        }
        return this;
    }

    @Override
    public JsonGenerator writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) {
        if (shouldWriteValue()) {
            super.writeBinary(b64variant, data, offset, len);
        }
        return this;
    }

    @Override
    public JsonGenerator writeBinary(byte[] data) {
        if (shouldWriteValue()) {
            super.writeBinary(data);
        }
        return this;
    }

    @Override
    public JsonGenerator writeBinary(byte[] data, int offset, int len) {
        if (shouldWriteValue()) {
            super.writeBinary(data, offset, len);
        }
        return this;
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) {
        if (shouldWriteValue()) {
            return super.writeBinary(b64variant, data, dataLength);
        } else {
            return readAndDiscard(data);
        }
    }

    @Override
    public int writeBinary(InputStream data, int dataLength) {
        if (shouldWriteValue()) {
            return super.writeBinary(data, dataLength);
        } else {
            return readAndDiscard(data);
        }
    }

    private int readAndDiscard(InputStream data) {
        try {
            int bytesRead = 0;
            while (data.read() != -1) {
                bytesRead++;
            }
            return bytesRead;
        } catch (IOException e) {
            throw JacksonIOException.construct(e, this);
        }
    }

    @Override
    public JsonGenerator writeBoolean(boolean state) {
        if (shouldWriteValue()) {
            super.writeBoolean(state);
        }
        return this;
    }

    @Override
    public JsonGenerator writeEmbeddedObject(Object object) {
        if (shouldWriteValue()) {
            super.writeEmbeddedObject(object);
        }
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        if (shouldWriteValue()) {
            super.writeNull();
        }
        return this;
    }

    @Override
    public JsonGenerator writeNumber(BigDecimal v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(BigInteger v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(double v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(float v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(int v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(short v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(long v) {
        return writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public JsonGenerator writeNumber(String encodedValue) {
        return writePotentiallyMaskedValue(encodedValue, () -> super.writeNumber(encodedValue));
    }

    @Override
    public JsonGenerator writeNumber(char[] encodedValueBuffer, int offset, int length) {
        return writePotentiallyMaskedValue(new String(encodedValueBuffer, offset, length),
                () -> super.writeNumber(encodedValueBuffer, offset, length));
    }

    @Override
    public JsonGenerator writeObjectId(Object id) {
        if (!maskingInProgress()) {
            super.writeObjectId(id);
        }
        return this;
    }

    @Override
    public JsonGenerator writeObjectRef(Object id) {
        if (!maskingInProgress()) {
            super.writeObjectRef(id);
        }
        return this;
    }

    @Override
    public JsonGenerator writeOmittedProperty(String fieldName) {
        if (!maskingInProgress()) {
            super.writeOmittedProperty(fieldName);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRaw(char c) {
        if (!maskingInProgress()) {
            super.writeRaw(c);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRaw(char[] text, int offset, int len) {
        if (!maskingInProgress()) {
            super.writeRaw(text, offset, len);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRaw(String text) {
        if (!maskingInProgress()) {
            super.writeRaw(text);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRaw(String text, int offset, int len) {
        if (!maskingInProgress()) {
            super.writeRaw(text, offset, len);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRaw(SerializableString raw) {
        if (!maskingInProgress()) {
            super.writeRaw(raw);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(String text) {
        if (shouldWriteValue(false)) {
            super.writeRawValue(text);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(String text, int offset, int len) {
        if (shouldWriteValue(false)) {
            super.writeRawValue(text, offset, len);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(char[] text, int offset, int len) {
        if (shouldWriteValue(false)) {
            super.writeRawValue(text, offset, len);
        }
        return this;
    }

    @Override
    public JsonGenerator writeRawUTF8String(byte[] text, int offset, int length) {
        if (shouldWriteValue(false)) {
            super.writeRawUTF8String(text, offset, length);
        }
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        if (shouldWriteValue()) {
            super.writeStartArray();
        } else {
            incrementMaskDepth();
        }
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue) {
        if (shouldWriteValue()) {
            super.writeStartArray(forValue);
        } else {
            incrementMaskDepth();
        }
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue, int size) {
        if (shouldWriteValue()) {
            super.writeStartArray(forValue, size);
        } else {
            incrementMaskDepth();
        }
        return this;
    }

    @Override
    public JsonGenerator writeStartObject() {
        if (shouldWriteValue()) {
            super.writeStartObject();
        } else {
            incrementMaskDepth();
        }
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue) {
        if (shouldWriteValue()) {
            super.writeStartObject(forValue);
        } else {
            incrementMaskDepth();
        }
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue, int size) {
        if (shouldWriteValue()) {
            super.writeStartObject(forValue, size);
        } else {
            incrementMaskDepth();
        }
        return this;
    }

    @Override
    public JsonGenerator writeString(char[] text, int offset, int len) {
        return writePotentiallyMaskedValue(new String(text, offset, len), () -> super.writeString(text, offset, len));
    }

    @Override
    public JsonGenerator writeString(String text) {
        return writePotentiallyMaskedValue(text, () -> super.writeString(text));
    }

    @Override
    public JsonGenerator writeString(SerializableString text) {
        return writePotentiallyMaskedValue(text.getValue(), () -> super.writeString(text));
    }

    @Override
    public JsonGenerator writeString(Reader reader, int len) {
        if (shouldWriteValue()) {
            super.writeString(reader, len);
        }
        return this;
    }

    @Override
    public JsonGenerator writeUTF8String(byte[] text, int offset, int length) {
        return writePotentiallyMaskedValue(new String(text, offset, length, StandardCharsets.UTF_8), () -> super.writeUTF8String(text, offset, length));
    }

    @Override
    public JsonGenerator writeTypeId(Object id) {
        if (!maskingInProgress()) {
            super.writeTypeId(id);
        }
        return this;
    }

    @Override
    public JsonGenerator writeEndArray() {
        if (maskDepth != 0) {
            decrementMaskDepth();
        } else {
            super.writeEndArray();
        }
        return this;
    }

    @Override
    public JsonGenerator writeEndObject() {
        if (maskDepth != 0) {
            decrementMaskDepth();
        } else {
            super.writeEndObject();
        }
        return this;
    }

    /**
     * @return the masked value for the current path if the current path should be masked.
     *         otherwise returns null.
     */
    private Object getMaskedValueForCurrentPath(TokenStreamContext context) {
        for (FieldMasker fieldMasker : fieldMaskers) {
            Object maskedValue = fieldMasker.mask(context);
            if (maskedValue != null) {
                return maskedValue;
            }
        }
        return null;
    }
    /**
     * @param originalValue the value to potentially mask
     * @return the masked value for the current path and value if the value should be masked.
     *         otherwise returns null.
     */
    private Object getMaskedValueForCurrentPathAndValue(Object originalValue) {
        TokenStreamContext context = streamWriteContext();
        Object localValue = originalValue;
        for (ValueMasker valueMasker : valueMaskers) {
            Object maskedValue = valueMasker.mask(context, localValue);
            if (maskedValue != null) {
                localValue = maskedValue;
            }
        }
        if (localValue != originalValue) {
            return localValue;
        }
        return null;
    }

    private JsonGenerator writePotentiallyMaskedValue(Object value, Runnable doWriteUnmaskedValue) {
        if (shouldWriteValue()) {
            Object maskedValue = getMaskedValueForCurrentPathAndValue(value);
            if (maskedValue != null) {
                delegate.writePOJO(maskedValue);
            } else {
                doWriteUnmaskedValue.run();
            }
        }
        return this;
    }

    private boolean shouldWriteValue() {
        return shouldWriteValue(true);
    }

    private boolean shouldWriteValue(boolean maskArrayElement) {
        if (maskDepth != 0) {
            return false;
        }
        if (maskNextValue) {
            maskNextValue = false;
            return false;
        }
        TokenStreamContext context = streamWriteContext();
        if (maskArrayElement && !fieldMaskers.isEmpty() && context.inArray()) {
            Object maskedValue = getMaskedValueForCurrentPath(new ArrayElementContext(context));
            if (maskedValue != null) {
                delegate.writePOJO(maskedValue);
                return false;
            }
        }
        return true;
    }

    /**
     * The delegate advances its array index only when it writes a value.
     * Present the upcoming index to maskers without changing the delegate's state.
     */
    private static class ArrayElementContext extends TokenStreamContext {

        private final TokenStreamContext context;

        ArrayElementContext(TokenStreamContext context) {
            super(TYPE_ARRAY, context.getEntryCount());
            this.context = context;
            _nestingDepth = context.getNestingDepth();
        }

        @Override
        public TokenStreamContext getParent() {
            return context.getParent();
        }

        @Override
        public String currentName() {
            return null;
        }

        @Override
        public Object currentValue() {
            return context.currentValue();
        }

        @Override
        public void assignCurrentValue(Object value) {
            context.assignCurrentValue(value);
        }
    }

    private void incrementMaskDepth() {
        maskDepth++;
    }

    private void decrementMaskDepth() {
        maskDepth--;
    }

    /**
     * Returns true if the next value or the current container is masked.
     */
    boolean maskingInProgress() {
        return maskNextValue || maskDepth != 0;
    }

}
