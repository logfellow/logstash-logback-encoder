/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logback.composite.loggingevent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.stacktrace.StackElementFilter;
import net.logstash.logback.stacktrace.ThrowableHasher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A JSON provider that adds a {@code stack_hash} Json field on a log with a stack trace
 * <p>
 * This hash is computed using {@link ThrowableHasher}
 * 
 * @author Pierre Smeyers
 */
public class StackHashJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public static final String FIELD_NAME = "stack_hash";

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

    private ThrowableHasher hasher = new ThrowableHasher();

    public StackHashJsonProvider() {
        setFieldName(FIELD_NAME);
    }

    @Override
    public void start() {
        if(!excludes.isEmpty()) {
            hasher = new ThrowableHasher(StackElementFilter.byPattern(excludes));
        }
        super.start();
    }

    public void addExclude(String exclusionPattern) {
        excludes.add(Pattern.compile(exclusionPattern));
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

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null && throwableProxy instanceof  ThrowableProxy) {
        	String hash = hasher.hexHash(((ThrowableProxy)event.getThrowableProxy()).getThrowable());
            JsonWritingUtils.writeStringField(generator, getFieldName(), hash);
        }
    }
}
