package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.NQuadsParser;

/**
 * Listener for the ANTLR4 generated parser for N-Quads.
 * This listener traverses the parse tree and builds the RDF model,
 * supporting named graphs. It includes unescaping logic for URIs and literals.
 */
public class NQuadsListener extends NQuadsBaseListener {

    private final Model model;
    private final ValueFactory factory;
    @SuppressWarnings("unused")
    private final IOOptions options;

    private Resource currentSubject;
    private IRI currentPredicate;
    private Resource currentGraph;

    /**
     * Constructor for the NQuadsListener.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration (if any).
     */
    public NQuadsListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;
        this.options = options;
    }



    /**
     * Exits a statement context, extracting the object and adding the complete triple/quad to the model.
     * Resets the current subject, predicate, and graph.
     * @param ctx The StatementContext from the ANTLR parse tree.
     */
    @Override
    public void enterStatement(NQuadsParser.StatementContext ctx) {
        currentSubject = extractSubject(ctx.subject());
        currentPredicate = extractPredicate(ctx.predicate());
        currentGraph = (ctx.graphLabel() != null) ? extractGraph(ctx.graphLabel()) : null;
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
     * Handles unescaping of URI characters for IRIs and extracts blank node labels.
     * @param ctx The SubjectContext from the ANTLR parse tree.
     * @return The created Resource (IRI or BNode).
     * @throws ParsingErrorException if the subject type is unsupported or blank node label is invalid.
     */
    protected Resource extractSubject(NQuadsParser.SubjectContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(stripAngles(ctx.IRIREF().getText())));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = ctx.BLANK_NODE_LABEL().getText().substring(2);
            validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        throw new ParsingErrorException("Unsupported N-Quads subject: " + ctx.getText());
    }
    /**
     * Extracts a predicate (IRI) from the predicate context.
     * Handles unescaping of URI characters.
     * @param ctx The PredicateContext from the ANTLR parse tree.
     * @return The created IRI.
     * @throws ParsingErrorException if the predicate type is unsupported.
     */
    protected IRI extractPredicate(NQuadsParser.PredicateContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(stripAngles(ctx.IRIREF().getText())));
        }
        throw new ParsingErrorException("Unsupported N-Quads predicate: " + ctx.getText());
    }

    /**
     * Extracts a value (IRI, Blank Node, or Literal) from the object context.
     * Delegates to specific extraction methods based on the object type.
     * @param ctx The ObjectContext from the ANTLR parse tree.
     * @return The created Value (IRI, BNode, or Literal).
     * @throws ParsingErrorException if the object type is unsupported or blank node label is invalid.
     */
    protected Value extractObject(NQuadsParser.ObjectContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(stripAngles(ctx.IRIREF().getText())));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = ctx.BLANK_NODE_LABEL().getText().substring(2);
            validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new ParsingErrorException("Unsupported N-Quads object: " + ctx.getText());
    }

    /**
     * Extracts a graph (IRI or Blank Node) from the graph context.
     * Handles unescaping of URI characters for IRIs and extracts blank node labels.
     * @param ctx The GraphLabelContext from the ANTLR parse tree.
     * @return The created Resource (IRI or BNode) representing the graph.
     * @throws ParsingErrorException if the graph label type is unsupported or blank node label is invalid.
     */
    protected Resource extractGraph(NQuadsParser.GraphLabelContext ctx) {
        if (ctx.IRIREF() != null) {
            return factory.createIRI(unescapeUri(stripAngles(ctx.IRIREF().getText())));
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = ctx.BLANK_NODE_LABEL().getText().substring(2);
            validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        throw new ParsingErrorException("Unsupported N-Quads graph: " + ctx.getText());
    }

    /**
     * Extracts and unescapes a literal from the ANTLR context.
     * This method handles string literals with or without datatype/language.
     * @param ctx The LiteralContext from the ANTLR parse tree.
     * @return The created Literal value.
     */
    protected Literal extractLiteral(NQuadsParser.LiteralContext ctx) {
        String rawLiteralText;
        if (ctx.STRING_LITERAL_QUOTE() != null) {
            rawLiteralText = ctx.STRING_LITERAL_QUOTE().getText();
        }
        else {
            throw new ParsingErrorException("Unsupported literal type or missing literal token: " + ctx.getText());
        }
        String label = unescapeLiteral(rawLiteralText);

        if (ctx.IRIREF() != null) {
            IRI datatype = factory.createIRI(unescapeUri(stripAngles(ctx.IRIREF().getText())));
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
     * This method handles \", \\, \n, \t, \r, \b, \f.
     * It also removes the surrounding quotes from the literal string.
     *
     * @param literalText The raw literal string from ANTLR (including quotes and escapes).
     * @return The unescaped literal string without surrounding quotes.
     * @throws ParsingErrorException if an invalid Unicode escape sequence is found.
     */
    protected String unescapeLiteral(String literalText) {
        String unquotedLiteral;
        int quoteLength;
        if (literalText.startsWith("\"\"\"") && literalText.endsWith("\"\"\"")) {
            if (literalText.length() < 6) {
                throw new ParsingErrorException("Invalid triple-quoted string");
            }
            quoteLength = 3;
        } else if (literalText.startsWith("\"") && literalText.endsWith("\"")) {
            if (literalText.length() < 2) {
                throw new ParsingErrorException("Invalid single-quoted string");
            }
            quoteLength = 1;
        } else {
            throw new ParsingErrorException("Literal text does not start/end with expected N-Quads quotes: " + literalText);
        }

        unquotedLiteral = literalText.substring(quoteLength, literalText.length() - quoteLength);

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
                                throw new ParsingErrorException("Invalid \\uXXXX escape sequence in literal: \\u" + hex);
                            }
                        } else {
                            throw new ParsingErrorException("Incomplete \\uXXXX escape sequence in literal: " + unquotedLiteral.substring(i));
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
                                throw new ParsingErrorException("Invalid \\UXXXXXXXX escape sequence in literal: \\U" + hex);
                            }
                        } else {
                            throw new ParsingErrorException("Incomplete \\UXXXXXXXX escape sequence in literal: " + unquotedLiteral.substring(i));
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
     * This method handles \>, \\, \ uXXXX, \UXXXXXXXX.
     *
     * @param uri The escaped URI string.
     * @return The unescaped URI string.
     * @throws ParsingErrorException if an invalid Unicode escape sequence is found.
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
                                throw new ParsingErrorException("Invalid \\uXXXX escape sequence in URI: \\u" + hex);
                            }
                        } else {
                            throw new ParsingErrorException("Incomplete \\uXXXX escape sequence in URI: " + uri.substring(i));
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
                                throw new ParsingErrorException("Invalid \\UXXXXXXXX escape sequence in URI: \\U" + hex);
                            }
                        } else {
                            throw new ParsingErrorException("Incomplete \\UXXXXXXXX escape sequence in URI: " + uri.substring(i));
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
    private String stripAngles(String iriRef) {
        return iriRef.substring(1, iriRef.length() - 1);
    }
    /**
     * Validates a blank node label according to RDF 1.1 N-Quads specification.
     * Blank node labels must match PN_LOCAL rules, which means they cannot be empty,
     * and cannot contain colons. They *can* start with a digit.
     * @param label The blank node label string (without the "_:" prefix).
     * @throws ParsingErrorException if the blank node label is invalid.
     */
    protected void validateBlankNodeLabel(String label) {
        if (label.isEmpty()) {
            throw new ParsingErrorException("Blank node label cannot be empty");
        }
        if (label.contains(":")) {
            throw new ParsingErrorException("Blank node label cannot contain colon");
        }

        if (!label.matches("^[A-Za-z_0-9][A-Za-z0-9_\\-\\.]*$")) {
            throw new ParsingErrorException("Invalid blank node label syntax: " + label);
        }
    }
}
