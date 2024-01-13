/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.openapi.http.uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import io.micronaut.core.annotation.Internal;

/**
 * Extends {@link UriTemplate}. Simplified copy of UriMatchTemplate class from http module.
 *
 * @since 6.5.0
 */
@Internal
public final class UriMatchTemplate extends UriTemplate {

    private static final String VARIABLE_MATCH_PATTERN = "([^\\/\\?#(?!\\{)&;\\+]";

    private StringBuilder pattern;
    private List<UriMatchVariable> variables;
    private final Pattern matchPattern;
    private final boolean exactMatch;

    /**
     * Construct a new URI template for the given template.
     *
     * @param templateString The template string
     */
    public UriMatchTemplate(CharSequence templateString) {
        this(templateString, new Object[0]);
    }

    /**
     * Construct a new URI template for the given template.
     *
     * @param templateString The template string
     * @param parserArguments The parsed arguments
     */
    protected UriMatchTemplate(CharSequence templateString, Object... parserArguments) {
        super(templateString, parserArguments);
        if (variables.isEmpty() && Pattern.quote(templateString.toString()).contentEquals(pattern)) {
            // if there are no variables and a match pattern matches template we can assume it matches exactly
            matchPattern = null;
            exactMatch = true;
        } else {
            matchPattern = Pattern.compile(pattern.toString());
            exactMatch = false;
        }
        // cleanup / reduce memory consumption
        pattern = null;
    }

    /**
     * @param templateString The template
     * @param segments The list of segments
     * @param matchPattern The match pattern
     * @param variables The variables
     */
    protected UriMatchTemplate(CharSequence templateString, List<PathSegment> segments, Pattern matchPattern, List<UriMatchVariable> variables) {
        super(templateString.toString(), segments);
        this.variables = variables;
        if (variables.isEmpty() && matchPattern.matcher(templateString).matches()) {
            // if there are no variables and match pattern matches template we can assume it matches exactly
            this.matchPattern = null;
            exactMatch = true;
        } else {
            this.matchPattern = matchPattern;
            exactMatch = false;
        }
    }

    /**
     * @param uriTemplate The template
     * @param newSegments The list of new segments
     * @param newPattern The list of new patters
     * @param variables The variables
     *
     * @return An instance of {@link UriMatchTemplate}
     */
    protected UriMatchTemplate newUriMatchTemplate(CharSequence uriTemplate, List<PathSegment> newSegments, Pattern newPattern, List<UriMatchVariable> variables) {
        return new UriMatchTemplate(uriTemplate, newSegments, newPattern, variables);
    }

    /**
     * @return The variables this template expects
     */
    public List<UriMatchVariable> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    /**
     * Returns the path string excluding any query variables.
     *
     * @return The path string
     */
    public String toPathString() {
        return toString(pathSegment -> {
            final Optional<String> var = pathSegment.getVariable();
            if (var.isPresent()) {
                final Optional<UriMatchVariable> umv = variables.stream()
                    .filter(v -> v.getName().equals(var.get())).findFirst();
                if (umv.isPresent()) {
                    return !umv.get().isQuery();
                }
            }
            return true;
        });
    }

    @Override
    public UriMatchTemplate nest(CharSequence uriTemplate) {
        return (UriMatchTemplate) super.nest(uriTemplate);
    }

    /**
     * Create a new {@link UriTemplate} for the given URI.
     *
     * @param uri The URI
     *
     * @return The template
     */
    public static UriMatchTemplate of(String uri) {
        return new UriMatchTemplate(uri);
    }

    @Override
    protected UriTemplate newUriTemplate(CharSequence uriTemplate, List<PathSegment> newSegments) {
        Pattern newPattern = Pattern.compile(exactMatch ? Pattern.quote(templateString) + pattern : matchPattern.pattern() + pattern);
        pattern = null;
        return newUriMatchTemplate(normalizeNested(toString(), uriTemplate), newSegments, newPattern, new ArrayList<>(variables));
    }

    @Override
    protected UriTemplateParser createParser(String templateString, Object... parserArguments) {

        if (Objects.isNull(pattern)) {
            pattern = new StringBuilder();
        }

        if (variables == null) {
            variables = new ArrayList<>();
        }
        return new UriMatchTemplateParser(templateString, this);
    }

