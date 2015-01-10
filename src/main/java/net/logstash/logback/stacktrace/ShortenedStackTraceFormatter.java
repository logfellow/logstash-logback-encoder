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
package net.logstash.logback.stacktrace;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.logstash.logback.CachingAbbreviator;
import net.logstash.logback.NullAbbreviator;
import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;

/**
 * A {@link StackTraceFormatter} that shortens stacktraces by doing the following:
 * 
 * <ul>
 * <li>Limits the number of stackTraceElements per throwable
 *     (applies to each individual throwable.  e.g. caused-bys and suppressed).</li>
 * <li>Abbreviates class names based on the {@link #shortenedClassNameLength}.</li>
 * <li>Filters out consecutive unwanted stackTraceElements based on regular expressions.</li>
 * </ul>
 */
public class ShortenedStackTraceFormatter implements StackTraceFormatter {
    
    public static final int UNLIMITED_STACK_TRACE_ELEMENTS = -1;
    public static final int DEFAULT_MAX_STACK_TRACE_ELEMENTS = 25;
    
    private static final int BUFFER_INITIAL_CAPACITY = 4096;
    private static final int INDENTION = 1;

    /**
     * Maximum number of stackTraceElements printed per throwable.
     */
    private int maxStackTraceElementsPerThrowable = DEFAULT_MAX_STACK_TRACE_ELEMENTS;
    
    /**
     * When set to anything >= 0 we will try to abbreviate the class names in stacktraces
     */
    private int shortenedClassNameLength = -1;

    /**
     * Abbreviator that will shorten the classnames if {@link #shortenedClassNameLength} is set
     */
    private Abbreviator abbreviator = NullAbbreviator.INSTANCE;
    
    /**
     * Patterns used to determine which stacktrace elements to exclude.
     * 
     * The strings being matched against are in the form "fullyQualifiedClassName.methodName"
     * (e.g. "java.lang.Object.toString").
     * 
     * Note that these elements will only be excluded if and only if
     * more than one consecutive line matches an exclusion pattern. 
     */
    private List<Pattern> excludes = new ArrayList<Pattern>();
    
    @Override
    public String format(IThrowableProxy throwableProxy) {
        StringBuilder builder = new StringBuilder(BUFFER_INITIAL_CAPACITY);
        append(builder, null, INDENTION, throwableProxy);
        return builder.toString();
    }

    /**
     * Appends a throwable and recursively appends its causedby/suppressed throwables.
     */
    private void append(
            StringBuilder builder,
            String prefix,
            int indent,
            IThrowableProxy throwableProxy) {
        
        if (throwableProxy == null) {
            return;
        }
        appendFirstLine(builder, prefix, indent, throwableProxy);
        appendStackTraceElements(builder, indent, throwableProxy);
        
        IThrowableProxy[] suppressedThrowableProxies = throwableProxy.getSuppressed();
        if (suppressedThrowableProxies != null) {
            for (IThrowableProxy suppressedThrowableProxy : suppressedThrowableProxies) {
                append(builder, CoreConstants.SUPPRESSED, indent + INDENTION, suppressedThrowableProxy);
            }
        }
        append(builder, CoreConstants.CAUSED_BY, indent, throwableProxy.getCause());
    }

    /**
     * Appends the frames of the throwable.
     */
    private void appendStackTraceElements(StringBuilder builder, int indent, IThrowableProxy throwableProxy) {
        
        StackTraceElementProxy[] stackTraceElements = throwableProxy.getStackTraceElementProxyArray();
        int commonFrames = throwableProxy.getCommonFrames();

        boolean appendingExcluded = false;
        int consecutiveExcluded = 0;
        int appended = 0;
        for (int i = 0; i < stackTraceElements.length - commonFrames; i++) {
            if (maxStackTraceElementsPerThrowable > 0 && appended >= maxStackTraceElementsPerThrowable) {
                /*
                 * We reached the configure limit.  Bail out.
                 */
                appendPlaceHolder(builder, indent, stackTraceElements.length - commonFrames - maxStackTraceElementsPerThrowable, "frames truncated");
                break;
            }
            StackTraceElementProxy stackTraceElement = stackTraceElements[i];
            if (i <= 1 || isIncluded(stackTraceElement)) {
                /*
                 * We should append this line.
                 * 
                 * consecutiveExcluded will be > 0 if we were previously skipping lines based on excludes
                 */
                if (consecutiveExcluded >= 2) {
                    /*
                     * Multiple consecutive lines were excluded, so append a placeholder 
                     */
                    appendPlaceHolder(builder, indent, consecutiveExcluded, "frames excluded");
                    consecutiveExcluded = 0;
                } else if (consecutiveExcluded == 1) {
                    /*
                     * We only excluded one line, so just go back and include it
                     * instead of printing the excluding message for it.
                     */
                    appendingExcluded = true;
                    consecutiveExcluded = 0;
                    i -= 2;
                    continue;
                }
                appendStackTraceElement(builder, indent, stackTraceElement);
                appendingExcluded = false;
                appended++;
            } else if (appendingExcluded) {
                /*
                 * We're going back and appending something we previously excluded
                 */
                appendStackTraceElement(builder, indent, stackTraceElement);
                appended++;
            } else {
                consecutiveExcluded++;
            }
        }
        
        if (consecutiveExcluded > 0) {
            /*
             * We were excluding stuff at the end, so append a placeholder
             */
            appendPlaceHolder(builder, indent, consecutiveExcluded, "frames excluded");
        }
        
        if (commonFrames > 0) {
            /*
             * Common frames found, append a placeholder
             */
            appendPlaceHolder(builder, indent, commonFrames, "common frames omitted");
        }

    }

