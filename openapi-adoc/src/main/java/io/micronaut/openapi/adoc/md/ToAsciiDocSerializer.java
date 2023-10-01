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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.pegdown.LinkRenderer;
import org.pegdown.Printer;
import org.pegdown.ast.AbbreviationNode;
import org.pegdown.ast.AbstractNode;
import org.pegdown.ast.AnchorLinkNode;
import org.pegdown.ast.AutoLinkNode;
import org.pegdown.ast.BlockQuoteNode;
import org.pegdown.ast.BulletListNode;
import org.pegdown.ast.CodeNode;
import org.pegdown.ast.DefinitionListNode;
import org.pegdown.ast.DefinitionNode;
import org.pegdown.ast.DefinitionTermNode;
import org.pegdown.ast.ExpImageNode;
import org.pegdown.ast.ExpLinkNode;
import org.pegdown.ast.HeaderNode;
import org.pegdown.ast.HtmlBlockNode;
import org.pegdown.ast.InlineHtmlNode;
import org.pegdown.ast.ListItemNode;
import org.pegdown.ast.MailLinkNode;
import org.pegdown.ast.Node;
import org.pegdown.ast.OrderedListNode;
import org.pegdown.ast.ParaNode;
import org.pegdown.ast.QuotedNode;
import org.pegdown.ast.RefImageNode;
import org.pegdown.ast.RefLinkNode;
import org.pegdown.ast.ReferenceNode;
import org.pegdown.ast.RootNode;
import org.pegdown.ast.SimpleNode;
import org.pegdown.ast.SpecialTextNode;
import org.pegdown.ast.StrikeNode;
import org.pegdown.ast.StrongEmphSuperNode;
import org.pegdown.ast.SuperNode;
import org.pegdown.ast.TableBodyNode;
import org.pegdown.ast.TableCaptionNode;
import org.pegdown.ast.TableCellNode;
import org.pegdown.ast.TableColumnNode;
import org.pegdown.ast.TableHeaderNode;
import org.pegdown.ast.TableNode;
import org.pegdown.ast.TableRowNode;
import org.pegdown.ast.TextNode;
import org.pegdown.ast.VerbatimNode;
import org.pegdown.ast.Visitor;
import org.pegdown.ast.WikiLinkNode;

import static org.parboiled.common.Preconditions.checkArgNotNull;

public class ToAsciiDocSerializer implements Visitor {

    public static final String HARD_LINE_BREAK_MARKDOWN = "  \n";

    protected String source;
    protected Printer printer;
    protected final Map<String, ReferenceNode> references = new HashMap<>();
    protected final Map<String, String> abbreviations = new HashMap<>();
    protected final LinkRenderer linkRenderer = new LinkRenderer();

    protected TableNode currentTableNode;
    protected int currentTableColumn;
    protected boolean inTableHeader;

    protected char listMarker;
    protected int listLevel;
    protected int blockQuoteLevel;

    protected boolean autoDetectLanguageType;

    protected RootNode rootNode;

    public ToAsciiDocSerializer(RootNode rootNode, String source) {
        printer = new Printer();
        autoDetectLanguageType = true;
        checkArgNotNull(rootNode, "rootNode");
        this.rootNode = rootNode;
        this.source = source;
    }

    public String toAsciiDoc() {
        cleanAst(rootNode);
        rootNode.accept(this);
        String result = normalizeWhitelines(printer.getString());
        printer.clear();
        return result;
    }

    @Override
    public void visit(RootNode node) {
        for (ReferenceNode refNode : node.getReferences()) {
            visitChildren(refNode);
            references.put(normalize(printer.getString()), refNode);
            printer.clear();
        }
        for (AbbreviationNode abbrNode : node.getAbbreviations()) {
            visitChildren(abbrNode);
            String abbr = printer.getString();
            printer.clear();
            abbrNode.getExpansion().accept(this);
            String expansion = printer.getString();
            abbreviations.put(abbr, expansion);
            printer.clear();
        }
        visitChildren(node);
    }