    /**
     * <p>Extended version of {@link UriTemplateParser} that builds a regular expression to match a path.
     * Note that fragments (#) and queries (?) are ignored for the purposes of matching.</p>
     */
    protected static class UriMatchTemplateParser extends UriTemplateParser {

        final UriMatchTemplate matchTemplate;

        /**
         * @param templateText The template
         * @param matchTemplate The Uri match template
         */
        protected UriMatchTemplateParser(String templateText, UriMatchTemplate matchTemplate) {
            super(templateText);
            this.matchTemplate = matchTemplate;
        }

        /**
         * @return The URI match template
         */
        public UriMatchTemplate getMatchTemplate() {
            return matchTemplate;
        }

        @Override
        protected void addRawContentSegment(List<PathSegment> segments, String value, boolean isQuerySegment) {
            matchTemplate.pattern.append(Pattern.quote(value));
            super.addRawContentSegment(segments, value, isQuerySegment);
        }

        @Override
        protected void addVariableSegment(List<PathSegment> segments,
                                          String variable,
                                          String prefix,
                                          String delimiter,
                                          boolean encode,
                                          boolean repeatPrefix,
                                          String modifierStr,
                                          char modifierChar,
                                          char operator,
                                          String previousDelimiter, boolean isQuerySegment) {
            matchTemplate.variables.add(new UriMatchVariable(variable, modifierChar, operator));
            StringBuilder pattern = matchTemplate.pattern;
            int modLen = modifierStr.length();
            boolean hasModifier = modifierChar == ':' && modLen > 0;
            String operatorPrefix = "";
            String operatorQuantifier = "";
            String variableQuantifier = "+?)";
            String variablePattern = getVariablePattern(variable, operator);
            if (hasModifier) {
                char firstChar = modifierStr.charAt(0);
                if (firstChar == '?') {
                    operatorQuantifier = "";
                } else if (modifierStr.chars().allMatch(Character::isDigit)) {
                    variableQuantifier = "{1," + modifierStr + "})";
                } else {

                    char lastChar = modifierStr.charAt(modLen - 1);
                    if (lastChar == '*' ||
                        (modLen > 1 && lastChar == '?' && (modifierStr.charAt(modLen - 2) == '*' || modifierStr.charAt(modLen - 2) == '+'))) {
                        operatorQuantifier = "?";
                    }
                    if (operator == '/' || operator == '.') {
                        variablePattern = "(" + ((firstChar == '^') ? modifierStr.substring(1) : modifierStr) + ")";
                    } else {
                        operatorPrefix = "(";
                        variablePattern = ((firstChar == '^') ? modifierStr.substring(1) : modifierStr) + ")";
                    }
                    variableQuantifier = "";
                }
            }

            boolean operatorAppended = false;

            switch (operator) {
                case '.':
                case '/':
                    pattern.append("(")
                        .append(operatorPrefix)
                        .append("\\")
                        .append(String.valueOf(operator))
                        .append(operatorQuantifier);
                    operatorAppended = true;
                    // fall through
                case '+':
                case '0': // no active operator
                    if (!operatorAppended) {
                        pattern.append("(").append(operatorPrefix);
                    }
                    pattern.append(variablePattern)
                        .append(variableQuantifier)
                        .append(")");
                    break;
                default:
                    // no-op
            }

            if (operator == '/' || modifierStr.equals("?")) {
                pattern.append("?");
            }
            super.addVariableSegment(segments, variable, prefix, delimiter, encode, repeatPrefix, modifierStr, modifierChar, operator, previousDelimiter, isQuerySegment);
        }

        /**
         * @param variable The variable
         * @param operator The operator
         *
         * @return The variable match pattern
         */
        protected String getVariablePattern(String variable, char operator) {
            if (operator == '+') {
                // Allow reserved characters. See https://tools.ietf.org/html/rfc6570#section-3.2.3
                return "([\\S]";
            } else {
                return VARIABLE_MATCH_PATTERN;
            }
        }
    }
}
