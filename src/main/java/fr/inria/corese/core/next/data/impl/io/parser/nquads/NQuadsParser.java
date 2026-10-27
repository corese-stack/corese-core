package fr.inria.corese.core.next.data.impl.io.parser.nquads;

import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.spi.io.parser.AbstractRDFParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Streaming RDF 1.1 N-Quads parser without tokens, parse trees, or line strings.
 *
 * <p>Each invocation owns its input buffer, reusable term builder and term maps.
 * IRIs are cached with a bounded cache; blank nodes are scoped to one document.
 * Memory is O(longest term + distinct blank node labels + cache size). Strings
 * and RDF terms required by the value factory are still allocated.</p>
 *
 * <p>Diagnostics use one-based lines and UTF-16 columns; CRLF counts as one line
 * ending. Input is borrowed and never closed. A malformed statement is not added,
 * but previously completed statements remain in the model. Neither the destination
 * model nor its factory is made thread-safe by this parser.</p>
 *
 * <p>The base URI is ignored: N-Quads requires absolute IRIs. RDF 1.2 triple
 * terms, VERSION directives and directional literals are not supported.</p>
 *
 * <p>An optional IRI or blank node graph label selects a named graph; statements
 * without a graph label are added to the default graph.</p>
 */
public final class NQuadsParser extends AbstractRDFParser {
    /** Creates a parser with the standard N-Quads options. */
    public NQuadsParser(Model model, ValueFactory factory) {
        this(model, factory, new NQuadsParserOptions.Builder().build());
    }

    /** Creates a parser using the supplied options. */
    public NQuadsParser(Model model, ValueFactory factory, RDFParsingOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.NQUADS;
    }

    @Override
    public void parse(InputStream input) throws ParsingException {
        parse(input, null);
    }

