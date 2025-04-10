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

import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.DOLLAR;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;
import static io.micronaut.openapi.visitor.StringUtil.SLASH_CHAR;
import static io.micronaut.openapi.visitor.StringUtil.capitalizedPathVar;
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

    private static final String PREFIX_FIRST = "With";
    private static final String PREFIX_NEXT = "And";

    private UrlUtils() {
    }

    /**
     * Construct all possible URL variants by parsed segments.
     *
     * @param segments url template segments
     * @param context visitor context
     * @return all possible URL variants by parsed segments.
     */
    public static List<OpPath> buildUrls(List<Segment> segments, VisitorContext context) {

        var results = new ArrayList<PathBuilders>();

        Segment prevSegment = null;
        for (var segment : segments) {
            appendSegment(segment, prevSegment, results);
            prevSegment = segment;
        }

        String contextPath = ConfigUtils.getServerContextPath(context);
        if (StringUtils.isNotEmpty(contextPath)) {
            if (!contextPath.startsWith(SLASH) && !contextPath.startsWith(DOLLAR)) {
                contextPath = SLASH + contextPath;
            }
            if (contextPath.endsWith(SLASH)) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
        }

        var resultOpPaths = new ArrayList<OpPath>();
        for (var res : results) {
            var url = res.urlBuilder.toString();
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

            var alreadyAdded = false;
            for (var opPath : resultOpPaths) {
                if (opPath.url.equals(url)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (alreadyAdded) {
                continue;
            }

            resultOpPaths.add(new OpPath(url, res.opIdBuilder.toString()));
        }

        return resultOpPaths;
    }

    private static void appendSegment(Segment segment, Segment prevSegment, List<PathBuilders> results) {
        var type = segment.type;
        var value = segment.value;
        if (results.isEmpty()) {
            if (type == PLACEHOLDER) {
                results.add(new PathBuilders(new StringBuilder(value), new StringBuilder(), true));
                return;
            }

            var isFirst = true;
            StringBuilder opIdBuilder;
            if (type == OPT_VAR) {
                opIdBuilder = new StringBuilder(PREFIX_FIRST).append(capitalizedPathVar(value));
                isFirst = false;
            } else {
                opIdBuilder = new StringBuilder();
            }
            results.add(new PathBuilders(new StringBuilder(value), opIdBuilder, isFirst));
            // case without optional path var
            if (type == OPT_VAR) {
                results.add(new PathBuilders(new StringBuilder(), new StringBuilder(), true));
            }
            return;
        }
        if (type == CONST || type == REQ_VAR || type == PLACEHOLDER) {
            for (var result : results) {
                result.urlBuilder.append(value);
            }
            return;
        }

        var newResults = new ArrayList<PathBuilders>();
        for (var result : results) {
            newResults.add(new PathBuilders(new StringBuilder(result.urlBuilder), new StringBuilder(result.opIdBuilder), result.isFirst));
        }
        for (var result : results) {
            if (prevSegment.type == OPT_VAR && result.urlBuilder.indexOf(prevSegment.value) < 0) {
                continue;
            }
            result.urlBuilder.append(SLASH_CHAR).append(value);
            result.opIdBuilder.append(result.isFirst ? PREFIX_FIRST : PREFIX_NEXT).append(capitalizedPathVar(value));
            result.isFirst = false;
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

    public static String parsePath(String path) {
        if (StringUtils.isNotEmpty(path) && !path.endsWith(StringUtil.SLASH)) {
            return path + StringUtil.SLASH;
        }
        return path;
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
     * Final path and operation ID builders.
     */
    static class PathBuilders {
        StringBuilder urlBuilder;
        StringBuilder opIdBuilder;
        boolean isFirst;

        public PathBuilders(StringBuilder urlBuilder, StringBuilder opIdBuilder, boolean isFirst) {
            this.urlBuilder = urlBuilder;
            this.opIdBuilder = opIdBuilder;
            this.isFirst = isFirst;
        }
    }

    /**
     * Final operation path URL and operation ID.
     *
     * @param url url
     * @param opIdPostfix operation ID postfix
     */
    public record OpPath(
        String url,
        String opIdPostfix
    ) {
    }

    /**
     * Type of segment.
     */
    enum SegmentType {
        REQ_VAR,
        OPT_VAR,
        CONST,
        PLACEHOLDER,
    }
}
