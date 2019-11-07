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
package net.logstash.logback.decorate;

import java.io.*;
import java.lang.Object;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

public class MaskingJsonGenerator extends JsonGeneratorDelegate {

  public static List<String> SENSITIVE_FIELDS = Arrays.asList(
      "apikey", "api-key", "api_key",
      "cookie",
      "ip",
      "username", "user-name", "user_name",
      "password",
      "name",
      "company",
      "address",
      "email", "e-mail",
      "phone",
      "fullname", "full_name",
      "firstname", "first-name", "first_name",
      "lastname", "last-name", "last_name"
  );

  public static List<String> SENSITIVE_PATTERNS = Arrays.asList(
      "(\\d+\\.\\d+\\.\\d+\\.\\d+)",  // IP pattern
      "(\\w+@\\w+\\.\\w+)", // email pattern
      "Cookie:\\s*(.*?)\\s" // cookie pattern
  );

  public static String SENSITIVE_MASK = "******";

  private String lastFieldWritten;

  public MaskingJsonGenerator(JsonGenerator d) {
    super(d);
  }

  public MaskingJsonGenerator(JsonGenerator d, boolean delegateCopyMethods) {
    super(d, delegateCopyMethods);
  }

  public static boolean isSensitiveField(Object name) {
    for (int i = 0; i < SENSITIVE_FIELDS.size(); ++ i) {
      if (name.toString().toLowerCase().equals(SENSITIVE_FIELDS.get(i).toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  public static String sanitize(String message) {
    String fullPattern = "";
    for (int i = 0, len = SENSITIVE_PATTERNS.size(); i < len; ++i) {
      String pattern = SENSITIVE_PATTERNS.get(i);
      if (fullPattern == "") {
        fullPattern = pattern;
      } else {
        fullPattern = fullPattern + "|" + pattern;
      }
    }
    for (int i = 0, len = SENSITIVE_FIELDS.size(); i < len; ++i) {
      String field = SENSITIVE_FIELDS.get(i);
      String[] patterns = {
          "\"" + field + "\"\\s*:\\s*\"(.*?)\"",
          field + "\\s*=\\s*(.*?)(?:$|\\s+)",
          field + "\\s*:\\s*(.*?)(?:$|\\s+)"
      };
      for (int j = 0; j < patterns.length; ++ j) {
        String pattern = patterns[j];
        if (fullPattern == "") {
          fullPattern = pattern;
        } else {
          fullPattern = fullPattern + "|" + pattern;
        }
      }
    }

    Pattern multilinePattern = Pattern.compile(
        fullPattern,
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    if (multilinePattern == null) {
      return message;
    }

    StringBuilder sb = new StringBuilder(message);
    Matcher matcher = multilinePattern.matcher(sb);
    while (matcher.find()) {
      for (int group = 1; group <= matcher.groupCount(); ++ group) {
        if (matcher.group(group) != null) {
          for (int i = matcher.start(group); i < matcher.end(group); ++ i) {
            sb.setCharAt(i, '*'); // TODO(cosmin): this still leaks the length of the sensitive data
          }
        }
      }
    }
    return sb.toString();
  }

  public static Object sanitize(Object ob) {
    Class<?> cls = ob.getClass();
    if (cls == String.class) {
      return sanitize((String) ob);
    } else if (cls == Integer.class) {
      return ob;
    } else  if (cls == Long.class) {
      return ob;
    } else  if (cls == Boolean.class) {
      return ob;
    } else  if (cls == Double.class) {
      return ob;
    } else if (Map.class.isAssignableFrom(cls)) {
      return sanitize((Map) ob);
    } else if (List.class.isAssignableFrom(cls)) {
      return sanitize((List) ob);
    } else {
      return ob;
    }
  }

  public static Map<Object, Object> sanitize(Map<Object, Object> value) {
    Map sanitizedMap = new HashMap<Object, Object>();
    for (Map.Entry<Object, Object> entry : value.entrySet()) {
      if (isSensitiveField(entry.getKey())) {
        sanitizedMap.put(entry.getKey(), SENSITIVE_MASK);
      } else {
        sanitizedMap.put(entry.getKey(), sanitize(entry.getValue()));
      }
    }
    return sanitizedMap;
  }

  public static List<Object> sanitize(List<Object> value) {
    List<Object> sanitizedList = new ArrayList<Object>();
    for (int i = 0, len = value.size(); i < len; ++i) {
      sanitizedList.add(sanitize(value.get(i)));
    }
    return sanitizedList;
  }

  public static List<String> sanitizeStringList(List<String> value) {
    List<String> sanitizedList = new ArrayList<String>();
    for (int i = 0, len = value.size(); i < len; ++i) {
      sanitizedList.add(sanitize(value.get(i)));
    }
    return sanitizedList;
  }

  public static Map<String, String> sanitizeStringMap(Map<String, String> value) {
    Map sanitizedMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : value.entrySet()) {
      if (isSensitiveField(entry.getKey())) {
        sanitizedMap.put(entry.getKey(), SENSITIVE_MASK);
      } else {
        sanitizedMap.put(entry.getKey(), sanitize(entry.getValue()));
      }
    }
    return sanitizedMap;
  }

  public static Map<String, List<String>> sanitizeStringsMap(Map<String, List<String>> value) {
    Map<String, List<String>> sanitizedMap = new HashMap<String, List<String>>();
    for (Map.Entry<String, List<String>> entry : value.entrySet()) {
      if (isSensitiveField(entry.getKey())) {
        sanitizedMap.put(entry.getKey(), Arrays.asList(SENSITIVE_MASK));
      } else {
        sanitizedMap.put(entry.getKey(), sanitizeStringList(entry.getValue()));
      }
    }
    return sanitizedMap;
  }

  @Override
  public void writeFieldName(String name) throws IOException {
    lastFieldWritten = name;
    super.writeFieldName(name);
  }

  @Override
  public void writeFieldName(SerializableString name) throws IOException {
    lastFieldWritten = name.toString();
    super.writeFieldName(name);
  }

  @Override
  public void writeFieldId(long id) throws IOException {
    super.writeFieldId(id);
  }

  @Override
  public void writeArray(int[] array, int offset, int length) throws IOException {
    super.writeArray(array, offset, length);
  }

  @Override
  public void writeArray(long[] array, int offset, int length) throws IOException {
    super.writeArray(array, offset, length);
  }

  @Override
  public void writeArray(double[] array, int offset, int length) throws IOException {
    super.writeArray(array, offset, length);
  }

  @Override
  public void writeString(String text) throws IOException {
    if (isSensitiveField(lastFieldWritten)) {
      super.writeString(SENSITIVE_MASK);
    } else {
      super.writeString(sanitize(text));
    }
  }

  @Override
  public void writeString(Reader reader, int len) throws IOException {
    super.writeString(reader, len);
  }

  @Override
  public void writeString(char[] text, int offset, int len) throws IOException {
    super.writeString(text, offset, len);
  }

  @Override
  public void writeString(SerializableString text) throws IOException {
    if (isSensitiveField(lastFieldWritten)) {
      super.writeString(SENSITIVE_MASK);
    } else {
      super.writeString(sanitize(text.toString()));
    }
  }

  @Override
  public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
    super.writeRawUTF8String(text, offset, length);
  }

  @Override
  public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
    super.writeUTF8String(text, offset, length);
  }

  @Override
  public void writeRaw(String text) throws IOException {
    super.writeRaw(text);
  }

  @Override
  public void writeRaw(String text, int offset, int len) throws IOException {
    super.writeRaw(text, offset, len);
  }

  @Override
  public void writeRaw(SerializableString raw) throws IOException {
    super.writeRaw(raw);
  }

  @Override
  public void writeRaw(char[] text, int offset, int len) throws IOException {
    super.writeRaw(text, offset, len);
  }

  @Override
  public void writeRaw(char c) throws IOException {
    super.writeRaw(c);
  }

  @Override
  public void writeRawValue(String text) throws IOException {
    super.writeRawValue(sanitize(text));
  }

  @Override
  public void writeRawValue(String text, int offset, int len) throws IOException {
    super.writeRawValue(text, offset, len);
  }

  @Override
  public void writeRawValue(char[] text, int offset, int len) throws IOException {
    super.writeRawValue(text, offset, len);
  }

  @Override
  public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException {
    super.writeBinary(b64variant, data, offset, len);
  }

  @Override
  public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException {
    return super.writeBinary(b64variant, data, dataLength);
  }

  @Override
  public void writeNumber(short v) throws IOException {
    super.writeNumber(v);
  }

  @Override
  public void writeNumber(int v) throws IOException {
    super.writeNumber(v);
  }

  @Override
  public void writeNumber(long v) throws IOException {
    super.writeNumber(v);
  }

  @Override
  public void writeNumber(BigInteger v) throws IOException {
    super.writeNumber(v);
  }

  @Override
  public void writeNumber(float v) throws IOException {
    super.writeNumber(v);
  }

  @Override
  public void writeNumber(BigDecimal v) throws IOException {
    super.writeNumber(v);
  }

  @Override
  public void writeNumber(String encodedValue) throws IOException, UnsupportedOperationException {
    super.writeNumber(encodedValue);
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {
    super.writeBoolean(state);
  }

  @Override
  public void writeStringField(String fieldName, String value) throws IOException {
    if (isSensitiveField(fieldName)) {
      super.writeStringField(fieldName, SENSITIVE_MASK);
    } else {
      super.writeStringField(fieldName, value);
    }
  }

  @Override
  public void writeOmittedField(String fieldName) throws IOException {
    super.writeOmittedField(fieldName);
  }

  @Override
  public void writeObjectId(Object id) throws IOException {
    super.writeObjectId(id);
  }

  @Override
  public void writeObjectRef(Object id) throws IOException {
    super.writeObjectRef(id);
  }

  @Override
  public void writeTypeId(Object id) throws IOException {
    super.writeObjectRef(id);
    super.writeTypeId(id);
  }

  @Override
  public void writeEmbeddedObject(Object object) throws IOException {
    super.writeEmbeddedObject(object);
  }

  @Override
  public void writeObject(Object pojo) throws IOException {
    if (isSensitiveField(lastFieldWritten)) {
      this.writeString(SENSITIVE_MASK);
    } else {
      if (pojo instanceof String) {
        this.writeString((String) pojo);
      } else {
        super.writeObject(sanitize(pojo));
      }
    }
  }

  @Override
  public void writeTree(TreeNode tree) throws IOException {
    super.writeTree(tree);
  }
}
