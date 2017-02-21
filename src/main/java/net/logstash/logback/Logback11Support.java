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
package net.logstash.logback;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ch.qos.logback.core.encoder.Encoder;

/**
 * Provides backwards compatibility at runtime with logback 1.1.x.
 * 
 * In logback version 1.2.0, the {@link Encoder} interface was changed from
 * writing to an {@link OutputStream} to returning byte arrays.
 * This was a backwards incompatible change, therefore some fancy runtime reflection
 * is required to make logstash-logback-encoder work with both pre- and post-1.2 logback versions.
 * 
 * This class is used to determine if logback 1.1 is on the runtime classpath ({@link #isLogback11OrBefore()}),
 * and invoke the old methods on the {@link Encoder} interface.
 */
public class Logback11Support {
    
    private static final Method ENCODER_INIT_METHOD = getMethod(Encoder.class, "init", OutputStream.class);
    private static final Method ENCODER_DO_ENCODE_METHOD = getMethod(Encoder.class, "doEncode", Object.class);
    private static final Method ENCODER_CLOSE_METHOD = getMethod(Encoder.class, "close");
    private static final boolean IS_LOGBACK_1_1 = ENCODER_INIT_METHOD != null;

    /**
     * Returns true if logback 1.1.x or earlier is on the runtime classpath.
     * Returns false if logback 1.2.x or later is on the runtime classpath
     */
    public static boolean isLogback11OrBefore() {
        return IS_LOGBACK_1_1;
    }

    /**
     * Called by logic that should only execute if logback 1.1.x or earlier is on the runtime classpath.
     * 
     * @throws IllegalStateException if the logback version is >= 1.2 
     */
    public static void verifyLogback11OrBefore() {
        if (!isLogback11OrBefore()) {
            throw new IllegalStateException("Logback 1.1 only method called, but Logback version is >= 1.2");
        }
    }
    /**
     * Called by logic that should only execute if logback 1.2.x or later is on the runtime classpath.
     * 
     * @throws IllegalStateException if the logback version is < 1.2 
     */
    public static void verifyLogback12OrAfter() {
        if (isLogback11OrBefore()) {
            throw new IllegalStateException("Logback 1.2+ method called, but Logback version is < 1.2");
        }
    }
    
    /**
     * Invokes the init method of a logback 1.1 encoder, with the given outputStream as the argument.
     */
    public static void init(Encoder<?> encoder, OutputStream outputStream) throws IOException {
        verifyLogback11OrBefore();
        try {
            ENCODER_INIT_METHOD.invoke(encoder, outputStream);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unable to initialize logback 1.1 encoder " + encoder, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to initialize logback 1.1 encoder " + encoder, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalStateException("Unable to initialize logback 1.1 encoder " + encoder, e.getCause());
            }
        }
    }
    
    /**
     * Invokes the doEncode method of a logback 1.1 encoder, with the given event as the argument.
     */
    public static void doEncode(Encoder<?> encoder, Object event) throws IOException {
        verifyLogback11OrBefore();
        try {
            ENCODER_DO_ENCODE_METHOD.invoke(encoder, event);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unable to encode event with logback 1.1 encoder " + encoder, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to encode event with logback 1.1 encoder " + encoder, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalStateException("Unable to encode event with logback 1.1 encoder " + encoder, e.getCause());
            }
        }
    }
    
    /**
     * Invokes the close method of a logback 1.1 encoder.
     */
    public static void close(Encoder<?> encoder) throws IOException {
        verifyLogback11OrBefore();
        try {
            ENCODER_CLOSE_METHOD.invoke(encoder);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unable to close logback 1.1 encoder " + encoder, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to close logback 1.1 encoder " + encoder, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalStateException("Unable to close logback 1.1 encoder " + encoder, e.getCause());
            }
        }
    }

    /**
     * Returns the specified method of the given class, or null if it can't be found.
     */
    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (SecurityException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
}