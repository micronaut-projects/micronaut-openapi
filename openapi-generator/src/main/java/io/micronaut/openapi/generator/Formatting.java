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
package io.micronaut.openapi.generator;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A class with lambdas to format mustache-generated code and formatting utility functions.
 */
public enum Formatting {
    /* This class is not supposed to be initialized */;

    /**
     * Remove whitespace on the right of the line.
     * @param line The line to be trimmed.
     * @return The trimmed line.
     */
    public static String rightTrim(String line) {
        int end = 0;
        for (int i = line.length(); i > 0; --i) {
            if (!Character.isWhitespace(line.charAt(i - 1))) {
                end = i;
                break;
            }
        }
        return line.substring(0, end);
    }

    /**
     * Remove whitespace from both sides of the line.
     * @param line The line to be trimmed.
     * @return The trimmed line.
     */
    public static String trim(String line) {
        int start = line.length();
        for (int i = 0; i < line.length(); ++i) {
            if (!Character.isWhitespace(line.charAt(i))) {
                start = i;
                break;
            }
        }
        int end = start;
        for (int i = line.length(); i > start; --i) {
            if (!Character.isWhitespace(line.charAt(i - 1))) {
                end = i;
                break;
            }
        }
        return line.substring(start, end);
    }

    /**
     * A formatter that is responsible for removing extra empty lines in mustache files.
     */
    public static class LineFormatter implements Mustache.Lambda {

        private final int maxEmptyLines;

        /**
         * Create the lambda.
         *
         * @param maxEmptyLines maximal empty lines.
         */
        public LineFormatter(int maxEmptyLines) {
            this.maxEmptyLines = maxEmptyLines;
        }

        @Override
        public void execute(Template.Fragment fragment, Writer writer) throws IOException {
            String text = fragment.execute();
            String finalWhitespace = getFinalWhitespace(text);

            String lines =
                Arrays.stream(text.split("\n"))
                    .map(Formatting::rightTrim)
                    .filter(new LineSkippingPredicate())
                    .collect(Collectors.joining("\n"));
            if (!lines.isEmpty() && maxEmptyLines == 0) {
                lines = "\n" + lines + "\n";
            }
            if (!lines.isEmpty()) {
                lines = lines + finalWhitespace;
            }
            writer.write(lines);
        }

        private String getFinalWhitespace(String text) {
            int i = text.length();
            while (i > 0 && Character.isWhitespace(text.charAt(i - 1)) && text.charAt(i - 1) != '\n') {
                --i;
            }
            return text.substring(i);
        }

        private class LineSkippingPredicate implements Predicate<String> {
            private int emptyLines;

            @Override
            public boolean test(String s) {
                if (s.isBlank()) {
                    ++emptyLines;
                } else {
                    emptyLines = 0;
                }
                return emptyLines <= maxEmptyLines;
            }
        }
    }

    /**
     * A formatter that collects everything in a single line.
     */
    public static class SingleLineFormatter implements Mustache.Lambda {

        @Override
        public void execute(Template.Fragment fragment, Writer writer) throws IOException {
            String text =
                fragment.execute()
                    .replaceAll("\\s+", " ")
                    .replaceAll("(?<=<)\\s+|\\s+(?=>)", "");
            writer.write(Formatting.trim(text));
        }

    }

    /**
     * A lambda that allows indenting its contents.
     */
    public static class IndentFormatter implements Mustache.Lambda {

        private final String indent;

        public IndentFormatter(int indentSize) {
            indent = " ".repeat(Math.max(0, indentSize));
        }

        @Override
        public void execute(Template.Fragment fragment, Writer writer) throws IOException {
            String text =
                Arrays.stream(fragment.execute().split("\n"))
                    .map(line -> indent + line)
                    .collect(Collectors.joining("\n"));
            writer.write(text);
        }
    }
}
