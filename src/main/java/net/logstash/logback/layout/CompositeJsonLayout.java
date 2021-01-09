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
package net.logstash.logback.layout;

import java.io.IOException;

import net.logstash.logback.LifeCycleManager;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.decorate.JsonFactoryDecorator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import net.logstash.logback.encoder.SeparatorParser;


public abstract class CompositeJsonLayout<Event extends DeferredProcessingAware> extends LayoutBase<Event> {

    private boolean immediateFlush = true;

    private Layout<Event> prefix;
    private Layout<Event> suffix;

    /**
     * Separator to use between events.
     *
     * <p>By default, this is null (for backwards compatibility), indicating no separator.
     * Note that this default is different than the default of {@link CompositeJsonEncoder#lineSeparator}.
     * In a future major release, the default will likely change to be the same as {@link CompositeJsonEncoder#lineSeparator}.</p>
     */
    private String lineSeparator;

    private final CompositeJsonFormatter<Event> formatter;

    /**
     * Manages the lifecycle of subcomponents
     */
    private final LifeCycleManager lifecycleManager = new LifeCycleManager();

    public CompositeJsonLayout() {
        super();
        this.formatter = createFormatter();
    }

    protected abstract CompositeJsonFormatter<Event> createFormatter();

    public String doLayout(Event event) {
        if (!isStarted()) {
            throw new IllegalStateException("Layout is not started");
        }
        final String result;
        try {
            result = formatter.writeEventAsString(event);
        } catch (IOException e) {
            addWarn("Error formatting logging event", e);
            return null;
        }

        if (prefix == null && suffix == null && lineSeparator == null) {
            return result;
        }

        String prefixResult = doLayoutWrapped(prefix, event);
        String suffixResult = doLayoutWrapped(suffix, event);

        int size = result.length()
                + (prefixResult == null ? 0 : prefixResult.length())
                + (suffixResult == null ? 0 : suffixResult.length())
                + (lineSeparator == null ? 0 : lineSeparator.length());

        StringBuilder stringBuilder = new StringBuilder(size);
        if (prefixResult != null) {
            stringBuilder.append(prefixResult);
        }
        stringBuilder.append(result);
        if (suffixResult != null) {
            stringBuilder.append(suffixResult);
        }
        if (lineSeparator != null) {
            stringBuilder.append(lineSeparator);
        }
        return stringBuilder.toString();
    }

    private String doLayoutWrapped(Layout<Event> wrapped, Event event) {
        return wrapped == null ? null : wrapped.doLayout(event);
    }

    @Override
    public void start() {
        super.start();
        formatter.setContext(getContext());
        lifecycleManager.start(formatter);
        startWrapped(prefix);
        startWrapped(suffix);
    }

    private void startWrapped(Layout<Event> wrapped) {
        if (wrapped == null) {
            return;
        }
        if (wrapped instanceof PatternLayoutBase) {
            /*
             * Don't ensure exception output (for ILoggingEvents)
             * or line separation (for IAccessEvents)
             */
            PatternLayoutBase<Event> layout = (PatternLayoutBase<Event>) wrapped;
            layout.setPostCompileProcessor(null);
            /*
             * The pattern will be re-parsed during start.
             * Needed so that the pattern is re-parsed without
             * the postCompileProcessor.
             */
            layout.start();
        }
        lifecycleManager.start(wrapped);
    }

    @Override
    public void stop() {
        super.stop();
        lifecycleManager.stop(formatter);
        stopWrapped(prefix);
        stopWrapped(suffix);
    }

    private void stopWrapped(Layout<Event> wrapped) {
        if (wrapped == null) {
            return;
        }
        if (wrapped instanceof PatternLayoutBase) {
            PatternLayoutBase<Event> layout = (PatternLayoutBase<Event>) wrapped;
            layout.stop();
        }
        lifecycleManager.stop(wrapped);
    }

    public JsonProviders<Event> getProviders() {
        return formatter.getProviders();
    }

    public void setProviders(JsonProviders<Event> jsonProviders) {
        formatter.setProviders(jsonProviders);
    }

    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }

    public JsonFactoryDecorator getJsonFactoryDecorator() {
        return formatter.getJsonFactoryDecorator();
    }

    public void setJsonFactoryDecorator(JsonFactoryDecorator jsonFactoryDecorator) {
        formatter.setJsonFactoryDecorator(jsonFactoryDecorator);
    }

    public JsonGeneratorDecorator getJsonGeneratorDecorator() {
        return formatter.getJsonGeneratorDecorator();
    }

    public void setJsonGeneratorDecorator(JsonGeneratorDecorator jsonGeneratorDecorator) {
        formatter.setJsonGeneratorDecorator(jsonGeneratorDecorator);
    }

    public void setFindAndRegisterJacksonModules(boolean findAndRegisterJacksonModules) {
        formatter.setFindAndRegisterJacksonModules(findAndRegisterJacksonModules);
    }

    protected CompositeJsonFormatter<Event> getFormatter() {
        return formatter;
    }

    public Layout<Event> getPrefix() {
        return prefix;
    }
    public void setPrefix(Layout<Event> prefix) {
        this.prefix = prefix;
    }

    public Layout<Event> getSuffix() {
        return suffix;
    }
    public void setSuffix(Layout<Event> suffix) {
        this.suffix = suffix;
    }
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets which lineSeparator to use between events.
     * <p>
     *
     * The following values have special meaning:
     * <ul>
     * <li><tt>null</tt> or empty string = no new line. (default)</li>
     * <li>"<tt>SYSTEM</tt>" = operating system new line.</li>
     * <li>"<tt>UNIX</tt>" = unix line ending (\n).</li>
     * <li>"<tt>WINDOWS</tt>" = windows line ending (\r\n).</li>
     * </ul>
     * <p>
     * Any other value will be used as given as the lineSeparator.
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = SeparatorParser.parseSeparator(lineSeparator);
    }

}
