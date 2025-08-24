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

import com.github.chhorz.javadoc.JavaDoc;
import com.github.chhorz.javadoc.JavaDocParser;
import com.github.chhorz.javadoc.JavaDocParserBuilder;
import com.github.chhorz.javadoc.OutputType;
import com.github.chhorz.javadoc.tags.DeprecatedTag;
import com.github.chhorz.javadoc.tags.ParamTag;
import com.github.chhorz.javadoc.tags.PropertyTag;
import com.github.chhorz.javadoc.tags.ReturnTag;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.aside.AsideExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacterExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.misc.Extension;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.micronaut.openapi.javadoc.DocsFormat.HTML_TO_MD;
import static io.micronaut.openapi.javadoc.DocsFormat.MD_TO_HTML;
import static io.micronaut.openapi.visitor.ContextUtils.warn;

/**
 * Very simple javadoc parser that can used to parse out the first paragraph description and parameter / return descriptions.
 * Most other tags are simply stripped and ignored.
 *
 * @author graemerocher
 * @since 1.0
 */
public class JavadocParser {

    private static final String TAG_OPEN_P = "<p>";
    private static final String TAG_CLOSE_P = "</p>";

    private static final Set<String> IGNORED = CollectionUtils.setOf("see", "since", "author", "version", "throws", "exception", "category");

    private final DocsFormat docsFormat;
    private final JavaDocParser javaDocParser;
    @Nullable
    private final VisitorContext context;
    private FlexmarkHtmlConverter htmlToMarkdownConverter;
    private Parser mdParser;
    private HtmlRenderer htmlRenderer;

    public JavadocParser(DocsFormat docsFormat) {
        this(docsFormat, null);
    }

    public JavadocParser(DocsFormat docsFormat, @Nullable VisitorContext context) {
        this.docsFormat = docsFormat;
        this.context = context;
        javaDocParser = JavaDocParserBuilder
            .withAllKnownTags()
            .withOutputType(OutputType.HTML)
            .build();

        List<Extension> extensions = Collections.emptyList();
        if (docsFormat == MD_TO_HTML || docsFormat == HTML_TO_MD) {

            extensions = List.of(
                AbbreviationExtension.create(),
                AnchorLinkExtension.create(),
                AsideExtension.create(),
                AutolinkExtension.create(),
                DefinitionExtension.create(),
                EmojiExtension.create(),
                EscapedCharacterExtension.create(),
                FootnoteExtension.create(),
                SubscriptExtension.create(),
                TaskListExtension.create(),
                InsExtension.create(),
                JekyllFrontMatterExtension.create(),
                SuperscriptExtension.create(),
                TablesExtension.create(),
                SimTocExtension.create(),
                TocExtension.create(),
                TypographicExtension.create(),
                WikiLinkExtension.create(),
                YamlFrontMatterExtension.create()
            );
        }


        if (docsFormat == MD_TO_HTML) {
            htmlRenderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();

            mdParser = Parser.builder()
                .extensions(extensions)
                .build();
        } else if (docsFormat == HTML_TO_MD) {
            htmlToMarkdownConverter = FlexmarkHtmlConverter.builder()
                .extensions(extensions)
                .build();
        }
    }

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

        JavaDoc javaDoc = javaDocParser.parse(text.strip());

        var javadocDescription = new JavadocDescription();

        javadocDescription.setMethodSummary(format(javaDoc.getSummary(), true));
        javadocDescription.setMethodDescription(format(javaDoc.getDescription()));

        if (CollectionUtils.isNotEmpty(javaDoc.getTags())) {
            for (var tag : javaDoc.getTags()) {
                if (IGNORED.contains(tag.getTagName())) {
                    continue;
                }
                if (tag instanceof ReturnTag returnTag) {
                    javadocDescription.setReturnDescription(format(returnTag.getDescription(), true));
                } else if (tag instanceof ParamTag paramTag) {
                    javadocDescription.getParameters().put(paramTag.getParamName(), format(paramTag.getParamDescription(), true));
                } else if (tag instanceof PropertyTag propertyTag) {
                    javadocDescription.getParameters().put(propertyTag.getPropertyName(), format(propertyTag.getParamDescription(), true));
                } else if (tag instanceof DeprecatedTag deprecatedTag) {
                    javadocDescription.setDeprecatedDescription(format(deprecatedTag.getDeprecatedText(), true));
                }
            }
        }

        return javadocDescription;
    }

    private String format(String text) {
        return format(text, false);
    }

    private String format(String text, boolean withRemoveP) {
        try {
            return switch (docsFormat) {
                case PLAIN -> text.strip();
                case HTML_TO_MD -> htmlToMarkdownConverter.convert(text.strip()).trim();
                case MD_TO_HTML -> {
                    var result = htmlRenderer.render(mdParser.parse(text.strip())).trim();
                    if (withRemoveP && result.startsWith(TAG_OPEN_P)) {
                        result = result.substring(TAG_OPEN_P.length());
                        result = result.substring(0, result.indexOf(TAG_CLOSE_P));
                    }
                    result = result.replaceAll("&ldquo;|&rdquo;|&quot;", "\"");
                    yield result;
                }
            };
        } catch (Exception e) {
            warn("Error with converting javadoc: " + e.getMessage(), context);
            return text.strip();
        }
    }
}
