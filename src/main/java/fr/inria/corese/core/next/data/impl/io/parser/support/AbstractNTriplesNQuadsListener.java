package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.ParsingException;

/**
 * Base class for N-Triples/N-Quads parsers providing common escape handling
 * and validation logic.
 */
public abstract class AbstractNTriplesNQuadsListener {

    protected final Model model;
    protected final ValueFactory factory;
    protected final IOOptions options;

    /**
     * Constructs a parser helper.
     *
     * @param model   RDF model to populate
     * @param factory ValueFactory for creating RDF terms
     * @param options IO configuration options
     */
    protected AbstractNTriplesNQuadsListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;
        this.options = options;
    }

    /**
     * Strips angle brackets from an IRI reference.
     *
     * @param iriRef IRI text with angle brackets
     * @return IRI text without angle brackets
     */
    public String stripAngles(String iriRef) {
        if (iriRef.length() < 2) {
            throw new ParsingException("Invalid IRI reference: too short");
        }
        return iriRef.substring(1, iriRef.length() - 1);
    }

    /**
     * Extracts the blank node label by removing the "_:" prefix.
     *
     * @param text Full blank node text including "_:" prefix
     * @return Blank node label without prefix
     */
    public String extractBlankNodeLabel(String text) {
        if (text.length() < 2) {
            throw new ParsingException("Invalid blank node: too short");
        }
        return text.substring(2);
    }

    /**
     * Validates a blank node label according to RDF specifications.
     * Labels must not be empty and cannot contain colons.
     *
     * @param label Blank node label without "_:" prefix
     * @throws ParsingException if the label is invalid
     */
    public void validateBlankNodeLabel(String label) {
        if (label == null || label.isEmpty()) {
            throw new ParsingException("Blank node label cannot be empty");
        }

        if (label.contains(ParserConstants.COLON)) {
            throw new ParsingException("Blank node label cannot contain colon (':')");
        }
    }

    /**
     * Unescapes literal strings according to N-Triples/N-Quads specification.
     *
     * @param literalText Raw literal text including quotes
     * @return Unescaped literal content without quotes
     */
    public String unescapeLiteral(String literalText) {
        String content = removeQuotes(literalText);
        return processEscapeSequences(content, "literal");
    }

    /**
     * Removes quotes from a literal string.
     * Supports both single quotes and triple quotes.
     *
     * @param text Quoted literal text
     * @return Content without quotes
     */
    public String removeQuotes(String text) {
        if (text.startsWith(ParserConstants.TRIPLE_QUOTE) && text.endsWith(ParserConstants.TRIPLE_QUOTE)) {
            if (text.length() < 6) {
                throw new ParsingException("Invalid triple-quoted string");
            }
            return text.substring(3, text.length() - 3);
        }

        if (text.startsWith(ParserConstants.QUOTE) && text.endsWith(ParserConstants.QUOTE)) {
            if (text.length() < 2) {
                throw new ParsingException("Invalid single-quoted string");
            }
            return text.substring(1, text.length() - 1);
        }

        throw new ParsingException("Literal does not have expected quotes: " + text);
    }

    /**
     * Unescapes URI strings according to N-Triples/N-Quads specification.
     *
     * @param uri Escaped URI string
     * @return Unescaped URI string
     */
    public String unescapeUri(String uri) {
        return processEscapeSequences(uri, "URI");
    }

    /**
     * Processes escape sequences in strings.
     * Handles: \", \\, \n, \t, \r, \b, \f, \ uXXXX, \UXXXXXXXX, \>
     *
     * @param input   String containing escape sequences
     * @param context Context for error messages ("literal" or "URI")
     * @return Unescaped string
     */
    @SuppressWarnings("java:S127")
    public String processEscapeSequences(String input, String context) {
        StringBuilder result = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && i + 1 < input.length()) {
                i = processEscapeSequence(input, i, result, context);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Processes a single escape sequence.
     *
     * @param input   Full input string
     * @param i       Current position (at backslash)
     * @param result  StringBuilder accumulating the result
     * @param context Context for error messages
     * @return New position after processing
     */
    public int processEscapeSequence(String input, int i, StringBuilder result, String context) {
        char next = input.charAt(i + 1);

        return switch (next) {
            case '"' -> {
                result.append('"');
                yield i + 1;
            }
            case '\'' -> {
                result.append('\'');
                yield i + 1;
            }
            case '\\' -> {
                result.append('\\');
                yield i + 1;
            }
            case '>' -> {
                result.append('>');
                yield i + 1;
            }
            case 'n' -> {
                result.append('\n');
                yield i + 1;
            }
            case 't' -> {
                result.append('\t');
                yield i + 1;
            }
            case 'r' -> {
                result.append('\r');
                yield i + 1;
            }
            case 'b' -> {
                result.append('\b');
                yield i + 1;
            }
            case 'f' -> {
                result.append('\f');
                yield i + 1;
            }
            case 'u' -> processUnicodeEscape(input, i, 4, result, context);
            case 'U' -> processUnicodeEscape(input, i, 8, result, context);
            default -> {
                result.append('\\').append(next);
                yield i + 1;
            }
        };
    }

    /**
     * Processes Unicode escape sequences  .
     *
     * @param input     Full input string
     * @param i         Current position (at backslash)
     * @param hexLength Length of hex digits (4 or 8)
     * @param result    StringBuilder accumulating the result
     * @param context   Context for error messages
     * @return New position after processing
     */
    public int processUnicodeEscape(String input, int i, int hexLength,
                                    StringBuilder result, String context) {
        if (i + hexLength + 1 >= input.length()) {
            String escapeType = hexLength == 4 ? "\\uXXXX" : "\\UXXXXXXXX";
            throw new ParsingException(
                    "Incomplete " + escapeType + " escape sequence in " + context + ": " +
                            input.substring(i));
        }

        String hex = input.substring(i + 2, i + 2 + hexLength);

        try {
            int codePoint = Integer.parseInt(hex, 16);
            appendCodePoint(result, codePoint);
            return i + hexLength + 1;
        } catch (NumberFormatException e) {
            String escapeType = hexLength == 4 ? "\\u" : "\\U";
            throw new ParsingException(
                    "Invalid " + escapeType + " escape sequence in " + context + ": " +
                            escapeType + hex);
        }
    }

    /**
     * Appends a Unicode code point, handling supplementary characters.
     *
     * @param result    StringBuilder to append to
     * @param codePoint Unicode code point
     */
    public void appendCodePoint(StringBuilder result, int codePoint) {
        if (Character.isSupplementaryCodePoint(codePoint)) {
            result.append(Character.highSurrogate(codePoint));
            result.append(Character.lowSurrogate(codePoint));
        } else {
            result.append((char) codePoint);
        }
    }

    /**
     * Creates a literal from parsed components.
     *
     * @param label       Literal lexical value
     * @param datatypeIRI Optional datatype IRI
     * @param languageTag Optional language tag
     * @return Created literal
     */
    public Literal createLiteral(String label, IRI datatypeIRI, String languageTag) {
        try {
            if (datatypeIRI != null) {
                return factory.createLiteral(label, datatypeIRI);
            }
            if (languageTag != null) {
                return factory.createLiteral(label, languageTag);
            }
            return factory.createLiteral(label);
        } catch (IllegalArgumentException e) {

            return factory.createLiteral(label);
        }
    }
}
