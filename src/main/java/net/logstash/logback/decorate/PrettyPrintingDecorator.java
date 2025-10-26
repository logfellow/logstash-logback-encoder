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
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;

/**
 * Enables pretty printing on a {@link MapperBuilder}.
 */
public class PrettyPrintingDecorator<M extends ObjectMapper, B extends MapperBuilder<M, B>> implements MapperBuilderDecorator<M, B> {

    private static final DefaultPrettyPrinter.FixedSpaceIndenter DEFAULT_ARRAY_INDENTER = DefaultPrettyPrinter.FixedSpaceIndenter.instance();

    private Separators separators = PrettyPrinter.DEFAULT_SEPARATORS
            .withRootSeparator(CoreConstants.EMPTY_STRING);

    private DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(separators)
            .withArrayIndenter(DEFAULT_ARRAY_INDENTER);

    @Override
    public B decorate(B mapperBuilder) {
        return mapperBuilder
                .enable(SerializationFeature.INDENT_OUTPUT)
                .defaultPrettyPrinter(prettyPrinter);
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
     * @see Separators#withRootSeparator(String)
     */
    public void setRootSeparator(String rootSeparator) {
        separators = separators.withRootSeparator(rootSeparator == null ? null : rootSeparator.replace("[SPACE]", " "));
        prettyPrinter = prettyPrinter.withSeparators(separators);
    }

    /**
     * Sets whether spaces appear in object entries.
     *
     * @param spacesInObjectEntries whether spaces appear in object entries.
     * @see Separators#withObjectEntrySpacing(Separators.Spacing)
     */
    public void setSpacesInObjectEntries(boolean spacesInObjectEntries) {
        separators = separators.withObjectNameValueSpacing(spacesInObjectEntries ? Separators.Spacing.BOTH : Separators.Spacing.NONE);
        prettyPrinter = prettyPrinter.withSeparators(separators);
    }

    /**
     * Sets whether arrays are indented with a new line.
     *
     * @param indentArraysWithNewLine whether arrays are indented with a new line.
     */
    public void setIndentArraysWithNewLine(boolean indentArraysWithNewLine) {
        if (indentArraysWithNewLine) {
            prettyPrinter = prettyPrinter.withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        } else {
            prettyPrinter = prettyPrinter.withArrayIndenter(DEFAULT_ARRAY_INDENTER);
        }
    }
}
