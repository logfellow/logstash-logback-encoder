/*
 * Copyright 2013-2023 the original author or authors.
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

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;


/**
 * A {@link JsonGenerator} that can mask sensitive field values before delegating to a delegate {@link JsonGenerator}.
 *
 * <h2>Identifying field values to mask by <em>path</em></h2>
 *
 * The {@code MaskingJsonGenerator} uses a collection of {@link FieldMasker}s
 * to identify fields to mask by <em>JSON path</em>.
 *
 * <p>These field maskers are invoked after a new field name is written
 * to determine if the field's value should be masked.
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
     * Keeps track of the current masking state.
     * A positive value indicates the generator is within a masked field.
     */
    private int maskDepth = 0;

    @FunctionalInterface
    private interface ThrowingRunnable<E extends Exception> {
        void run() throws E;
    }

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
    public void writeArray(int[] array, int offset, int length) throws IOException {
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
    }

    @Override
    public void writeArray(long[] array, int offset, int length) throws IOException {
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
    }

    @Override
    public void writeArray(double[] array, int offset, int length) throws IOException {
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
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException {
        writeFieldName(() -> super.writeFieldName(name));
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        writeFieldName(() -> super.writeFieldName(name));
    }

    @Override
    public void writeFieldId(long id) throws IOException {
        writeFieldName(() -> super.writeFieldId(id));
    }

    private void writeFieldName(ThrowingRunnable<IOException> doWriteFieldName) throws IOException {

        if (maskingInProgress()) {
            /*
             * This allows writing unmasked fields after masked fields in the same object.
             */
            decrementMaskDepth();
        }
        if (maskingInProgress()) {
            /*
             * If we're in a subobject of a masked field, then increment the mask stack.
             * This ensures that no fields are written in the subobject.
             */
            incrementMaskDepth();
        } else {
            /*
             * We're not currently masking, so go ahead and write the field name.
             *
             * This must be called before shouldMaskCurrentPath(),
             * so that the current path is updated.
             */
            doWriteFieldName.run();

            Object maskedValue = getMaskedValueForCurrentPath();
            if (maskedValue != null) {
                /*
                 * Write the masked value.
                 * No other values will be written when masking is in progress.
                 */
                delegate.writeObject(maskedValue);

                incrementMaskDepth();

            }
        }
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeBinary(b64variant, data, offset, len);
        }
    }

    @Override
    public void writeBinary(byte[] data) throws IOException {
        if (!maskingInProgress()) {
            super.writeBinary(data);
        }
    }

    @Override
    public void writeBinary(byte[] data, int offset, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeBinary(data, offset, len);
        }
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException {
        if (!maskingInProgress()) {
            return super.writeBinary(b64variant, data, dataLength);
        } else {
            return readAndDiscard(data);
        }
    }

    @Override
    public int writeBinary(InputStream data, int dataLength) throws IOException {
        if (!maskingInProgress()) {
            return super.writeBinary(data, dataLength);
        } else {
            return readAndDiscard(data);
        }
    }

    private int readAndDiscard(InputStream data) throws IOException {
        int bytesRead = 0;
        while (data.read() != -1) {
            bytesRead++;
        }
        return bytesRead;
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        if (!maskingInProgress()) {
            super.writeBoolean(state);
        }
    }

    @Override
    public void writeEmbeddedObject(Object object) throws IOException {
        if (!maskingInProgress()) {
            super.writeEmbeddedObject(object);
        }
    }

    @Override
    public void writeNull() throws IOException {
        if (!maskingInProgress()) {
            super.writeNull();
        }
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(double v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(float v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(int v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(short v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(long v) throws IOException {
        writePotentiallyMaskedValue(v, () -> super.writeNumber(v));
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        writePotentiallyMaskedValue(encodedValue, () -> super.writeNumber(encodedValue));
    }

    @Override
    public void writeObjectId(Object id) throws IOException {
        if (!maskingInProgress()) {
            super.writeObjectId(id);
        }
    }

    @Override
    public void writeObjectRef(Object id) throws IOException {
        if (!maskingInProgress()) {
            super.writeObjectRef(id);
        }
    }

    @Override
    public void writeOmittedField(String fieldName) throws IOException {
        if (!maskingInProgress()) {
            super.writeOmittedField(fieldName);
        }
    }

    @Override
    public void writeRaw(char c) throws IOException {
        if (!maskingInProgress()) {
            super.writeRaw(c);
        }
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeRaw(String text) throws IOException {
        if (!maskingInProgress()) {
            super.writeRaw(text);
        }
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeRaw(SerializableString raw) throws IOException {
        if (!maskingInProgress()) {
            super.writeRaw(raw);
        }
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        if (!maskingInProgress()) {
            super.writeRawValue(text);
        }
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeRawValue(text, offset, len);
        }
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeRawValue(text, offset, len);
        }
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        if (!maskingInProgress()) {
            super.writeRawUTF8String(text, offset, length);
        }
    }

    @Override
    public void writeStartArray(int size) throws IOException {
        if (!maskingInProgress()) {
            super.writeStartArray(size);
        }
    }

    @Override
    public void writeStartArray() throws IOException {
        if (!maskingInProgress()) {
            super.writeStartArray();
        }
    }
    @Override
    public void writeStartArray(Object forValue) throws IOException {
        if (!maskingInProgress()) {
            super.writeStartArray(forValue);
        }
    }

    @Override
    public void writeStartArray(Object forValue, int size) throws IOException {
        if (!maskingInProgress()) {
            super.writeStartArray(forValue, size);
        }
    }

    @Override
    public void writeStartObject() throws IOException {
        if (!maskingInProgress()) {
            super.writeStartObject();
        } else {
            incrementMaskDepth();
        }
    }

    @Override
    public void writeStartObject(Object forValue) throws IOException {
        if (!maskingInProgress()) {
            super.writeStartObject(forValue);
        } else {
            incrementMaskDepth();
        }
    }

    @Override
    public void writeStartObject(Object forValue, int size) throws IOException {
        if (!maskingInProgress()) {
            super.writeStartObject(forValue, size);
        } else {
            incrementMaskDepth();
        }
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        writePotentiallyMaskedValue(new String(text, offset, len), () -> super.writeString(text, offset, len));
    }

    @Override
    public void writeString(String text) throws IOException {
        writePotentiallyMaskedValue(text, () -> super.writeString(text));
    }

    @Override
    public void writeString(SerializableString text) throws IOException {
        writePotentiallyMaskedValue(text.getValue(), () -> super.writeString(text));
    }

    @Override
    public void writeString(Reader reader, int len) throws IOException {
        if (!maskingInProgress()) {
            super.writeString(reader, len);
        }
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        writePotentiallyMaskedValue(new String(text, offset, length, StandardCharsets.UTF_8), () -> super.writeUTF8String(text, offset, length));
    }

    @Override
    public void writeTypeId(Object id) throws IOException {
        if (!maskingInProgress()) {
            super.writeTypeId(id);
        }
    }

    @Override
    public void writeEndArray() throws IOException {
        if (!maskingInProgress()) {
            super.writeEndArray();
        }
    }

    @Override
    public void writeEndObject() throws IOException {
        if (maskingInProgress()) {
            decrementMaskDepth();
        }
        if (!maskingInProgress()) {
            super.writeEndObject();
        }
    }

    /**
     * @return the masked value for the current path if the current path should be masked.
     *         otherwise returns null.
     */
    private Object getMaskedValueForCurrentPath() {
        JsonStreamContext context = getOutputContext();
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
        JsonStreamContext context = getOutputContext();
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

    private void writePotentiallyMaskedValue(Object value, ThrowingRunnable<IOException> doWriteUnmaskedValue) throws IOException {
        if (!maskingInProgress()) {
            Object maskedValue = getMaskedValueForCurrentPathAndValue(value);
            if (maskedValue != null) {
                delegate.writeObject(maskedValue);
            } else {
                doWriteUnmaskedValue.run();
            }
        }
    }

    private void incrementMaskDepth() {
        maskDepth++;
    }

    private void decrementMaskDepth() {
        maskDepth--;
    }

    /**
     * Returns true if the event iterator is within an field that should be masked.
     */
    boolean maskingInProgress() {
        return maskDepth != 0;
    }

}
