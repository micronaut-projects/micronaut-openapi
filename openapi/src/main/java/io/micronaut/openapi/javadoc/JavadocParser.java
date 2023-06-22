/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.javadoc;

import java.util.Set;

import io.micronaut.core.util.CollectionUtils;

import com.github.chhorz.javadoc.JavaDoc;
import com.github.chhorz.javadoc.JavaDocParser;
import com.github.chhorz.javadoc.JavaDocParserBuilder;
import com.github.chhorz.javadoc.OutputType;
import com.github.chhorz.javadoc.tags.ParamTag;
import com.github.chhorz.javadoc.tags.PropertyTag;
import com.github.chhorz.javadoc.tags.ReturnTag;
import com.github.chhorz.javadoc.tags.Tag;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

/**
 * Very simple javadoc parser that can used to parse out the first paragraph description and parameter / return descriptions.
 * Most other tags are simply stripped and ignored.
 *
 * @author graemerocher
 * @since 1.0
 */
public class JavadocParser {

    private static final Set<String> IGNORED = CollectionUtils.setOf("see", "since", "author", "version", "deprecated", "throws", "exception", "category");

    private final FlexmarkHtmlConverter htmlToMarkdownConverter = FlexmarkHtmlConverter.builder()
            .build();

    /**
     * Parse the javadoc in a {@link JavadocDescription}.
     *
     * @param text The text
     *
     * @return The description
     */
    public JavadocDescription parse(String text) {

        if (text == null) {
            return null;
        }

        JavaDocParser javaDocParser = JavaDocParserBuilder
            .withAllKnownTags()
            .withOutputType(OutputType.HTML)
            .build();

        JavaDoc javaDoc = javaDocParser.parse(text.strip());

        JavadocDescription javadocDescription = new JavadocDescription();
        javadocDescription.setMethodSummary(htmlToMarkdownConverter.convert(javaDoc.getSummary()).strip());
        javadocDescription.setMethodDescription(htmlToMarkdownConverter.convert(javaDoc.getDescription()).strip());

        if (CollectionUtils.isNotEmpty(javaDoc.getTags())) {
            for (Tag tag : javaDoc.getTags()) {
                if (IGNORED.contains(tag.getTagName())) {
                    continue;
                }
                if (tag instanceof ReturnTag returnTag) {
                    javadocDescription.setReturnDescription(htmlToMarkdownConverter.convert(returnTag.getDescription()).strip());
                } else if (tag instanceof ParamTag paramTag) {
                    String paramDesc = htmlToMarkdownConverter.convert(paramTag.getParamDescription()).strip();
                    javadocDescription.getParameters().put(paramTag.getParamName(), paramDesc);
                } else if (tag instanceof PropertyTag propertyTag) {
                    String paramDesc = htmlToMarkdownConverter.convert(propertyTag.getParamDescription()).strip();
                    javadocDescription.getParameters().put(propertyTag.getPropertyName(), paramDesc);
                }
            }
        }

        return javadocDescription;
    }
}