    @Override
    public void visit(AbbreviationNode node) {
    }

    @Override
    public void visit(AnchorLinkNode node) {
        printer.print(node.getText());
    }

    @Override
    public void visit(AutoLinkNode node) {
        printLink(linkRenderer.render(node));
    }

    @Override
    public void visit(BlockQuoteNode node) {
        printer.println().println();

        blockQuoteLevel += 4;

        repeat('_', blockQuoteLevel);
        printer.println();
        visitChildren(node);
        printer.println().println();
        repeat('_', blockQuoteLevel);

        blockQuoteLevel -= 4;

        printer.println();
    }

    @Override
    public void visit(BulletListNode node) {
        char prevListMarker = listMarker;
        listMarker = '*';

        listLevel = listLevel + 1;
        visitChildren(node);
        listLevel = listLevel - 1;

        listMarker = prevListMarker;
    }

    @Override
    public void visit(CodeNode node) {
        printer.print('`');
        printer.printEncoded(node.getText());
        printer.print('`');
    }

    @Override
    public void visit(DefinitionListNode node) {
        printer.println();
        visitChildren(node);
    }

    @Override
    public void visit(DefinitionTermNode node) {
        visitChildren(node);
        printer.indent(2);
        printer.print("::").println();
    }

    @Override
    public void visit(DefinitionNode node) {
        visitChildren(node);
        if (printer.indent > 0) {
            printer.indent(-2);
        }
        printer.println();
    }

    @Override
    public void visit(ExpImageNode node) {
        String text = printChildrenToString(node);
        LinkRenderer.Rendering imageRenderer = linkRenderer.render(node, text);
        Node linkNode;
        if ((linkNode = findParentNode(node, rootNode)) instanceof ExpLinkNode) {
            printImageTagWithLink(imageRenderer, linkRenderer.render((ExpLinkNode) linkNode, null));
        } else {
            printImageTag(linkRenderer.render(node, text));
        }
    }

    @Override
    public void visit(ExpLinkNode node) {
        String text = printChildrenToString(node);
        if (text.startsWith("image:")) {
            printer.print(text);
        } else {
            printLink(linkRenderer.render(node, text));
        }
    }

    @Override
    public void visit(HeaderNode node) {
        printer.println().println();
        repeat('=', node.getLevel());
        printer.print(' ');
        visitChildren(node);
        printer.println().println();
    }

    private void repeat(char c, int times) {
        for (int i = 0; i < times; i++) {
            printer.print(c);
        }
    }

    @Override
    public void visit(HtmlBlockNode node) {
        String text = node.getText();
        if (!text.isEmpty()) {
            printer.println();
        }

        if (text.startsWith("<table")) {
            printer.print(TableToAsciiDoc.convert(text))
                .println();
        }
    }

    @Override
    public void visit(InlineHtmlNode node) {
        printer.print(node.getText());
    }

    @Override
    public void visit(ListItemNode node) {
        printer.println();
        repeat(listMarker, listLevel);
        printer.print(' ');

        visitChildren(node);
    }

    @Override
    public void visit(MailLinkNode node) {
        printLink(linkRenderer.render(node));
    }

    @Override
    public void visit(OrderedListNode node) {
        char prevListMarker = listMarker;
        listMarker = '.';

        listLevel = listLevel + 1;
        visitChildren(node);
        listLevel = listLevel - 1;

        listMarker = prevListMarker;
    }

    @Override
    public void visit(ParaNode node) {
        if (!isListItemText(node)) {
            printer.println().println();
        }
        visitChildren(node);
        printer.println().println();
    }

    @Override
    public void visit(QuotedNode node) {
        switch (node.getType()) {
            case DoubleAngle -> {
                printer.print("«");
                visitChildren(node);
                printer.print("»");
            }
            case Double -> {
                printer.print('"');
                visitChildren(node);
                printer.print('"');
            }
            case Single -> {
                printer.print('\'');
                visitChildren(node);
                printer.print('\'');
            }
        }
    }