    /**
     * Appends a placeholder indicating that some frames were not written. 
     */
    private void appendPlaceHolder(StringBuilder builder, int indent, int consecutiveExcluded, String message) {
        indent(builder, indent);
        builder.append("... ")
            .append(consecutiveExcluded)
            .append(" ")
            .append(message)
            .append(CoreConstants.LINE_SEPARATOR);
    }
    
    /**
     * Return true if the stack trace element is included (i.e. doesn't match any exclude patterns).
     */
    private boolean isIncluded(StackTraceElementProxy step) {
        if (!excludes.isEmpty()) {
            StackTraceElement stackTraceElement = step.getStackTraceElement();
            String testString = stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName();
            
            for (Pattern exclusionPattern : excludes) {
                if (exclusionPattern.matcher(testString).find()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Appends a single stack trace element.
     */
    private void appendStackTraceElement(StringBuilder builder, int indent, StackTraceElementProxy step) {
        indent(builder, indent);
        
        StackTraceElement stackTraceElement = step.getStackTraceElement();
        
        String fileName = stackTraceElement.getFileName();
        int lineNumber = stackTraceElement.getLineNumber();
        builder.append("at ")
            .append(abbreviator.abbreviate(stackTraceElement.getClassName()))
            .append(".")
            .append(stackTraceElement.getMethodName())
            .append("(")
            .append(fileName == null ? "Unknown Source" : fileName);
        
        if (lineNumber >= 0) {
            builder.append(":")
                .append(lineNumber);
        }
        builder.append(")");
        
        appendPackagingData(builder, step);
        builder.append(CoreConstants.LINE_SEPARATOR);
    }

    private void appendPackagingData(StringBuilder builder, StackTraceElementProxy step) {
        ThrowableProxyUtil.subjoinPackagingData(builder, step);
    }

    /**
     * Appends the first line containing the prefix and throwable message 
     */
    private void appendFirstLine(StringBuilder builder, String prefix, int indent, IThrowableProxy throwableProxy) {
        indent(builder, indent - 1);
        if (prefix != null) {
            builder.append(prefix);
        }
        builder.append(abbreviator.abbreviate(throwableProxy.getClassName()))
            .append(": ")
            .append(throwableProxy.getMessage())
            .append(CoreConstants.LINE_SEPARATOR);
    }
    
    private void indent(StringBuilder builder, int indent) {
        ThrowableProxyUtil.indent(builder, indent);
    }

    public int getShortenedClassNameLength() {
        return shortenedClassNameLength;
    }
    
    public void setShortenedClassNameLength(int length) {
        this.shortenedClassNameLength = length;
        if (length >= 0) {
            abbreviator = new CachingAbbreviator(new TargetLengthBasedClassNameAbbreviator(this.shortenedClassNameLength));
        } else {
            abbreviator = NullAbbreviator.INSTANCE;
        }
    }
    
    
    public int getMaxStackTraceElementsPerThrowable() {
        return maxStackTraceElementsPerThrowable;
    }
    public void setMaxStackTraceElementsPerThrowable(int maxStackTraceElementsPerSegment) {
        this.maxStackTraceElementsPerThrowable = maxStackTraceElementsPerSegment;
    }
    
    public void addExclude(String exclusionPattern) {
        excludes.add(Pattern.compile(exclusionPattern));
    }
    
    public void setExcludes(List<String> exclusionPatterns) {
        if (exclusionPatterns == null || exclusionPatterns.isEmpty()) {
            this.excludes = new ArrayList<Pattern>();
        } else {
            this.excludes = new ArrayList<Pattern>();
            for (String pattern : exclusionPatterns) {
                addExclude(pattern);
            }
        }
    }
    
}
