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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import ch.qos.logback.access.PatternLayout;
import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.status.ErrorStatus;
import net.logstash.logback.CachingAbbreviator;
import net.logstash.logback.NullAbbreviator;

/**
 * A {@link ThrowableHandlingConverter} (similar to logback's {@link ThrowableProxyConverter})
 * that formats stacktraces by doing the following:
 * 
 * <ul>
 * <li>Limits the number of stackTraceElements per throwable
 *     (applies to each individual throwable.  e.g. caused-bys and suppressed).
 *     See {@link #maxDepthPerThrowable}.</li>
 * <li>Limits the total length in characters of the trace.
 *     See {@link #maxLength}.</li>
 * <li>Abbreviates class names based on the {@link #shortenedClassNameLength}.
 *     See {@link #shortenedClassNameLength}.</li>
 * <li>Filters out consecutive unwanted stackTraceElements based on regular expressions.
 *     See {@link #excludes}.</li>
 * <li>Uses evaluators to determine if the stacktrace should be logged.
 *     See {@link #evaluators}.</li>
 * <li>Outputs in either 'normal' order (root-cause-last), or root-cause-first.
 *     See {@link #rootCauseFirst}.</li>
 * </ul>
 * 
 * <p>
 * To use this with a {@link PatternLayout}, you must configure <tt>conversionRule</tt>
 * as described <a href="http://logback.qos.ch/manual/layouts.html#customConversionSpecifier">here</a>.
 * Options can be specified in the pattern in the following order:
 * <ol>
 * <li>maxDepthPerThrowable = "full" or "short" or an integer value</li>
 * <li>shortenedClassNameLength = "full" or "short" or an integer value</li>
 * <li>maxLength = "full" or "short" or an integer value</li>
 * </ol>
 * If any other remaining options are "rootFirst",
 * then the converter awill be configured as root-cause-first.
 * If any other remaining options equal to an evaluator name,
 * then the evaluator will be used to determine if the stacktrace should be printed.
 * Other options will be interpreted as exclusion regexes.
 * <p>
 * For example,
 * <pre>
 * {@code
 *     <conversionRule conversionWord="stack" 
 *                   converterClass="net.logstash.logback.stacktrace.ShortenedThrowableConverter" />
 *         
 *     <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *         <encoder>
 *             <pattern>[%thread] - %msg%n%stack{5,1024,10,rootFirst,regex1,regex2,evaluatorName}</pattern>
 *         </encoder>
 *     </appender>
 * }
 * </pre> 
 * 
 * 
 * 
 */
public class ShortenedThrowableConverter extends ThrowableHandlingConverter {
    
    public static final int FULL_MAX_DEPTH_PER_THROWABLE = Integer.MAX_VALUE;
    public static final int SHORT_MAX_DEPTH_PER_THROWABLE = 3;
    public static final int DEFAULT_MAX_DEPTH_PER_THROWABLE = FULL_MAX_DEPTH_PER_THROWABLE;
    
    public static final int FULL_MAX_LENGTH = Integer.MAX_VALUE;
    public static final int SHORT_MAX_LENGTH = 1024;
    public static final int DEFAULT_MAX_LENGTH = FULL_MAX_LENGTH;
    
    public static final int FULL_CLASS_NAME_LENGTH = Integer.MAX_VALUE;
    public static final int SHORT_CLASS_NAME_LENGTH = 10;
    public static final int DEFAULT_CLASS_NAME_LENGTH = FULL_CLASS_NAME_LENGTH;
    
    private static final String ELLIPSIS = "...";
    private static final int BUFFER_INITIAL_CAPACITY = 4096;
    
    private static final String OPTION_VALUE_FULL = "full";
    private static final String OPTION_VALUE_SHORT = "short";
    private static final String OPTION_VALUE_ROOT_FIRST = "rootFirst";
    private static final String OPTION_VALUE_INLINE_HASH = "inlineHash";

    private static final int OPTION_INDEX_MAX_DEPTH = 0;
    private static final int OPTION_INDEX_SHORTENED_CLASS_NAME = 1;
    private static final int OPTION_INDEX_MAX_LENGTH = 2;

