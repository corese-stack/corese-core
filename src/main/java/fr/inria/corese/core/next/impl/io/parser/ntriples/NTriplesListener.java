package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.parser.antlr.NTriplesBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.NTriplesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the ANTLR4 generated parser for N-Triples.
 * This listener traverses the parse tree and builds the RDF model.
 * It includes unescaping logic for URIs and literals.
 */
public class NTriplesListener extends NTriplesBaseListener {

    private static final Logger logger = LoggerFactory.getLogger(NTriplesListener.class);

    private final Model model;
    private final ValueFactory factory;
    private final IOOptions options;

    private Resource currentSubject;
    private IRI currentPredicate;

    /**
     * Constructor for the NTriplesListener.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration (if any).
     */
    public NTriplesListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;
        this.options = options;
    }

    @Override
    public void enterTriple(NTriplesParser.TripleContext ctx) {
        currentSubject = extractSubject(ctx.subject());
        currentPredicate = extractPredicate(ctx.predicate());
    }

    @Override
    public void exitTriple(NTriplesParser.TripleContext ctx) {
        Value object = extractObject(ctx.object());
        model.add(currentSubject, currentPredicate, object);
        currentSubject = null;
        currentPredicate = null;
    }

    /**
     * Extracts a resource (IRI or Blank Node) from the subject context.
     */
    protected Resource extractSubject(NTriplesParser.SubjectContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            return factory.createBNode(ctx.BLANK_NODE_LABEL().getText().substring(2));
        }
        throw new IllegalArgumentException("Unsupported N-Triples subject: " + ctx.getText());
    }

    /**
     * Extracts a predicate (IRI) from the predicate context.
     */
    protected IRI extractPredicate(NTriplesParser.PredicateContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        throw new IllegalArgumentException("Unsupported N-Triples predicate: " + ctx.getText());
    }

    /**
     * Extracts a value (IRI, Blank Node, or Literal) from the object context.
     */
    protected Value extractObject(NTriplesParser.ObjectContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(ctx.IRIREF().getText().substring(1, ctx.IRIREF().getText().length() - 1)));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            return factory.createBNode(ctx.BLANK_NODE_LABEL().getText().substring(2));
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new IllegalArgumentException("Unsupported N-Triples object: " + ctx.getText());
    }

    /**
     * Extracts and unescapes a literal from the ANTLR context.
     * This method handles string literals with or without datatype/language.
     */
    protected Literal extractLiteral(NTriplesParser.LiteralContext ctx) {
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
     * Unescapes common N-Triples literal escape sequences.
     * This method handles `\"`, `\\`, `\n`, `\t`, `\r`, `\b`, `\f`.
     * It also handles `\ uXXXX` and `\UXXXXXXXX` for Unicode escapes.
     * It also removes the surrounding quotes from the literal string.
     *
     * @param literalText The raw literal string from ANTLR (including quotes and escapes).
     * @return The unescaped literal string without surrounding quotes.
     */
    protected String unescapeLiteral(String literalText) {
        String unquotedLiteral = literalText.substring(1, literalText.length() - 1);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < unquotedLiteral.length(); i++) {
            char c = unquotedLiteral.charAt(i);
            if (c == '\\' && i + 1 < unquotedLiteral.length()) {
                char nextChar = unquotedLiteral.charAt(i + 1);
                switch (nextChar) {
                    case '"':
                        builder.append('"');
                        i++;
                        break;
                    case '\\':
                        builder.append('\\');
                        i++;
                        break;
                    case 'n':
                        builder.append('\n');
                        i++;
                        break;
                    case 't':
                        builder.append('\t');
                        i++;
                        break;
                    case 'r':
                        builder.append('\r');
                        i++;
                        break;
                    case 'b':
                        builder.append('\b');
                        i++;
                        break;
                    case 'f':
                        builder.append('\f');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < unquotedLiteral.length()) {
                            String hex = unquotedLiteral.substring(i + 2, i + 6);
                            try {
                                int unicodeChar = Integer.parseInt(hex, 16);
                                builder.append((char) unicodeChar);
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
                                    builder.append(Character.highSurrogate(unicodeChar));
                                    builder.append(Character.lowSurrogate(unicodeChar));
                                } else {
                                    builder.append((char) unicodeChar);
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
                        builder.append(c).append(nextChar);
                        i++;
                        break;
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * Unescapes common N-Triples URI escape sequences.
     * This method handles `\>`, `\\`, `\ uXXXX`, `\UXXXXXXXX`.
     *
     * @param uri The escaped URI string.
     * @return The unescaped URI string.
     */
    protected String unescapeUri(String uri) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == '\\' && i + 1 < uri.length()) {
                char nextChar = uri.charAt(i + 1);
                switch (nextChar) {
                    case '>':
                        builder.append('>');
                        i++;
                        break;
                    case '\\':
                        builder.append('\\');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < uri.length()) {
                            String hex = uri.substring(i + 2, i + 6);
                            try {
                                int unicodeChar = Integer.parseInt(hex, 16);
                                builder.append((char) unicodeChar);
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
                                    builder.append(Character.highSurrogate(unicodeChar));
                                    builder.append(Character.lowSurrogate(unicodeChar));
                                } else {
                                    builder.append((char) unicodeChar);
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
                        builder.append(c).append(nextChar);
                        i++;
                        break;
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