    @Override
    public void visit(ReferenceNode node) {
        // reference nodes are not printed
    }

    @Override
    public void visit(RefImageNode node) {
        String text = printChildrenToString(node);
        String key = node.referenceKey != null ? printChildrenToString(node.referenceKey) : text;
        ReferenceNode refNode = references.get(normalize(key));
        if (refNode == null) { // "fake" reference image link
            printer.print("![").print(text).print(']');
            if (node.separatorSpace != null) {
                printer.print(node.separatorSpace).print('[');
                if (node.referenceKey != null) {
                    printer.print(key);
                }
                printer.print(']');
            }
        } else {
            printImageTag(linkRenderer.render(node, refNode.getUrl(), refNode.getTitle(), text));
        }
    }

    @Override
    public void visit(RefLinkNode node) {
        String text = printChildrenToString(node);
        String key = node.referenceKey != null ? printChildrenToString(node.referenceKey) : text;
        ReferenceNode refNode = references.get(normalize(key));
        if (refNode == null) { // "fake" reference link
            printer.print('[').print(text).print(']');
            if (node.separatorSpace != null) {
                printer.print(node.separatorSpace).print('[');
                if (node.referenceKey != null) {
                    printer.print(key);
                }
                printer.print(']');
            }
        } else {
            printLink(linkRenderer.render(node, refNode.getUrl(), refNode.getTitle(), text));
        }
    }

    @Override
    public void visit(SimpleNode node) {
        switch (node.getType()) {
            case Apostrophe -> printer.print('\'');
            case Ellipsis -> printer.print("…");
            case Emdash -> printer.print("—");
            case Endash -> printer.print("–");
            case HRule -> printer.println().print("'''");
            case Linebreak -> {
                // look for length of span to detect hard line break (2 trailing spaces plus end-line)
                // necessary because Pegdown doesn't distinguish between a hard line break and a normal line break
                if (source != null && source.substring(node.getStartIndex(), node.getEndIndex()).startsWith(HARD_LINE_BREAK_MARKDOWN)) {
                    printer.print(" +").println();
                } else {
                    // QUESTION should we fold or preserve soft line breaks? (pandoc emits a space here)
                    printer.println();
                }
            }
            case Nbsp -> printer.print("{nbsp}");
            default -> throw new IllegalStateException();
        }
    }

    @Override
    public void visit(StrongEmphSuperNode node) {
        if (node.isClosed()) {
            if (node.isStrong()) {
                printNodeSurroundedBy(node, "*");
            } else {
                printNodeSurroundedBy(node, "_");
            }
        } else {
            //sequence was not closed, treat open chars as ordinary chars
            printer.print(node.getChars());
            visitChildren(node);
        }
    }

    @Override
    public void visit(StrikeNode node) {
        printer.print("[line-through]").print('#');
        visitChildren(node);
        printer.print('#');
    }

    @Override
    public void visit(TableBodyNode node) {
        visitChildren(node);
    }

    @Override
    public void visit(TableCaptionNode node) {
        printer.println().print("<caption>");
        visitChildren(node);
        printer.print("</caption>");
    }

    @Override
    public void visit(TableCellNode node) {
//        String tag = inTableHeader ? "th" : "td";
        List<TableColumnNode> columns = currentTableNode.getColumns();
        TableColumnNode column = columns.get(Math.min(currentTableColumn, columns.size() - 1));

        String pstr = printer.getString();
        if (!pstr.isEmpty()) {
            if (pstr.endsWith("\n") || pstr.endsWith(" ")) {
                printer.print('|');
            } else {
                printer.print(" |");
            }
        } else {
            printer.print('|');
        }
        column.accept(this);
        if (node.getColSpan() > 1) {
            printer.print(" colspan=\"").print(Integer.toString(node.getColSpan())).print('"');
        }
        visitChildren(node);

        currentTableColumn += node.getColSpan();
    }

