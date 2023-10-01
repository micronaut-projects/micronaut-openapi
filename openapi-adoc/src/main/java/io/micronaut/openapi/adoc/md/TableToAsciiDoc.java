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
package io.micronaut.openapi.adoc.md;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Utilities methods to convert html table to adoc table.
 *
 * @since 5.2.0
 */
public final class TableToAsciiDoc {

    private TableToAsciiDoc() {
    }

    /**
     * Convert HTML table to adoc table.
     *
     * @param html HTML text
     *
     * @return adoc text
     */
    public static String convert(String html) {
        if (!html.startsWith("<table")) {
            throw new IllegalArgumentException("No table found in HTML: " + html);
        }

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").get(0); //select the first table.

        var result = new StringBuilder("|===\n");
        Elements rows = table.select("tr");

        for (Element row : rows) {
            // table headers
            result.append(buildAsciiDocRow(row, "th"));
            if (!row.select("th").isEmpty()) {
                result.append('\n');
            }

            // table data
            result.append(buildAsciiDocRow(row, "td")).append('\n');
        }

        result.append("|===\n");

        return result.toString();
    }

    private static String buildAsciiDocRow(Element row, String query) {
        Elements columns = row.select(query);
        var dataRow = new StringBuilder();
        for (Element col : columns) {
            dataRow.append('|').append(applyBasicFormatting(col)).append(' ');
        }
        return dataRow.toString().trim();
    }

    private static String applyBasicFormatting(Element element) {

        String result = element.ownText();

        for (Element child : element.children()) {
            if ("code".equals(child.tagName())) {
                result = "`" + child.ownText() + "`";
            } else if ("b".equals(child.tagName()) || "strong".equals(child.tagName())) {
                result = "*" + child.ownText() + "*";
            } else if ("i".equals(child.tagName()) || "em".equals(child.tagName())) {
                result = "_" + child.ownText() + "_";
            } else if ("a".equals(child.tagName())) {
                result = child.attr("href") + "[" + child.ownText() + "]";
            }
        }

        return result;
    }
}
