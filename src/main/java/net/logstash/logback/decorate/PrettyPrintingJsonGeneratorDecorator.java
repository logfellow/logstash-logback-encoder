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
package net.logstash.logback.decorate;

import ch.qos.logback.core.CoreConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Enables pretty printing on the {@link JsonGenerator}
 */
public class PrettyPrintingJsonGeneratorDecorator implements JsonGeneratorDecorator {

    private DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
            .withRootSeparator(CoreConstants.EMPTY_STRING);

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        return generator.setPrettyPrinter(prettyPrinter);
    }

    /**
     * Sets the root separator used by the pretty printer.
     *
     * <p>Replaces occurrences of the string literal {@code [SPACE]} with a space character
     * to work around the fact that logback trims values read from xml before calling the setter.
     * Therefore, to set the root separator to a single space, you can specify
     * {@code <rootSeparator>[SPACE]</rootSeparator>} in the xml configuration.</p>
     *
     * @param rootSeparator the new root separator
     * @see DefaultPrettyPrinter#withRootSeparator(String)
     */
    public void setRootSeparator(String rootSeparator) {
        prettyPrinter = prettyPrinter.withRootSeparator(
                rootSeparator == null ? null : rootSeparator.replace("[SPACE]", " "));
    }

    /**
     * Sets whether spaces appear in object entries.
     *
     * @param spacesInObjectEntries whether spaces appear in object entries.
     * @see DefaultPrettyPrinter#withSpacesInObjectEntries()
     * @see DefaultPrettyPrinter#withoutSpacesInObjectEntries()
     */
    public void setSpacesInObjectEntries(boolean spacesInObjectEntries) {
        if (spacesInObjectEntries) {
            prettyPrinter = prettyPrinter.withSpacesInObjectEntries();
        } else {
            prettyPrinter = prettyPrinter.withoutSpacesInObjectEntries();
        }
    }
}