    @Override
    public void visit(TableColumnNode node) {
        // nothing here yet
    }

    @Override
    public void visit(TableHeaderNode node) {
        inTableHeader = true;
//        printIndentedTag(node, "thead");

        visitChildren(node);

        inTableHeader = false;
    }

    private boolean ifColumnsHaveAlignmentSpecified(List<TableColumnNode> columns) {
        for (TableColumnNode column : columns) {
            if (column.getAlignment() != TableColumnNode.Alignment.None) {
                return true;
            }
        }
        return false;
    }

    private String getColumnAlignment(List<TableColumnNode> columns) {

        List<String> result = new ArrayList<>();

        for (TableColumnNode column : columns) {
            switch (column.getAlignment()) {
                case None, Left -> result.add("<");
                case Right -> result.add(">");
                case Center -> result.add("^");
                default -> throw new IllegalStateException();
            }
        }

        return String.join(",", result);
    }

    @Override
    public void visit(TableNode node) {
        currentTableNode = node;

        List<TableColumnNode> columns = node.getColumns();

        if (ifColumnsHaveAlignmentSpecified(columns)) {
            printer.println()
                .print("[cols=\"").print(getColumnAlignment(columns)).print("\"]")
                .println();
        } else {
            printer.println();
        }

        printer.print("|===");
        visitChildren(node);
        printer.println()
            .print("|===")
            .println();

        currentTableNode = null;
    }

    @Override
    public void visit(TableRowNode node) {
        currentTableColumn = 0;

        printer.println();

        visitChildren(node);
//        printIndentedTag(node, "tr");

        if (inTableHeader) {
            printer.println();
        }
    }

    @Override
    public void visit(VerbatimNode node) {
        printer.println();

        String type = node.getType();
        String text = node.getText();

        if ((type == null || type.isBlank()) && autoDetectLanguageType) {
            type = detectLanguage(text);
        }

        if (type != null && !type.isEmpty()) {
            printer.print("[source," + type + "]");
        }

        printer.println();
        repeat('-', 4);
        printer.println().print(text);
        repeat('-', 4);
        printer.println().println();
    }

    private String detectLanguage(String text) {
        if (text.startsWith("<")) {
            return "html";
        } else if (text.endsWith(";")) {
            return "java";
        } else if (text.contains("fun ")) {
            return "kotlin";
        } else {
            return "groovy";
        }
    }

    @Override
    public void visit(WikiLinkNode node) {
        printLink(linkRenderer.render(node));
    }

    @Override
    public void visit(TextNode node) {
        if (abbreviations.isEmpty()) {
            printer.print(node.getText());
        } else {
            printWithAbbreviations(node.getText());
        }
    }

    @Override
    public void visit(SpecialTextNode node) {
        printer.printEncoded(node.getText());
    }

    @Override
    public void visit(SuperNode node) {
        visitChildren(node);
    }

    @Override
    public void visit(Node node) {
        throw new RuntimeException("Don't know how to handle node " + node);
    }

    // helpers

    protected void visitChildren(AbstractNode node) {
        for (Node child : node.getChildren()) {
            child.accept(this);
        }
    }


    /**
     * Removes superfluous nodes from the tree.
     *
     * @param node The node to clean.
     */
    protected void cleanAst(Node node) {
        List<Node> children = node.getChildren();
        for (int i = 0, len = children.size(); i < len; i++) {
            Node c = children.get(i);
            if (c instanceof RootNode) {
                children.set(i, c.getChildren().get(0));
            } else if (c.getClass().equals(SuperNode.class) && c.getChildren().size() == 1) {
                children.set(i, c.getChildren().get(0));
            }

            cleanAst(c);
        }
    }

    protected void printNodeSurroundedBy(AbstractNode node, String token) {
        printer.print(token);
        visitChildren(node);
        printer.print(token);
    }

