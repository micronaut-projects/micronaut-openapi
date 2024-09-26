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
package io.micronaut.openapi.visitor;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.List;

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_SERVER_CONTEXT_PATH;
import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.DOLLAR;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;
import static io.micronaut.openapi.visitor.StringUtil.SLASH_CHAR;
import static io.micronaut.openapi.visitor.UrlUtils.SegmentType.CONST;
import static io.micronaut.openapi.visitor.UrlUtils.SegmentType.OPT_VAR;
import static io.micronaut.openapi.visitor.UrlUtils.SegmentType.PLACEHOLDER;
import static io.micronaut.openapi.visitor.UrlUtils.SegmentType.REQ_VAR;

/**
 * URL and URL paths util methods.
 *
 * @since 6.12.0
 */
@Internal
public final class UrlUtils {

    private UrlUtils() {
    }

    /**
     * Construct all possible URL variants by parsed segments.
     *
     * @param segments url template segments
     * @return all possible URL variants by parsed segments.
     */
    public static List<String> buildUrls(List<Segment> segments, VisitorContext context) {

        var results = new ArrayList<StringBuilder>();

        Segment prevSegment = null;
        for (var segment : segments) {
            appendSegment(segment, prevSegment, results);
            prevSegment = segment;
        }

        String contextPath = ConfigUtils.getConfigProperty(MICRONAUT_SERVER_CONTEXT_PATH, context);
        if (StringUtils.isNotEmpty(contextPath)) {
            if (!contextPath.startsWith(SLASH) && !contextPath.startsWith(DOLLAR)) {
                contextPath = SLASH + contextPath;
            }
            if (contextPath.endsWith(SLASH)) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
        }

        var resultStrings = new ArrayList<String>();
        for (var res : results) {
            var url = res.toString();
            if (url.endsWith(SLASH) && url.length() > 1) {
                url = url.substring(0, url.length() - SLASH.length());
            } else if (!url.startsWith(SLASH) && !url.startsWith(DOLLAR)) {
                url = SLASH + url;
            } else if (url.startsWith(SLASH + DOLLAR)) {
                url = url.substring(1);
            }

            if (StringUtils.isNotEmpty(contextPath)) {
                url = contextPath + url;
            }

            if (!resultStrings.contains(url)) {
                resultStrings.add(url);
            }
        }

        return resultStrings;
    }

    private static void appendSegment(Segment segment, Segment prevSegment, List<StringBuilder> results) {
        var type = segment.type;
        var value = segment.value;
        if (results.isEmpty()) {
            if (type == PLACEHOLDER) {
                results.add(new StringBuilder(value));
                return;
            }
            var builder = new StringBuilder();
            builder.append(value);
            results.add(builder);
            if (type == OPT_VAR) {
                results.add(new StringBuilder());
            }
            return;
        }
        if (type == CONST || type == REQ_VAR || type == PLACEHOLDER) {
            for (var result : results) {
                result.append(value);
            }
            return;
        }

        var newResults = new ArrayList<StringBuilder>();
        for (var result : results) {
            newResults.add(new StringBuilder(result));
        }
        for (var result : results) {
            if (prevSegment.type == OPT_VAR && result.indexOf(prevSegment.value) < 0) {
                continue;
            }
            result.append(SLASH_CHAR).append(value);
        }
        results.addAll(newResults);
    }

    /**
     * Parse path string to list of segments.
     *
     * @param pathString path string
     * @return list of segments
     */
    public static List<Segment> parsePathSegments(String pathString) {

        var segments = new ArrayList<Segment>();

        var startPos = 0;

        for (; ; ) {

            var varStartPos = pathString.indexOf('{', startPos);
            if (varStartPos < 0) {
                addConstValue(pathString.substring(startPos), segments);
                break;
            }

            var varEndPos = pathString.indexOf('}', varStartPos);

            var constSegment = pathString.substring(startPos, varStartPos);
            var nextChar = pathString.charAt(varStartPos + 1);

            // skip non path vars
            if (nextChar == '?' || nextChar == '.' || nextChar == '+' || nextChar == '0') {
                addConstValue(constSegment, segments);
                startPos = varEndPos + 1;
                continue;
            }

            // process placeholders
            if (varStartPos >= 1 && pathString.charAt(varStartPos - 1) == '$') {
                if (!constSegment.isEmpty()) {
                    addConstValue(constSegment.substring(0, constSegment.length() - 1), segments);
                }
                segments.add(new Segment(PLACEHOLDER, pathString.substring(varStartPos - 1, varEndPos + 1)));
                startPos = varEndPos + 1;
                continue;
            }

            SegmentType type = nextChar == '/' ? OPT_VAR : REQ_VAR;

            if (!constSegment.isEmpty()) {
                addConstValue(constSegment, segments);
            }

            var startBlockPos = varStartPos;
            if (pathString.charAt(startBlockPos + 1) == '/') {
                startBlockPos++;
            }
            for (; ; ) {
                var dotPos = pathString.indexOf(',', startBlockPos + 1);
                var dotPos2 = pathString.indexOf(':', startBlockPos + 1);
                var minEndPos = dotPos > 0 && dotPos < varEndPos ? dotPos : varEndPos;
                minEndPos = dotPos2 > 0 && dotPos2 < minEndPos ? dotPos2 : minEndPos;
                var varName = pathString.substring(startBlockPos + 1, minEndPos);
                segments.add(new Segment(type, OPEN_BRACE + varName + CLOSE_BRACE));
                if (minEndPos != dotPos) {
                    break;
                }
                startBlockPos = minEndPos;
            }
            startPos = varEndPos + 1;
        }

        if (segments.isEmpty()) {
            segments.add(new Segment(CONST, SLASH));
        }

        return segments;
    }

    private static void addConstValue(String constValue, List<Segment> segments) {
        if (!constValue.isEmpty()) {
            segments.add(new Segment(CONST, constValue));
        }
    }

    /**
     * Segment of urlTemplate.
     *
     * @param type segment type
     * @param value value
     */
    public record Segment(
        SegmentType type,
        String value
    ) {
    }

    /**
     * Type of segment.
     */
    public enum SegmentType {
        REQ_VAR,
        OPT_VAR,
        CONST,
        PLACEHOLDER,
    }
}
