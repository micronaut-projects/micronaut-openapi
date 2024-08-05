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

import java.util.ArrayList;
import java.util.List;

import static io.micronaut.openapi.visitor.StringUtil.CLOSE_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.OPEN_BRACE;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;

/**
 * URL and URL paths util methods.
 *
 * @since 6.12.0
 */
@Internal
public final class UrlUtils {

    private UrlUtils() {
    }

    public static List<String> buildUrls(List<Segment> segments) {

        var results = new ArrayList<StringBuilder>();

        Segment prevSegment = null;
        for (var segment : segments) {
            appendSegment(segment, prevSegment, results);
            prevSegment = segment;
        }

        var resultStrings = new ArrayList<String>();
        for (var res : results) {
            var url = res.toString();
            if (!resultStrings.contains(url)) {
                if (url.endsWith(SLASH)) {
                    url = url.substring(0, url.length() - SLASH.length());
                }
                resultStrings.add(url);
            }
        }

        return resultStrings;
    }

    private static void appendSegment(Segment segment, Segment prevSegment, List<StringBuilder> results) {
        var type = segment.type;
        var value = segment.value;
        if (results.isEmpty()) {
            if (type == SegmentType.PLACEHOLDER) {
                results.add(new StringBuilder(value));
                return;
            }
            var builder = new StringBuilder(SLASH).append(value);
            if (!value.endsWith(SLASH)) {
                builder.append(SLASH);
            }
            results.add(builder);
            if (type == SegmentType.OPT_VAR) {
                results.add(new StringBuilder(SLASH));
            }
            return;
        }
        if (type == SegmentType.CONST || type == SegmentType.REQ_VAR || type == SegmentType.PLACEHOLDER) {
            for (var result : results) {
                result.append(value);
                if (type != SegmentType.PLACEHOLDER) {
                    result.append(SLASH);
                }
            }
            return;
        }

        var newResults = new ArrayList<StringBuilder>();
        for (var result : results) {
            newResults.add(new StringBuilder(result));
        }
        for (var result : results) {
            if (prevSegment.type == SegmentType.OPT_VAR && result.indexOf(prevSegment.value + SLASH) < 0) {
                continue;
            }
            result.append(value).append(SLASH);
        }
        results.addAll(newResults);
    }

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
                segments.add(new Segment(SegmentType.PLACEHOLDER, pathString.substring(varStartPos - 1, varEndPos + 1)));
                startPos = varEndPos + 1;
                continue;
            }

            SegmentType type = nextChar == '/' ? SegmentType.OPT_VAR : SegmentType.REQ_VAR;

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
            segments.add(new Segment(SegmentType.CONST, SLASH));
        }

        return segments;
    }

    private static void addConstValue(String constValue, List<Segment> segments) {
        if (constValue.startsWith(SLASH)) {
            constValue = constValue.substring(1);
        }
        if (constValue.endsWith(SLASH)) {
            constValue = constValue.substring(0, constValue.length() - SLASH.length());
        }
        if (!constValue.isEmpty()) {
            segments.add(new Segment(SegmentType.CONST, constValue));
        }
    }

    private record Segment(
        SegmentType type,
        String value
    ) {
    }

    private enum SegmentType {
        REQ_VAR,
        OPT_VAR,
        CONST,
        PLACEHOLDER,
    }
}