    protected void printImageTag(LinkRenderer.Rendering rendering) {
        printer.print("image:").print(rendering.href).print('[');
        printTextWithQuotesIfNeeded(printer, rendering.text);
        printer.print(']');
    }

    protected void printImageTagWithLink(LinkRenderer.Rendering image, LinkRenderer.Rendering link) {
        printer.print("image:").print(image.href).print('[');
        if (image.text != null && !image.text.isEmpty()) {
            printTextWithQuotesIfNeeded(printer, image.text);
            printer.print(',');
        }

        printer.print("link=").print(link.href).print(']');
    }

    protected void printLink(LinkRenderer.Rendering rendering) {
        String uri = rendering.href;
        String text = rendering.text;

        if (uri.startsWith("#")) {
            printer.print("<<").print(uri.substring(1)).print(',').print(text).print(">>");
        } else {
            if (!uri.contains("://")) {
                uri = "link:" + uri;
            }
            printer.print(uri);
            if (!uri.equals(text)) {
                printer.print('[');
                printTextWithQuotesIfNeeded(printer, rendering.text);
                printer.print(']');
            }
        }
    }

    protected String printChildrenToString(SuperNode node) {
        Printer priorPrinter = printer;
        printer = new Printer();
        visitChildren(node);
        String result = printer.getString();
        printer = priorPrinter;
        return result;
    }

    protected String normalize(String string) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t') {
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    protected String normalizeWhitelines(String text) {
        // replace all double or more empty lines with single empty lines
        return text.replaceAll("(?m)^[ \t]*\r?\n{2,}", "\n").trim();
    }

    protected void printTextWithQuotesIfNeeded(Printer p, String text) {
        if (text != null && !text.isEmpty()) {
            if (text.contains(",")) {
                p.print('"').print(text).print('"');
            } else {
                p.print(text);
            }
        }
    }

    protected void printWithAbbreviations(String string) {
        Map<Integer, Map.Entry<String, String>> expansions = null;

        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            // first check, whether we have a legal match
            String abbr = entry.getKey();

            int ix = 0;
            while (true) {
                int sx = string.indexOf(abbr, ix);
                if (sx == -1) {
                    break;
                }

                // only allow whole word matches
                ix = sx + abbr.length();

                if (sx > 0 && Character.isLetterOrDigit(string.charAt(sx - 1))) {
                    continue;
                }
                if (ix < string.length() && Character.isLetterOrDigit(string.charAt(ix))) {
                    continue;
                }

                // ok, legal match so save an expansions "task" for all matches
                if (expansions == null) {
                    expansions = new TreeMap<>();
                }
                expansions.put(sx, entry);
            }
        }

        if (expansions != null) {
            int ix = 0;
            for (Map.Entry<Integer, Map.Entry<String, String>> entry : expansions.entrySet()) {
                int sx = entry.getKey();
                String abbr = entry.getValue().getKey();
                String expansion = entry.getValue().getValue();

                printer.printEncoded(string.substring(ix, sx))
                    .print("<abbr");
                if (expansion != null && !expansion.isEmpty()) {
                    printer.print(" title=\"").printEncoded(expansion).print('"');
                }
                printer.print('>')
                    .printEncoded(abbr)
                    .print("</abbr>");
                ix = sx + abbr.length();
            }
            printer.print(string.substring(ix));
        } else {
            printer.print(string);
        }
    }

    protected Node findParentNode(Node target, Node from) {
        if (target.equals(rootNode)) {
            return null;
        }

        Node candidate;

        for (Node c : from.getChildren()) {
            if (target.equals(c)) {
                return from;
            } else if ((candidate = findParentNode(target, c)) != null) {
                return candidate;
            }
        }

        return null;
    }

    protected boolean isFirstChild(Node parent, Node child) {
        return child.equals(parent.getChildren().get(0));
    }

    protected boolean isListItemText(Node node) {
        if (listLevel == 0) {
            return false;
        }
        Node parent = findParentNode(node, rootNode);
        return parent instanceof ListItemNode && isFirstChild(parent, node);
    }
}