    @Override
    public void parse(InputStream input, String baseURI) throws ParsingException {
        Objects.requireNonNull(input, "input");
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        parse(new InputStreamReader(input, decoder), baseURI);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingException {
        new Document(Objects.requireNonNull(reader, "reader"), getModel(), getValueFactory()).parse();
    }

    /** All mutable parsing state is local to a single document. */
    private static final class Document {
        private static final int BUFFER_SIZE = 64 * 1024;
        private static final int IRI_CACHE_LIMIT = 4096;
        private final Reader reader;
        private final Model model;
        private final ValueFactory factory;
        private final char[] buffer = new char[BUFFER_SIZE];
        private final StringBuilder term = new StringBuilder(128);
        private final Map<String, IRI> iris = new HashMap<>();
        private final Map<String, BNode> blankNodes = new HashMap<>();
        private int offset;
        private int limit;
        private boolean eof;
        private long line = 1;
        private long column = 1;
        private boolean afterCr;
        private boolean blankNodeDot;

        private Document(Reader reader, Model model, ValueFactory factory) {
            this.reader = reader;
            this.model = model;
            this.factory = factory;
        }

        private void parse() {
            try {
                while (peek() != -1) {
                    checkInterrupted();
                    statement();
                }
            } catch (IOException e) {
                throw new ParsingException(location("Failed to read N-Quads input"), e);
            } catch (IllegalArgumentException | IncorrectFormatException e) {
                throw new ParsingException(location("Value factory rejected RDF term: " + e.getMessage()), e);
            }
        }

        private void statement() throws IOException {
            horizontalSpace();
            if (peek() == '#' || isLineEnd(peek()) || peek() == -1) {
                endOfLine();
                return;
            }
            Resource subject = resource();
            if (blankNodeDot) {
                throw error("Expected predicate IRI");
            }
            horizontalSpace();
            IRI predicate = iri();
            horizontalSpace();
            Value object = object();
            horizontalSpace();
            Resource graph = graphLabel();
            finishStatement();
            if (graph == null) {
                model.add(subject, predicate, object);
            } else {
                model.add(subject, predicate, object, graph);
            }
        }

        private Resource graphLabel() throws IOException {
            // A blank node may already have consumed the statement's final dot.
            if (blankNodeDot || peek() == '.') {
                return null;
            }
            return resource();
        }

        private void finishStatement() throws IOException {
            horizontalSpace();
            if (!blankNodeDot) {
                expect('.');
            }
            blankNodeDot = false;
            endOfLine();
        }

        private Resource resource() throws IOException {
            return switch (peek()) {
                case '<' -> iri();
                case '_' -> blankNode();
                default -> throw error("Expected IRI or blank node");
            };
        }

        private Value object() throws IOException {
            return peek() == '"' ? literal() : resource();
        }

        private IRI iri() throws IOException {
            expect('<');
            term.setLength(0);
            while (peek() != '>') {
                int c = peek();
                if (c == -1 || isLineEnd(c)) {
                    throw error("Unterminated IRI");
                }
                int scalar = c == '\\' ? escape(false) : scalar();
                if (forbiddenIriCharacter(scalar)) {
                    throw error("Forbidden character in IRI");
                }
                term.appendCodePoint(scalar);
            }
            expect('>');
            String text = term.toString();
            IRI cached = iris.get(text);
            if (cached != null) {
                return cached;
            }
            validateIri(text);
            IRI result = factory.createIRI(text);
            if (iris.size() == IRI_CACHE_LIMIT) {
                iris.clear();
            }
            iris.put(text, result);
            return result;
        }

        private static boolean forbiddenIriCharacter(int c) {
            return c <= 0x20 || "<>\"{}|^`\\".indexOf(c) >= 0;
        }

        private void validateIri(String text) {
            if (!isAbsoluteIri(text)) {
                throw error("N-Quads requires an absolute IRI");
            }
        }

        private static boolean isAbsoluteIri(String text) {
            if (text == null || text.isEmpty() || !isAsciiAlpha(text.charAt(0))) {
                return false;
            }
            int i = 1;
            int length = text.length();
            while (i < length) {
                char c = text.charAt(i);
                if (isSchemeChar(c)) {
                    i++;
                } else {
                    break;
                }
            }
            return i < length && text.charAt(i) == ':';
        }

        private static boolean isAsciiAlpha(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private static boolean isSchemeChar(char c) {
            return isAsciiAlpha(c) || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
        }

        private BNode blankNode() throws IOException {
            expect('_');
            expect(':');
            term.setLength(0);
            int first = scalar();
            if (!nameStart(first) && !digit(first)) {
                throw error("Invalid first character of blank node label");
            }
            term.appendCodePoint(first);
            int trailingDots = blankNodeTail();
            if (trailingDots > 1) {
                throw error("Blank node label cannot end with dots");
            }
            blankNodeDot = trailingDots == 1;
            term.setLength(term.length() - trailingDots);
            // Allocate fresh IDs, rather than leaking source labels across documents.
            return blankNodes.computeIfAbsent(term.toString(), factory::createBNode);
        }

        private int blankNodeTail() throws IOException {
            int dots = 0;
            while (true) {
                int c = peek();
                if (c == '.') {
                    take();
                    term.append('.');
                    dots++;
                } else if (nameCharacter(c) || Character.isHighSurrogate((char) c)) {
                    int cp = scalar();
                    if (!nameCharacter(cp)) {
                        throw error("Invalid character in blank node label");
                    }
                    term.appendCodePoint(cp);
                    dots = 0;
                } else {
                    return dots;
                }
            }
        }

        private Value literal() throws IOException {
            String label = quotedString();
            if (peek() == '@') {
                take();
                return factory.createLiteral(label, language());
            }
            if (peek() == '^') {
                expect('^');
                expect('^');
                return factory.createLiteral(label, iri());
            }
            return factory.createLiteral(label);
        }

        private String quotedString() throws IOException {
            expect('"');
            term.setLength(0);
            while (peek() != '"') {
                int c = peek();
                if (c == -1 || isLineEnd(c)) {
                    throw error("Unterminated literal");
                }
                term.appendCodePoint(c == '\\' ? escape(true) : scalar());
            }
            expect('"');
            return term.toString();
        }

        private String language() throws IOException {
            term.setLength(0);
            if (!letter(peek())) {
                throw error("Language tag must start with an ASCII letter");
            }
            while (letter(peek())) {
                term.append((char) take());
            }
            while (peek() == '-') {
                term.append((char) take());
                languageSubtag();
            }
            return term.toString();
        }

        private void languageSubtag() throws IOException {
            if (!letter(peek()) && !digit(peek())) {
                throw error("Expected nonempty alphanumeric language subtag");
            }
            while (letter(peek()) || digit(peek())) {
                term.append((char) take());
            }
        }

        private int escape(boolean literal) throws IOException {
            expect('\\');
            int type = take();
            if (type == 'u' || type == 'U') {
                return unicodeEscape(type == 'u' ? 4 : 8);
            }
            if (!literal) {
                throw error("Only Unicode escapes are allowed in IRIs");
            }
            return switch (type) {
                case 't' -> '\t';
                case 'b' -> '\b';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 'f' -> '\f';
                case '"', '\'', '\\' -> type;
                default -> throw error("Invalid literal escape");
            };
        }

        private int unicodeEscape(int digits) throws IOException {
            long result = 0;
            for (int i = 0; i < digits; i++) {
                int hex = hexValue(peek());
                if (hex < 0) {
                    throw error("Expected hexadecimal digit in Unicode escape");
                }
                take();
                result = (result << 4) | hex;
            }
            if (result > Character.MAX_CODE_POINT || result >= 0xD800 && result <= 0xDFFF) {
                throw error("Unicode escape is not a Unicode scalar value");
            }
            return (int) result;
        }

        private static int hexValue(int c) {
            if (digit(c)) {
                return c - '0';
            }
            if (c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            }
            return c >= 'A' && c <= 'F' ? c - 'A' + 10 : -1;
        }

        /** Reads one scalar, rejecting isolated UTF-16 surrogates, even in comments. */
        private int scalar() throws IOException {
            int first = peek();
            if (first == -1) {
                throw error("Unexpected end of input");
            }
            if (Character.isLowSurrogate((char) first)) {
                throw error("Unpaired low surrogate");
            }
            take();
            if (!Character.isHighSurrogate((char) first)) {
                return first;
            }
            int second = peek();
            if (second == -1 || !Character.isLowSurrogate((char) second)) {
                throw error("Unpaired high surrogate");
            }
            take();
            return Character.toCodePoint((char) first, (char) second);
        }

        private void horizontalSpace() throws IOException {
            while (peek() == ' ' || peek() == '\t') {
                take();
            }
        }

        private void endOfLine() throws IOException {
            horizontalSpace();
            if (peek() == '#') {
                while (peek() != -1 && !isLineEnd(peek())) {
                    scalar();
                }
            }
            if (peek() == -1) {
                return;
            }
            if (!isLineEnd(peek())) {
                throw error("Expected end of line or comment");
            }
            int ending = take();
            if (ending == '\r' && peek() == '\n') {
                take();
            }
        }

        private void expect(int expected) throws IOException {
            if (peek() != expected) {
                throw error("Expected '" + (char) expected + "'");
            }
            take();
        }

        private int peek() throws IOException {
            if (offset == limit && !eof) {
                checkInterrupted();
                int count = reader.read(buffer, 0, buffer.length);
                if (count == 0) {
                    throw new IOException("Reader returned zero characters for a nonempty buffer");
                }
                eof = count < 0;
                offset = 0;
                limit = Math.max(count, 0);
            }
            return offset == limit ? -1 : buffer[offset];
        }

        private int take() throws IOException {
            int c = peek();
            if (c == -1) {
                return -1;
            }
            offset++;
            updatePosition(c);
            return c;
        }

        private void updatePosition(int c) {
            if (c == '\r') {
                line++;
                column = 1;
            } else if (c == '\n') {
                if (!afterCr) {
                    line++;
                }
                column = 1;
            } else {
                column++;
            }
            afterCr = c == '\r';
        }

        private void checkInterrupted() {
            if (Thread.currentThread().isInterrupted()) {
                throw error("RDF parsing was interrupted");
            }
        }

        private ParsingException error(String message) {
            return new ParsingException(location(message));
        }

        private String location(String message) {
            return message + " at line " + line + ", column " + column;
        }

        private static boolean isLineEnd(int c) {
            return c == '\r' || c == '\n';
        }

        private static boolean letter(int c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        private static boolean digit(int c) {
            return c >= '0' && c <= '9';
        }

        private static boolean nameStart(int c) {
            return c == '_' || letter(c) || unicodeNameStart(c);
        }

        /** Exact PN_CHARS_BASE ranges; Character.isLetter is insufficient. */
        private static boolean unicodeNameStart(int c) {
            return between(c, 0xC0, 0xD6) || between(c, 0xD8, 0xF6)
                    || between(c, 0xF8, 0x2FF) || between(c, 0x370, 0x37D)
                    || between(c, 0x37F, 0x1FFF) || between(c, 0x200C, 0x200D)
                    || between(c, 0x2070, 0x218F) || between(c, 0x2C00, 0x2FEF)
                    || between(c, 0x3001, 0xD7FF) || between(c, 0xF900, 0xFDCF)
                    || between(c, 0xFDF0, 0xFFFD) || between(c, 0x10000, 0xEFFFF);
        }

        private static boolean nameCharacter(int c) {
            return nameStart(c) || digit(c) || c == '-' || c == 0xB7
                    || between(c, 0x300, 0x36F) || between(c, 0x203F, 0x2040);
        }

        private static boolean between(int c, int lower, int upper) {
            return c >= lower && c <= upper;
        }
    }
}