    private AtomicInteger errorCount = new AtomicInteger();

    /**
     * Maximum number of stackTraceElements printed per throwable.
     */
    private int maxDepthPerThrowable = DEFAULT_MAX_DEPTH_PER_THROWABLE;
    
    /**
     * Maximum number of characters in the entire stacktrace.
     */
    private int maxLength = DEFAULT_MAX_LENGTH;
    
    /**
     * Will try to shorten class name lengths to less than this value
     */
    private int shortenedClassNameLength = DEFAULT_CLASS_NAME_LENGTH;

    /**
     * Abbreviator that will shorten the classnames if {@link #shortenedClassNameLength}
     * is set less than {@link #FULL_CLASS_NAME_LENGTH}
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
    private List<Pattern> excludes = new ArrayList<Pattern>(5);
    
    /**
     * True to print the root cause first.  False to print exceptions normally (root cause last).
     */
    private boolean rootCauseFirst;

    /**
     * True to compute and inline stack hashes.
     */
    private boolean inlineHash;

    private StackElementFilter stackElementFilter;

    private StackHasher stackHasher;

    /**
     * Evaluators that determine if the stacktrace should be logged.
     */
    private List<EventEvaluator<ILoggingEvent>> evaluators = new ArrayList<EventEvaluator<ILoggingEvent>>(1);
    
    @Override
    public void start() {
        parseOptions();
        // instantiate stack element filter
        if(excludes == null || excludes.isEmpty()) {
            if(inlineHash) {
                // filter out elements with no source info
                addInfo("[inlineHash] is active with no exclusion pattern: use non null source info filter to exclude generated classnames (see doc)");
                stackElementFilter = StackElementFilter.withSourceInfo();
            } else {
                // use any filter
                stackElementFilter = StackElementFilter.any();
            }
        } else {
            // use patterns filter
            stackElementFilter = StackElementFilter.byPattern(excludes);
        }
        // instantiate stack hasher if "inline hash" is active
        if(inlineHash) {
            stackHasher = new StackHasher(stackElementFilter);
        }
        super.start();
    }

    private void parseOptions() {
        List<String> optionList = getOptionList();

        if (optionList == null) {
            return;
        }
        final int optionListSize = optionList.size();
        for (int i = 0; i < optionListSize; i++) {
            String option = optionList.get(i);
            switch (i) {
                case OPTION_INDEX_MAX_DEPTH:
                    setMaxDepthPerThrowable(parseIntegerOptionValue(option, FULL_MAX_DEPTH_PER_THROWABLE, SHORT_MAX_DEPTH_PER_THROWABLE, DEFAULT_MAX_DEPTH_PER_THROWABLE));
                    break;
                case OPTION_INDEX_SHORTENED_CLASS_NAME:
                    setShortenedClassNameLength(parseIntegerOptionValue(option, FULL_CLASS_NAME_LENGTH, SHORT_CLASS_NAME_LENGTH, DEFAULT_CLASS_NAME_LENGTH));
                    break;
                case OPTION_INDEX_MAX_LENGTH:
                    setMaxLength(parseIntegerOptionValue(option, FULL_MAX_LENGTH, SHORT_MAX_LENGTH, DEFAULT_MAX_LENGTH));
                    break;
                default:
                    /*
                     * Remaining options are either
                     *     - "rootFirst" - indicating that stacks should be printed root-cause first
                     *     - "inlineHash" - indicating that hexadecimal error hashes should be computed and inlined
                     *     - evaluator name - name of evaluators that will determine if the stacktrace is ignored
                     *     - exclusion pattern - pattern for stack trace elements to exclude
                     */
                    if (OPTION_VALUE_ROOT_FIRST.equals(option)) {
                        setRootCauseFirst(true);
                    } else if(OPTION_VALUE_INLINE_HASH.equals(option)) {
                        setInlineHash(true);
                    } else {
                        @SuppressWarnings("rawtypes")
                        Map evaluatorMap = (Map) getContext().getObject(CoreConstants.EVALUATOR_MAP);
                        @SuppressWarnings("unchecked")
                        EventEvaluator<ILoggingEvent> evaluator = (evaluatorMap != null)
                            ? (EventEvaluator<ILoggingEvent>) evaluatorMap.get(option)
                            : null;

                        if (evaluator != null) {
                            addEvaluator(evaluator);
                        } else {
                            addExclude(option);
                        }
                    }
                    break;
            }
        }
    }

