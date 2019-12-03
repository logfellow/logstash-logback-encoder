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
package net.logstash.logback.mask;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonStreamContext;

/**
 * Masks values of an absolute or partial path within a JSON stream.
 *
 * <p>Values for paths that match a given path string will be replaced with a given mask string.</p>
 *
 * <h1>Path String Format</h1>
 *
 * <p>The path string to match follows a format similar to (but not exactly the same as) a
 * <a href="http://tools.ietf.org/html/draft-ietf-appsawg-json-pointer-03">JSON Pointer</a> string,
 * with the differences being:</p>
 * <ul>
 *     <li>At least one reference token is required  (e.g. "" and "/" are not allowed)</li>
 *     <li>The path string does not need to start with {@value #TOKEN_SEPARATOR}.
 *         If a path string starts with {@value #TOKEN_SEPARATOR} it is interpreted as an absolute path.
 *         Otherwise, it is a partial path.</li>
 *     <li>A wildcard token ({@value WILDCARD_TOKEN}) is supported.</li>
 *     <li>The path string must end with a field name (not an array index)</li>
 * </ul>
 *
 * <h1>Absolute Paths</h1>
 *
 * <p>Absolute paths start with {@value #TOKEN_SEPARATOR}, followed by one or more
 * reference tokens separated by {@value #TOKEN_SEPARATOR}.
 * Absolute paths must match the full path from the root of the streaming context.</p>
 *
 * <p>For example, given the following JSON:</p>
 *
 * <pre>
 * {
 *     "aaa": {
 *         "bbb": [
 *             {
 *                 "ccc": "ddd"
 *             }
 *         ]
 *     },
 *     "bbb": [
 *         {
 *             "eee": "fff"
 *         }
 *     ]
 * }
 * </pre>
 *
 * <p>Then the following matches occur:</p>
 *
 * <ul>
 *     <li><code>/aaa</code> matches <code>{ "bbb" : [ { "ccc" : "ddd" } ] }</code></li>
 *     <li><code>/aaa/bbb</code> matches <code>[ { "ccc" : "ddd" } ]</code></li>
 *     <li><code>/aaa/bbb/0/ccc</code> matches <code>"ddd"</code></li>
 * </ul>
 *
 * <h1>Partial Paths</h1>
 *
 * <p>Partial paths do NOT start with {@value #TOKEN_SEPARATOR}, and contain
 * one or more reference tokens separated by {@value #TOKEN_SEPARATOR}.
 * Partial paths mask a partial path anywhere in the stream.</p>
 *
 * <p>For example, given the following JSON:</p>
 *
 * <pre>
 * {
 *     "aaa": {
 *         "bbb": [
 *             {
 *                 "ccc": "ddd"
 *             }
 *         ]
 *     },
 *     "bbb": [
 *         {
 *             "eee": "fff"
 *         }
 *     ]
 * }
 * </pre>
 *
 * <p>Then the following matches occur:</p>
 *
 * <ul>
 *     <li><code>aaa</code> matches <code>{ "bbb" : [ { "ccc" : "ddd" } ] }</code></li>
 *     <li><code>aaa/bbb</code> matches <code>[ { "ccc" : "ddd" } ]</code></li>
 *     <li><code>aaa/bbb/0/ccc</code> matches <code>"ddd"</code></li>
 *     <li><code>bbb</code> matches <code>[ { "ccc" : "ddd" } ]</code> and <code>[ { "eee" : "fff" } ]</code></li>
 *     <li><code>bbb/0/ccc</code> matches <code>"ddd"</code></li>
 *     <li><code>0/ccc</code> matches <code>"ddd"</code></li>
 *     <li><code>ccc</code> matches <code>"ddd"</code></li>
 * </ul>
 *
 * <p>For single field values (e.g. partial paths with only one token), consider
 * using a {@link FieldNameBasedFieldMasker} instead.
 * A single {@link FieldNameBasedFieldMasker} configured with many field names,
 * is much more efficient than having a {@link PathBasedFieldMasker} per field name.</p>
 *
 * <h1>Wildcard Tokens</h1>
 *
 * <p>The wildcard value ({@value WILDCARD_TOKEN}) can be used as a token in the path string.
 * The wildcard token will match any token.</p>
 *
 * <p>For example, given the following JSON:</p>
 *
 * <pre>
 * {
 *     "aaa": {
 *         "bbb": {
 *             "ccc": "ddd"
 *         },
 *         "eee": {
 *             "ccc": "hhh",
 *         },
 *     },
 *     "iii": {
 *         "jjj": {
 *             "ccc": "lll"
 *         },
 *     },
 *     "ccc": "mmm"
 * }
 * </pre>
 *
 * <p>Then the following matches occur:</p>
 *
 * <ul>
 *     <li><code>aaa/*&#47;ccc</code> matches <code>"ddd"</code> and <code>"hhh"</code></li>
 *     <li><code>*&#47;ccc</code> matches <code>"ddd"</code> and <code>"hhh"</code> and <code>"lll"</code></li>
 * </ul>
 *
 * <h1>Escaping</h1>
 *
 * <p>JSON Pointer escaping can be used to escape '/' and '~' within tokens.  Specifically, use:</p>
 * <ul>
 *     <li>'~1' to represent '/' within a token</li>
 *     <li>'~0' to represent '~' within a token</li>
 * </ul>
 *
 */
