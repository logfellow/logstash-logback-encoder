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

import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.Util;

/**
 * Wrapper around slf4j Logger that provides type safe structural logging
 * The interface is the same as @see org.slf4j.Logger but every time an Object is allowed as an argumeng only a StructuredArgument is allowed
 *
 * Also the class acts as a replacement for org.slf4j.LoggerFactory, so you only need one import
 * To reduce boilerplate code further a method is provided that will get the Logger class from the call stack
 */
public class Logger {
  private final org.slf4j.Logger delegate;

  public static Logger getLogger(Class<?> clazz) {
    return new Logger(LoggerFactory.getLogger(clazz));
  }

  public static Logger getLogger(String name) {
    return new Logger(LoggerFactory.getLogger(name));
  }

  /**
   * Get the Logger dynamically from the call stack
   * This should be safe to use as long as your Logger is declared as static final
   * @return Logger with autodetected class name (or ROOT in case of errors)
   */
  public static Logger getLogger() {
    // https://github.com/qos-ch/slf4j/pull/167
    Class<?> autoComputedCallingClass = Util.getCallingClass();
    if (autoComputedCallingClass != null) {
      return getLogger(autoComputedCallingClass);
    } else {
      Util.report("Failed to detect logger name from caller.");
      return getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }
  }

  public Logger(org.slf4j.Logger logger) {
    this.delegate = logger;
  }

  /**
   * Return the name of this <code>Logger</code> instance.
   * @return name of this logger instance
   */
  public String getName() {
    return delegate.getName();
  }

  /**
   * Is the logger instance enabled for the TRACE level?
   *
   * @return True if this Logger is enabled for the TRACE level,
   *         false otherwise.
   * @since 1.4
   */
  public boolean isTraceEnabled() {
    return delegate.isTraceEnabled();
  }

  /**
   * Log a message at the TRACE level.
   *
   * @param msg the message string to be logged
   * @since 1.4
   */
  public void trace(String msg) {
    delegate.trace(msg);
  }

  /**
   * Log a message at the TRACE level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the TRACE level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   * @since 1.4
   */
  public void trace(String format, StructuredArgument arg) {
    delegate.trace(format, arg);
  }

  /**
   * Log a message at the TRACE level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the TRACE level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   * @since 1.4
   */
  public void trace(String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.trace(format, arg1, arg2);
  }

  /**
   * Log a message at the TRACE level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the TRACE level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>StructuredArgument[]</code> before invoking the method,
   * even if this logger is disabled for TRACE. The variants taking {@link #trace(String, StructuredArgument) one} and
   * {@link #trace(String, StructuredArgument, StructuredArgument) two} arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   * @since 1.4
   */
  public void trace(String format, StructuredArgument... arguments) {
    delegate.trace(format, arguments);
  }

  /**
   * Log an exception (throwable) at the TRACE level with an
   * accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   * @since 1.4
   */
  public void trace(String msg, Throwable t) {
    delegate.trace(msg, t);
  }