    private int parseIntegerOptionValue(String option, int valueIfFull, int valueIfShort, int valueIfNonParsable) {
        if (OPTION_VALUE_FULL.equals(option)) {
            return valueIfFull;
        } else if (OPTION_VALUE_SHORT.equals(option)) {
            return valueIfShort;
        } else {
            try {
                return Integer.parseInt(option);
            } catch (NumberFormatException nfe) {
                addError("Could not parse [" + option + "] as an integer");
                return valueIfNonParsable;
            }
        }
    }
    
    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null || isExcludedByEvaluator(event)) {
            return CoreConstants.EMPTY_STRING;
        }

        // compute stack trace hashes
        Deque<String> stackHashes = null;
        if(inlineHash && (throwableProxy instanceof ThrowableProxy)) {
            stackHashes = stackHasher.hexHashes(((ThrowableProxy) throwableProxy).getThrowable());
        }

        /*
         * The extra 100 gives a little more buffer room since we actually
         * go over the maxLength before detecting it and truncating.
         */
        StringBuilder builder = new StringBuilder(Math.min(BUFFER_INITIAL_CAPACITY, this.maxLength + 100 > 0 ? this.maxLength + 100 : this.maxLength));
        if (rootCauseFirst) {
            appendRootCauseFirst(builder, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, throwableProxy, stackHashes);
        } else {
            appendRootCauseLast(builder, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, throwableProxy, stackHashes);
        }
        if (builder.length() > maxLength) {
            builder.setLength(maxLength - ELLIPSIS.length());
            builder.append(ELLIPSIS);
        }
        return builder.toString();
    }

    /**
     * Return true if any evaluator returns true, indicating that
     * the stack trace should not be logged.
     */
    private boolean isExcludedByEvaluator(ILoggingEvent event) {
        for (int i = 0; i < evaluators.size(); i++) {
            EventEvaluator<ILoggingEvent> evaluator = evaluators.get(i);
            try {
                if (evaluator.evaluate(event)) {
                    return true;
                }
            } catch (EvaluationException eex) {
                int errors = errorCount.incrementAndGet();
                if (errors < CoreConstants.MAX_ERROR_COUNT) {
                    addError(String.format("Exception thrown for evaluator named [%s]", evaluator.getName()), eex);
                } else if (errors == CoreConstants.MAX_ERROR_COUNT) {
                    ErrorStatus errorStatus = new ErrorStatus(
                        String.format("Exception thrown for evaluator named [%s]", evaluator.getName()), this, eex);
                    errorStatus.add(new ErrorStatus(
                        "This was the last warning about this evaluator's errors."
                            + "We don't want the StatusManager to get flooded.",
                        this));
                    addStatus(errorStatus);
                }
            }
        }
        return false;
    }

    /**
     * Appends a throwable and recursively appends its causedby/suppressed throwables
     * in "normal" order (Root cause last).
     */
    private void appendRootCauseLast(
            StringBuilder builder,
            String prefix,
            int indent,
            IThrowableProxy throwableProxy,
            Deque<String> stackHashes) {
        
        if (throwableProxy == null || builder.length() > maxLength) {
            return;
        }

        String hash = stackHashes == null || stackHashes.isEmpty() ? null : stackHashes.removeFirst();
        appendFirstLine(builder, prefix, indent, throwableProxy, hash);
        appendStackTraceElements(builder, indent, throwableProxy);
        
        IThrowableProxy[] suppressedThrowableProxies = throwableProxy.getSuppressed();
        if (suppressedThrowableProxies != null) {
            for (IThrowableProxy suppressedThrowableProxy : suppressedThrowableProxies) {
            	// stack hashes are not computed/inlined on suppressed errors
                appendRootCauseLast(builder, CoreConstants.SUPPRESSED, indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT, suppressedThrowableProxy, null);
            }
        }
        appendRootCauseLast(builder, CoreConstants.CAUSED_BY, indent, throwableProxy.getCause(), stackHashes);
    }

    /**
     * Appends a throwable and recursively appends its causedby/suppressed throwables
     * in "reverse" order (Root cause first).
     */
    private void appendRootCauseFirst(
            StringBuilder builder,
            String prefix,
            int indent,
            IThrowableProxy throwableProxy,
            Deque<String> stackHashes) {
        
        if (throwableProxy == null || builder.length() > maxLength) {
            return;
        }
        
        if (throwableProxy.getCause() != null) {
            appendRootCauseFirst(builder, prefix, indent, throwableProxy.getCause(), stackHashes);
            prefix = CoreConstants.WRAPPED_BY;
        }

        String hash = stackHashes == null || stackHashes.isEmpty() ? null : stackHashes.removeLast();
        appendFirstLine(builder, prefix, indent, throwableProxy, hash);
        appendStackTraceElements(builder, indent, throwableProxy);
        
        IThrowableProxy[] suppressedThrowableProxies = throwableProxy.getSuppressed();
        if (suppressedThrowableProxies != null) {
            for (IThrowableProxy suppressedThrowableProxy : suppressedThrowableProxies) {
            	// stack hashes are not computed/inlined on suppressed errors
                appendRootCauseFirst(builder, CoreConstants.SUPPRESSED, indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT, suppressedThrowableProxy, null);
            }
        }
    }

    /**
     * Appends the frames of the throwable.
     */
    private void appendStackTraceElements(StringBuilder builder, int indent, IThrowableProxy throwableProxy) {
        
        if (builder.length() > maxLength) {
            return;
        }
        StackTraceElementProxy[] stackTraceElements = throwableProxy.getStackTraceElementProxyArray();
        int commonFrames = throwableProxy.getCommonFrames();

        boolean appendingExcluded = false;
        int consecutiveExcluded = 0;
        int appended = 0;
        StackTraceElementProxy previousWrittenStackTraceElement = null;
        for (int i = 0; i < stackTraceElements.length - commonFrames; i++) {
            if (maxDepthPerThrowable > 0 && appended >= maxDepthPerThrowable) {
                /*
                 * We reached the configure limit.  Bail out.
                 */
                appendPlaceHolder(builder, indent, stackTraceElements.length - commonFrames - maxDepthPerThrowable, "frames truncated");
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
                appendStackTraceElement(builder, indent, stackTraceElement, previousWrittenStackTraceElement);
                previousWrittenStackTraceElement = stackTraceElement;
                appendingExcluded = false;
                appended++;
            } else if (appendingExcluded) {
                /*
                 * We're going back and appending something we previously excluded
                 */
                appendStackTraceElement(builder, indent, stackTraceElement, previousWrittenStackTraceElement);
                previousWrittenStackTraceElement = stackTraceElement;
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
        builder.append(ELLIPSIS)
            .append(" ")
            .append(consecutiveExcluded)
            .append(" ")
            .append(message)
            .append(CoreConstants.LINE_SEPARATOR);
    }
    
    /**
     * Return true if the stack trace element is included (i.e. doesn't match any exclude patterns).
     */
    private boolean isIncluded(StackTraceElementProxy step) {
        return stackElementFilter.accept(step.getStackTraceElement());
    }
    
    /**
     * Appends a single stack trace element.
     */
    private void appendStackTraceElement(StringBuilder builder, int indent, StackTraceElementProxy step, StackTraceElementProxy previousStep) {
        if (builder.length() > maxLength) {
            return;
        }
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
        
        if (shouldAppendPackagingData(step, previousStep)) {
            appendPackagingData(builder, step);
        }
        builder.append(CoreConstants.LINE_SEPARATOR);
    }

    /**
     * Return true if packaging data should be appended for the current step.
     * 
     * Packaging data for the current step is only appended if it differs
     * from the packaging data from the previous step.
     */
    private boolean shouldAppendPackagingData(StackTraceElementProxy step, StackTraceElementProxy previousStep) {
        if (step == null || step.getClassPackagingData() == null) {
            return false;
        }
        if (previousStep == null || previousStep.getClassPackagingData() == null) {
            return true;
        }
        return !step.getClassPackagingData().equals(previousStep.getClassPackagingData());
    }

    private void appendPackagingData(StringBuilder builder, StackTraceElementProxy step) {
        ThrowableProxyUtil.subjoinPackagingData(builder, step);
    }

    /**
     * Appends the first line containing the prefix and throwable message 
     */
    private void appendFirstLine(StringBuilder builder, String prefix, int indent, IThrowableProxy throwableProxy, String hash) {
        if (builder.length() > maxLength) {
            return;
        }
        indent(builder, indent - 1);
        if (prefix != null) {
            builder.append(prefix);
        }
        if (hash != null) {
            // inline stack hash
            builder.append("<#" + hash + "> ");
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
        if (length <= 0) {
            throw new IllegalArgumentException();
        }
        this.shortenedClassNameLength = length;
        if (length < FULL_CLASS_NAME_LENGTH) {
            abbreviator = new CachingAbbreviator(new TargetLengthBasedClassNameAbbreviator(this.shortenedClassNameLength));
        } else {
            abbreviator = NullAbbreviator.INSTANCE;
        }
    }
    
    
    public int getMaxDepthPerThrowable() {
        return maxDepthPerThrowable;
    }
    public void setMaxDepthPerThrowable(int maxDepthPerThrowable) {
        if (maxDepthPerThrowable <= 0) {
            throw new IllegalArgumentException();
        }
        this.maxDepthPerThrowable = maxDepthPerThrowable;
    }
    
    public void setMaxLength(int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException();
        }
        this.maxLength = maxLength;
    }
    public int getMaxLength() {
        return maxLength;
    }
    
    public boolean isRootCauseFirst() {
        return rootCauseFirst;
    }
    public void setRootCauseFirst(boolean rootCauseFirst) {
        this.rootCauseFirst = rootCauseFirst;
    }

    public boolean isInlineHash() {
        return inlineHash;
    }

    public void setInlineHash(boolean inlineHash) {
        this.inlineHash = inlineHash;
    }

    protected void setStackHasher(StackHasher stackHasher) {
        this.stackHasher = stackHasher;
    }

    public void addExclude(String exclusionPattern) {
        excludes.add(Pattern.compile(exclusionPattern));
    }

    /**
     * Set exclusion patterns as a list of coma separated patterns
     * @param comaSeparatedPatterns list of coma separated patterns
     */
    public void setExclusions(String comaSeparatedPatterns) {
        if (comaSeparatedPatterns == null || comaSeparatedPatterns.isEmpty()) {
            this.excludes = new ArrayList<Pattern>(5);
        } else {
            setExcludes(Arrays.asList(comaSeparatedPatterns.split("\\s*\\,\\s*")));
        }
    }

    public void setExcludes(List<String> exclusionPatterns) {
        if (exclusionPatterns == null || exclusionPatterns.isEmpty()) {
            this.excludes = new ArrayList<Pattern>(5);
        } else {
            this.excludes = new ArrayList<Pattern>(exclusionPatterns.size());
            for (String pattern : exclusionPatterns) {
                addExclude(pattern);
            }
        }
    }
    
    public List<String> getExcludes() {
        List<String> exclusionPatterns = new ArrayList<String>(excludes.size());
        for (Pattern pattern : excludes) {
            exclusionPatterns.add(pattern.pattern());
        }
        return exclusionPatterns;
    }
    
    public void addEvaluator(EventEvaluator<ILoggingEvent> evaluator) {
        evaluators.add(evaluator);
    }
    
    public void setEvaluators(List<EventEvaluator<ILoggingEvent>> evaluators) {
        if (evaluators == null || evaluators.isEmpty()) {
            this.evaluators = new ArrayList<EventEvaluator<ILoggingEvent>>(1);
        } else {
            this.evaluators = new ArrayList<EventEvaluator<ILoggingEvent>>(evaluators);
        }
    }
    
    public List<EventEvaluator<ILoggingEvent>> getEvaluators() {
        return new ArrayList<EventEvaluator<ILoggingEvent>>(evaluators);
    }
    
}