public class PathBasedFieldMasker implements FieldMasker {

    public static final String TOKEN_SEPARATOR = "/";
    public static final String WILDCARD_TOKEN = "*";

    private final boolean isAbsolutePath;
    private final String[] tokens;
    private final Object mask;

    /**
     * @param pathToMask the absolute or partial path to mask (see class javadoc)
     * @param mask the value to write for any paths that match the pathToMask
     */
    public PathBasedFieldMasker(String pathToMask, Object mask) {
        validatePathToMask(pathToMask);

        isAbsolutePath = pathToMask.startsWith(TOKEN_SEPARATOR);

        if (isAbsolutePath) {
            pathToMask = pathToMask.substring(TOKEN_SEPARATOR.length());
        }

        tokens = pathToMask.split(TOKEN_SEPARATOR);

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = unescapeJsonPointerToken(tokens[i]);
        }
        this.mask = mask;
    }

    static void validatePathToMask(String pathToMask) {
        Objects.requireNonNull(pathToMask, "pathToMask must not be null");
        if (pathToMask.isEmpty()) {
            throw new IllegalArgumentException("pathToMask must not be empty");
        }
        if (pathToMask.equals(TOKEN_SEPARATOR)) {
            throw new IllegalArgumentException("pathToMask must contain at least one token");
        }
    }

    @Override
    public Object mask(JsonStreamContext context) {
        JsonStreamContext currentContext = context;
        for (int i = tokens.length; --i >= 0; currentContext = currentContext.getParent()) {
            if (!currentLeafMatches(currentContext, tokens[i])) {
                return null;
            }
        }

        return (currentContext != null && (!isAbsolutePath || currentContext.getParent() == null || currentContext.getParent().inRoot()))
                ? mask
                : null;
    }

    private boolean currentLeafMatches(JsonStreamContext context, String leafName) {
        if (context != null) {
            if (WILDCARD_TOKEN.equals(leafName)) {
                return true;
            }
            if (context.hasCurrentName()) {
                return context.getCurrentName().equals(leafName);
            }
            if (context.hasCurrentIndex()) {
                return Integer.toString(context.getCurrentIndex()).equals(leafName);
            }
        }
        return false;
    }

    /**
     * Returns true if the given path represents a single field name (e.g. not a multi token or wildcard path).
     *
     * @param path the path to check
     * @return true if the given path represents a single field name (e.g. not a multi token or wildcard path).
     */
    static boolean isSingleFieldName(String path) {
        return !path.contains(PathBasedFieldMasker.TOKEN_SEPARATOR) && !path.contains(PathBasedFieldMasker.WILDCARD_TOKEN);
    }

    /**
     * Unescapes "~1" as "/", and "~0" as "~" from a JSON pointer token.
     *
     * @param token the JSON pointer token to unescape
     * @return the unescaped token value.
     */
    static String unescapeJsonPointerToken(String token) {
        return token
                // As per JSON Pointer string spec, ~1 is used to escape "/"
                .replaceAll("~1", "/")
                // As per JSON Pointer string spec, ~0 is used to escape "~"
                .replaceAll("~0", "~");
    }

}
