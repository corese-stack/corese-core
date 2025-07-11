package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the ANTLR4 generated parser for N-Quads.
 * This listener traverses the parse tree and builds the RDF model,
 * supporting named graphs. It includes unescaping logic for URIs and literals.
 */
public class NQuadsListener extends NQuadsBaseListener {

    private static final Logger logger = LoggerFactory.getLogger(NQuadsListener.class);

    private final Model model;
    private final ValueFactory factory;
    private final IOOptions options;

    private Resource currentSubject;
    private IRI currentPredicate;
    private Resource currentGraph;

    public NQuadsListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;
        this.options = options;
    }

    @Override
    public void enterStatement(NQuadsParser.StatementContext ctx) {

        currentSubject = extractSubject(ctx.subject());
        currentPredicate = extractPredicate(ctx.predicate());
        if (ctx.graphLabel() != null) {
            currentGraph = extractGraph(ctx.graphLabel());
        } else {
            currentGraph = null;
        }
    }

    @Override
    public void exitStatement(NQuadsParser.StatementContext ctx) {

        Value object = extractObject(ctx.object());
        if (currentGraph != null) {
            model.add(currentSubject, currentPredicate, object, currentGraph);
        } else {
            model.add(currentSubject, currentPredicate, object);
        }
        currentSubject = null;
        currentPredicate = null;
        currentGraph = null;
    }

    /**
     * Extracts a resource (IRI or Blank Node) from the subject context.
     */
    protected Resource extractSubject(NQuadsParser.SubjectContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            return factory.createBNode(ctx.BLANK_NODE_LABEL().getText().substring(2));
        }
        throw new IllegalArgumentException("Unsupported N-Quads subject: " + ctx.getText());
    }

    /**
     * Extracts a predicate (IRI) from the predicate context.
     */
    protected IRI extractPredicate(NQuadsParser.PredicateContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        throw new IllegalArgumentException("Unsupported N-Quads predicate: " + ctx.getText());
    }

    /**
     * Extracts a value (IRI, Blank Node, or Literal) from the object context.
     */
    protected Value extractObject(NQuadsParser.ObjectContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            return factory.createBNode(ctx.BLANK_NODE_LABEL().getText().substring(2));
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new IllegalArgumentException("Unsupported N-Quads object: " + ctx.getText());
    }

    /**
     * Extracts a graph (IRI or Blank Node) from the graph context.
     */
    protected Resource extractGraph(NQuadsParser.GraphLabelContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            return factory.createBNode(ctx.BLANK_NODE_LABEL().getText().substring(2));
        }
        throw new IllegalArgumentException("Unsupported N-Quads graph: " + ctx.getText());
    }

    /**
     * Extracts and unescapes a literal from the ANTLR context.
     * This method handles string literals with or without datatype/language.
     */
    protected Literal extractLiteral(NQuadsParser.LiteralContext ctx) {
        String label = ctx.STRING_LITERAL_QUOTE().getText();
        label = unescapeLiteral(label);

        if (ctx.IRIREF() != null) {
            IRI datatype = factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
            return factory.createLiteral(label, datatype);
        }
        if (ctx.LANGTAG() != null) {
            String lang = ctx.LANGTAG().getText().substring(1);
            return factory.createLiteral(label, lang);
        }

        return factory.createLiteral(label);
    }

    /**
     * Unescapes common N-Quads literal escape sequences.
     * This method handles `\"`, `\\`, `\n`, `\t`, `\r`, `\b`, `\f`.
     * It also handles `\ uXXXX` and `\UXXXXXXXX` for Unicode escapes.
     * It also removes the surrounding quotes from the literal string.
     *
     * @param literalText The raw literal string from ANTLR (including quotes and escapes).
     * @return The unescaped literal string without surrounding quotes.
     */
    protected String unescapeLiteral(String literalText) {
        String unquotedLiteral = literalText.substring(1, literalText.length() - 1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < unquotedLiteral.length(); i++) {
            char c = unquotedLiteral.charAt(i);
            if (c == '\\' && i + 1 < unquotedLiteral.length()) {
                char nextChar = unquotedLiteral.charAt(i + 1);
                switch (nextChar) {
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 'b':
                        sb.append('\b');
                        i++;
                        break;
                    case 'f':
                        sb.append('\f');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < unquotedLiteral.length()) {
                            String hex = unquotedLiteral.substring(i + 2, i + 6);
                            try {
                                int unicodeChar = Integer.parseInt(hex, 16);
                                sb.append((char) unicodeChar);
                                i += 5;
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid \\uXXXX escape sequence in literal: \\u" + hex);
                            }
                        } else {
                            throw new IllegalArgumentException("Incomplete \\uXXXX escape sequence in literal: " + unquotedLiteral.substring(i));
                        }
                        break;
                    case 'U':
                        if (i + 9 < unquotedLiteral.length()) {
                            String hex = unquotedLiteral.substring(i + 2, i + 10);
                            try {
                                int unicodeChar = Integer.parseInt(hex, 16);
                                if (Character.isSupplementaryCodePoint(unicodeChar)) {
                                    sb.append(Character.highSurrogate(unicodeChar));
                                    sb.append(Character.lowSurrogate(unicodeChar));
                                } else {
                                    sb.append((char) unicodeChar);
                                }
                                i += 9;
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid \\UXXXXXXXX escape sequence in literal: \\U" + hex);
                            }
                        } else {
                            throw new IllegalArgumentException("Incomplete \\UXXXXXXXX escape sequence in literal: " + unquotedLiteral.substring(i));
                        }
                        break;
                    default:
                        sb.append(c).append(nextChar);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Unescapes common N-Quads URI escape sequences.
     * This method handles `\>`, `\\`, `\ uXXXX`, `\UXXXXXXXX`.
     *
     * @param uri The escaped URI string.
     * @return The unescaped URI string.
     */
    protected String unescapeUri(String uri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == '\\' && i + 1 < uri.length()) {
                char nextChar = uri.charAt(i + 1);
                switch (nextChar) {
                    case '>':
                        sb.append('>');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < uri.length()) {
                            String hex = uri.substring(i + 2, i + 6);
                            try {
                                int unicodeChar = Integer.parseInt(hex, 16);
                                sb.append((char) unicodeChar);
                                i += 5;
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid \\uXXXX escape sequence in URI: \\u" + hex);
                            }
                        } else {
                            throw new IllegalArgumentException("Incomplete \\uXXXX escape sequence in URI: " + uri.substring(i));
                        }
                        break;
                    case 'U':
                        if (i + 9 < uri.length()) {
                            String hex = uri.substring(i + 2, i + 10);
                            try {
                                int unicodeChar = Integer.parseInt(hex, 16);
                                if (Character.isSupplementaryCodePoint(unicodeChar)) {
                                    sb.append(Character.highSurrogate(unicodeChar));
                                    sb.append(Character.lowSurrogate(unicodeChar));
                                } else {
                                    sb.append((char) unicodeChar);
                                }
                                i += 9;
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid \\UXXXXXXXX escape sequence in URI: \\U" + hex);
                            }
                        } else {
                            throw new IllegalArgumentException("Incomplete \\UXXXXXXXX escape sequence in URI: " + uri.substring(i));
                        }
                        break;
                    default:
                        sb.append(c).append(nextChar);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