  /**
   * Similar to {@link #isTraceEnabled()} method except that the
   * marker data is also taken into account.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the TRACE level,
   *         false otherwise.
   *
   * @since 1.4
   */
  public boolean isTraceEnabled(Marker marker) {
    return delegate.isTraceEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @since 1.4
   */
  public void trace(Marker marker, String msg) {
    delegate.trace(marker, msg);
  }

  /**
   * This method is similar to {@link #trace(String, StructuredArgument)} method except that the
   * marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   * @since 1.4
   */
  public void trace(Marker marker, String format, StructuredArgument arg) {
    delegate.trace(marker, format, arg);
  }

  /**
   * This method is similar to {@link #trace(String, StructuredArgument, StructuredArgument)}
   * method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   * @since 1.4
   */
  public void trace(Marker marker, String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.trace(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #trace(String, StructuredArgument...)}
   * method except that the marker data is also taken into
   * consideration.
   *
   * @param marker   the marker data specific to this log statement
   * @param format   the format string
   * @param argArray an array of arguments
   * @since 1.4
   */
  public void trace(Marker marker, String format, StructuredArgument... argArray) {
    delegate.trace(marker, format, argArray);
  }

  /**
   * This method is similar to {@link #trace(String, Throwable)} method except that the
   * marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   * @since 1.4
   */
  public void trace(Marker marker, String msg, Throwable t) {
    delegate.trace(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level,
   *         false otherwise.
   */
  public boolean isDebugEnabled() {
    return delegate.isDebugEnabled();
  }

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  public void debug(String msg) {
    delegate.debug(msg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  public void debug(String format, StructuredArgument arg) {
    delegate.debug(format, arg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void debug(String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.debug(format, arg1, arg2);
  }

  /**
   * Log a message at the DEBUG level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>StructuredArgument[]</code> before invoking the method,
   * even if this logger is disabled for DEBUG. The variants taking
   * {@link #debug(String, StructuredArgument) one} and {@link #debug(String, StructuredArgument, StructuredArgument) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void debug(String format, StructuredArgument... arguments) {
    delegate.debug(format, arguments);
  }

  /**
   * Log an exception (throwable) at the DEBUG level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void debug(String msg, Throwable t) {
    delegate.debug(msg, t);
  }

  /**
   * Similar to {@link #isDebugEnabled()} method except that the
   * marker data is also taken into account.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the DEBUG level,
   *         false otherwise.
   */
  public boolean isDebugEnabled(Marker marker) {
    return delegate.isDebugEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *  @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  public void debug(Marker marker, String msg) {
    delegate.debug(marker, msg);
  }

  /**
   * This method is similar to {@link #debug(String, StructuredArgument)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void debug(Marker marker, String format, StructuredArgument arg) {
    delegate.debug(marker, format, arg);
  }

  /**
   * This method is similar to {@link #debug(String, StructuredArgument, StructuredArgument)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void debug(Marker marker, String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.debug(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #debug(String, StructuredArgument...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void debug(Marker marker, String format, StructuredArgument... arguments) {
    delegate.debug(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #debug(String, Throwable)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void debug(Marker marker, String msg, Throwable t) {
    delegate.debug(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level,
   *         false otherwise.
   */
  public boolean isInfoEnabled() {
    return delegate.isInfoEnabled();
  }

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  public void info(String msg) {
    delegate.info(msg);
  }

  /**
   * Log a message at the INFO level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  public void info(String format, StructuredArgument arg) {
    delegate.info(format, arg);
  }

  /**
   * Log a message at the INFO level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void info(String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.info(format, arg1, arg2);
  }

  /**
   * Log a message at the INFO level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the INFO level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>StructuredArgument[]</code> before invoking the method,
   * even if this logger is disabled for INFO. The variants taking
   * {@link #info(String, StructuredArgument) one} and {@link #info(String, StructuredArgument, StructuredArgument) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void info(String format, StructuredArgument... arguments) {
    delegate.info(format, arguments);
  }

  /**
   * Log an exception (throwable) at the INFO level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void info(String msg, Throwable t) {
    delegate.info(msg, t);
  }

  /**
   * Similar to {@link #isInfoEnabled()} method except that the marker
   * data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return true if this logger is warn enabled, false otherwise
   */
  public boolean isInfoEnabled(Marker marker) {
    return delegate.isInfoEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the INFO level.
   *  @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  public void info(Marker marker, String msg) {
    delegate.info(marker, msg);
  }

  /**
   * This method is similar to {@link #info(String, StructuredArgument)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void info(Marker marker, String format, StructuredArgument arg) {
    delegate.info(marker, format, arg);
  }

  /**
   * This method is similar to {@link #info(String, StructuredArgument, StructuredArgument)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void info(Marker marker, String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.info(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #info(String, StructuredArgument...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void info(Marker marker, String format, StructuredArgument... arguments) {
    delegate.info(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #info(String, Throwable)} method
   * except that the marker data is also taken into consideration.
   *  @param marker the marker data for this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void info(Marker marker, String msg, Throwable t) {
    delegate.info(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return True if this Logger is enabled for the WARN level,
   *         false otherwise.
   */
  public boolean isWarnEnabled() {
    return delegate.isWarnEnabled();
  }

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  public void warn(String msg) {
    delegate.warn(msg);
  }

  /**
   * Log a message at the WARN level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  public void warn(String format, StructuredArgument arg) {
    delegate.warn(format, arg);
  }

  /**
   * Log a message at the WARN level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the WARN level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>StructuredArgument[]</code> before invoking the method,
   * even if this logger is disabled for WARN. The variants taking
   * {@link #warn(String, StructuredArgument) one} and {@link #warn(String, StructuredArgument, StructuredArgument) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void warn(String format, StructuredArgument... arguments) {
    delegate.warn(format, arguments);
  }

  /**
   * Log a message at the WARN level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void warn(String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.warn(format, arg1, arg2);
  }

  /**
   * Log an exception (throwable) at the WARN level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void warn(String msg, Throwable t) {
    delegate.warn(msg, t);
  }

  /**
   * Similar to {@link #isWarnEnabled()} method except that the marker
   * data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the WARN level,
   *         false otherwise.
   */
  public boolean isWarnEnabled(Marker marker) {
    return delegate.isWarnEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the WARN level.
   *  @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  public void warn(Marker marker, String msg) {
    delegate.warn(marker, msg);
  }

  /**
   * This method is similar to {@link #warn(String, StructuredArgument)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void warn(Marker marker, String format, StructuredArgument arg) {
    delegate.warn(marker, format, arg);
  }

  /**
   * This method is similar to {@link #warn(String, StructuredArgument, StructuredArgument)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void warn(Marker marker, String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.warn(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #warn(String, StructuredArgument...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void warn(Marker marker, String format, StructuredArgument... arguments) {
    delegate.warn(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #warn(String, Throwable)} method
   * except that the marker data is also taken into consideration.
   *  @param marker the marker data for this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void warn(Marker marker, String msg, Throwable t) {
    delegate.warn(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return True if this Logger is enabled for the ERROR level,
   *         false otherwise.
   */
  public boolean isErrorEnabled() {
    return delegate.isErrorEnabled();
  }

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  public void error(String msg) {
    delegate.error(msg);
  }

  /**
   * Log a message at the ERROR level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  public void error(String format, StructuredArgument arg) {
    delegate.error(format, arg);
  }

  /**
   * Log a message at the ERROR level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void error(String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.error(format, arg1, arg2);
  }

  /**
   * Log a message at the ERROR level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the ERROR level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>StructuredArgument[]</code> before invoking the method,
   * even if this logger is disabled for ERROR. The variants taking
   * {@link #error(String, StructuredArgument) one} and {@link #error(String, StructuredArgument, StructuredArgument) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void error(String format, StructuredArgument... arguments) {
    delegate.error(format, arguments);
  }

  /**
   * Log an exception (throwable) at the ERROR level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void error(String msg, Throwable t) {
    delegate.error(msg, t);
  }

  /**
   * Similar to {@link #isErrorEnabled()} method except that the
   * marker data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the ERROR level,
   *         false otherwise.
   */
  public boolean isErrorEnabled(Marker marker) {
    return delegate.isErrorEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the ERROR level.
   *  @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  public void error(Marker marker, String msg) {
    delegate.error(marker, msg);
  }

  /**
   * This method is similar to {@link #error(String, StructuredArgument)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void error(Marker marker, String format, StructuredArgument arg) {
    delegate.error(marker, format, arg);
  }

  /**
   * This method is similar to {@link #error(String, StructuredArgument, StructuredArgument)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void error(Marker marker, String format, StructuredArgument arg1, StructuredArgument arg2) {
    delegate.error(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #error(String, StructuredArgument...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void error(Marker marker, String format, StructuredArgument... arguments) {
    delegate.error(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #error(String, Throwable)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void error(Marker marker, String msg, Throwable t) {
    delegate.error(marker, msg, t);
  }
}
