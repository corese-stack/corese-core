package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Character-level streaming FSM for Turtle and TriG documents.
 *
 * <p>Implements an event-driven byte-streaming scanner that handles Turtle/TriG
 * directives ({@code @prefix}, {@code PREFIX}, {@code @base}, {@code BASE}) and
 * triples/quads without allocating intermediate AST nodes. Override
 * {@link #emitTriple} to route statements to named graphs (TriG) or override
 * {@link #parseDocument} to intercept graph blocks.</p>
 *
 * <p>Memory is O(longest term + distinct blank node labels + IRI cache size).
 * Diagnostics use one-based lines and UTF-16 columns; CRLF counts as one line
 * ending. Input is borrowed and never closed.</p>
 */
public class TurtleTriGDocument {

    // ── Constants ──────────────────────────────────────────────────────────
    static final int BUFFER_SIZE     = 64 * 1024;
    static final int IRI_CACHE_LIMIT = 4096;
    private static final int NO_PUSHBACK = Integer.MIN_VALUE;

    // ── I/O state ──────────────────────────────────────────────────────────
    private final Reader reader;
    private final char[] buffer = new char[BUFFER_SIZE];
    private int  bufOffset, bufLimit;
    private boolean eof;
    /** One-character pushback slot used for dot-in-local-name disambiguation. */
    private int  pushback = NO_PUSHBACK;

    // ── Position tracking ──────────────────────────────────────────────────
    protected long line   = 1;
    protected long column = 1;
    private boolean afterCr;

    // ── RDF infrastructure ─────────────────────────────────────────────────
    protected final Model        model;
    protected final ValueFactory factory;

    // ── Shared term builder – reset before every leaf token ────────────────
    private final StringBuilder term = new StringBuilder(256);

    // ── Term caches ────────────────────────────────────────────────────────
    private final Map<String, IRI>   iriCache   = new HashMap<>();
    private final Map<String, BNode> blankNodes = new HashMap<>();

    // ── Prefix / base state ────────────────────────────────────────────────
    protected final Map<String, String> prefixes = new HashMap<>();
    protected String baseURI;

    // ── Pre-cached RDF / XSD constants ────────────────────────────────────
    protected final IRI RDF_TYPE;
    protected final IRI RDF_FIRST;
    protected final IRI RDF_REST;
    protected final IRI RDF_NIL;
    protected final IRI XSD_INTEGER;
    protected final IRI XSD_DECIMAL;
    protected final IRI XSD_DOUBLE;
    protected final IRI XSD_BOOLEAN;


    public TurtleTriGDocument(Reader reader, Model model, ValueFactory factory, String baseURI) {
        this.reader  = reader;
        this.model   = model;
        this.factory = factory;
        this.baseURI = baseURI != null ? baseURI : "";
        RDF_TYPE    = intern("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        RDF_FIRST   = intern("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
        RDF_REST    = intern("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
        RDF_NIL     = intern("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil");
        XSD_INTEGER = intern("http://www.w3.org/2001/XMLSchema#integer");
        XSD_DECIMAL = intern("http://www.w3.org/2001/XMLSchema#decimal");
        XSD_DOUBLE  = intern("http://www.w3.org/2001/XMLSchema#double");
        XSD_BOOLEAN = intern("http://www.w3.org/2001/XMLSchema#boolean");
    }


    public final void parse() {
        try {
            parseDocument();
        } catch (IOException e) {
            throw new ParsingException(location("Failed to read Turtle/TriG input"), e);
        } catch (IllegalArgumentException | IncorrectFormatException e) {
            throw new ParsingException(location("Value factory rejected RDF term: " + e.getMessage()), e);
        }
    }


    protected void parseDocument() throws IOException {
        while (true) {
            ws();
            if (peek() == -1) break;
            checkInterrupted();
            topLevel();
        }
    }

    /**
     * Dispatches one top-level item: {@code @prefix}, {@code @base},
     * SPARQL {@code PREFIX}/{@code BASE}, or a triple statement.
     */
    protected void topLevel() throws IOException {
        int c = peek();

        // @prefix / @base
        if (c == '@') { atDirective(); return; }

        // Blank node property list as subject: [ ... ] predicateObjectList? .
        if (c == '[') {
            Resource subj = blankNodePropertyList();
            ws();
            if (peek() != '.') { predicateObjectList(subj); ws(); }
            expect('.');
            return;
        }

        // Collection as subject: ( items ) predicateObjectList .
        if (c == '(') {
            Resource subj = collection();
            ws();
            predicateObjectList(subj);
            ws();
            expect('.');
            return;
        }

        // Angle-bracket IRI as subject
        if (c == '<') {
            Resource subj = intern(iriRefContent());
            ws();
            predicateObjectList(subj);
            ws();
            expect('.');
            return;
        }

        // Blank node _:label as subject
        if (c == '_') {
            Resource subj = blankNodeLabel();
            ws();
            predicateObjectList(subj);
            ws();
            expect('.');
            return;
        }

        // Default-prefix name :local
        if (c == ':') {
            take();
            Resource subj = intern(lookupPrefix("") + readLocalPart());
            ws();
            predicateObjectList(subj);
            ws();
            expect('.');
            return;
        }

        // Name: SPARQL keyword (PREFIX / BASE) or prefixed-name subject
        if (isAsciiAlpha(c) || unicodeNameStart(c)) {
            String name = readName();
            if (peek() == ':') {
                take();
                Resource subj = intern(lookupPrefix(name) + readLocalPart());
                ws();
                predicateObjectList(subj);
                ws();
                expect('.');
            } else if (name.equalsIgnoreCase("PREFIX")) {
                ws();
                sparqlPrefixDirective();
            } else if (name.equalsIgnoreCase("BASE")) {
                ws();
                sparqlBaseDirective();
            } else {
                throw error("Expected ':' after '" + name + "' or a valid directive keyword");
            }
            return;
        }

        throw error("Unexpected character at statement start: '" + (char) c + "'");
    }


    protected void atDirective() throws IOException {
        expect('@');
        String kw = readName();
        ws();
        if      (kw.equals("prefix")) turtlePrefixBody(false);
        else if (kw.equals("base"))   turtleBaseBody(false);
        else throw error("Unknown directive: @" + kw);
    }

    protected void sparqlPrefixDirective() throws IOException { turtlePrefixBody(true); }
    protected void sparqlBaseDirective()   throws IOException { turtleBaseBody(true);   }

    protected void turtlePrefixBody(boolean sparql) throws IOException {
        term.setLength(0);
        while (peek() != ':' && peek() != -1 && !isWhitespace(peek())) {
            term.appendCodePoint(scalar());
        }
        String prefix = term.toString();
        expect(':');
        ws();
        String ns = iriRefContent();
        registerPrefix(prefix, ns);
        if (!sparql) { ws(); expect('.'); }
    }

    protected void turtleBaseBody(boolean sparql) throws IOException {
        baseURI = iriRefContent();
        if (!sparql) { ws(); expect('.'); }
    }

    protected void registerPrefix(String prefix, String ns) {
        prefixes.put(prefix, ns);
        model.setNamespace(prefix, ns);
    }


    protected void predicateObjectList(Resource subj) throws IOException {
        IRI pred = verb();
        ws();
        objectList(subj, pred);
        ws();
        while (peek() == ';') {
            take();
            ws();
            int c = peek();
            if (c == '.' || c == '}' || c == -1) break; // trailing semicolon is legal
            if (c == ';') continue; // skip consecutive semicolons (;; is valid)
            pred = verb();
            ws();
            objectList(subj, pred);
            ws();
        }
    }

    private void objectList(Resource subj, IRI pred) throws IOException {
        Value obj = object();
        emitTriple(subj, pred, obj);
        ws();
        while (peek() == ',') {
            take();
            ws();
            obj = object();
            emitTriple(subj, pred, obj);
            ws();
        }
    }

    protected IRI verb() throws IOException {
        int c = peek();
        if (c == '<') return intern(iriRefContent());
        if (c == '_') throw error("Blank node cannot be used as predicate");
        if (c == ':') { take(); return intern(lookupPrefix("") + readLocalPart()); }
        String name = readName();
        if (peek() == ':') { take(); return intern(lookupPrefix(name) + readLocalPart()); }
        if (name.equals("a")) return RDF_TYPE;
        throw error("Expected predicate IRI or 'a', got: '" + name + "'");
    }


    protected Resource subject() throws IOException {
        int c = peek();
        if (c == '<') return intern(iriRefContent());
        if (c == '_') return blankNodeLabel();
        if (c == '[') return blankNodePropertyList();
        if (c == '(') return collection();
        if (c == ':') { take(); return intern(lookupPrefix("") + readLocalPart()); }
        if (isAsciiAlpha(c) || unicodeNameStart(c)) {
            String name = readName();
            if (peek() == ':') { take(); return intern(lookupPrefix(name) + readLocalPart()); }
            throw error("Expected ':' after prefix '" + name + "'");
        }
        throw error("Expected subject (IRI, blank node, or collection), got: '" + (char) c + "'");
    }


    protected Value object() throws IOException {
        int c = peek();
        if (c == '<')                    return intern(iriRefContent());
        if (c == '_')                    return blankNodeLabel();
        if (c == '[')                    return blankNodePropertyList();
        if (c == '(')                    return collection();
        if (c == '"' || c == '\'')       return literal();
        if (c == '+' || c == '-' || digit(c)) return numericLiteral();
        if (c == '.')                    return numericLeadingDot();
        if (c == ':') { take(); return intern(lookupPrefix("") + readLocalPart()); }
        if (isAsciiAlpha(c) || unicodeNameStart(c)) {
            String name = readName();
            if (peek() == ':') { take(); return intern(lookupPrefix(name) + readLocalPart()); }
            if (name.equals("true"))  return factory.createLiteral("true",  XSD_BOOLEAN);
            if (name.equals("false")) return factory.createLiteral("false", XSD_BOOLEAN);
            throw error("Unexpected token in object position: '" + name + "'");
        }
        throw error("Expected object (IRI, literal, blank node, or collection), got: '" + (char) c + "'");
    }


    /**
     * Reads {@code <iri>}, resolves against {@link #baseURI}, and returns
     * the absolute IRI string. Does not create an IRI object; caller should use
     * {@link #intern(String)}.
     */
    protected String iriRefContent() throws IOException {
        expect('<');
        term.setLength(0);
        while (peek() != '>') {
            int c = peek();
            if (c == -1 || isLineEnd(c)) throw error("Unterminated IRI reference");
            int scalar = (c == '\\') ? escape(false) : scalar();
            if (forbiddenIriChar(scalar)) throw error("Forbidden character U+" + Integer.toHexString(scalar) + " in IRI");
            term.appendCodePoint(scalar);
        }
        expect('>');
        return resolveIRI(term.toString());
    }

    protected String resolveIRI(String raw) {
        if (raw.isEmpty()) return baseURI.isEmpty() ? "" : baseURI;
        if (isAbsoluteIri(raw)) return raw;
        if (baseURI.isEmpty()) return raw;
        return IRIUtils.resolveIRIAgainstBase(baseURI, raw);
    }

    protected String lookupPrefix(String prefix) {
        String ns = prefixes.get(prefix);
        if (ns == null) {
            throw error(prefix.isEmpty()
                ? "Undeclared default prefix ':'; declare with '@prefix : <namespace> .'"
                : "Undeclared prefix: '" + prefix + "'");
        }
        return ns;
    }


    protected BNode blankNodeLabel() throws IOException {
        expect('_');
        expect(':');
        term.setLength(0);
        int first = scalar();
        if (!nameStart(first) && !digit(first)) throw error("Invalid blank node label start character");
        term.appendCodePoint(first);
        int trailingDots = blankNodeTail();
        if (trailingDots > 1) throw error("Blank node label cannot end with multiple dots");
        if (trailingDots == 1) {
            term.setLength(term.length() - 1);
            pushBack();
        }
        return blankNodes.computeIfAbsent(term.toString(), k -> factory.createBNode());
    }

    private int blankNodeTail() throws IOException {
        int dots = 0;
        while (true) {
            int c = peek();
            if (c == '.') {
                take(); term.append('.'); dots++;
            } else if (nameCharacter(c) || Character.isHighSurrogate((char) c)) {
                int cp = scalar();
                if (!nameCharacter(cp)) throw error("Invalid character in blank node label");
                term.appendCodePoint(cp); dots = 0;
            } else {
                return dots;
            }
        }
    }


    protected Resource blankNodePropertyList() throws IOException {
        expect('[');
        ws();
        BNode bnode = factory.createBNode();
        if (peek() != ']') {
            predicateObjectList(bnode);
            ws();
        }
        expect(']');
        return bnode;
    }


    protected Resource collection() throws IOException {
        expect('(');
        ws();
        if (peek() == ')') { take(); return RDF_NIL; }
        BNode head    = factory.createBNode();
        BNode current = head;
        while (true) {
            Value item = object();
            ws();
            emitTriple(current, RDF_FIRST, item);
            if (peek() == ')') {
                emitTriple(current, RDF_REST, RDF_NIL);
                take();
                break;
            }
            BNode next = factory.createBNode();
            emitTriple(current, RDF_REST, next);
            current = next;
        }
        return head;
    }


    private Literal literal() throws IOException {
        int q = peek();
        take(); // opening quote
        String label;
        if (peek() == q) {          // potential triple quote or empty string
            take();
            if (peek() == q) {      // confirmed triple quote
                take();
                label = longStringContent((char) q);
            } else {
                label = "";         // empty short string ""
            }
        } else {
            label = shortStringContent((char) q);
        }
        // Language tag
        if (peek() == '@') {
            take();
            return factory.createLiteral(label, language());
        }
        // Datatype IRI ^^<iri> or ^^prefix:local
        if (peek() == '^') {
            expect('^'); expect('^');
            String dtIri;
            if (peek() == '<') {
                dtIri = iriRefContent();
            } else {
                String pfx = peek() == ':' ? "" : readName();
                expect(':');
                dtIri = lookupPrefix(pfx) + readLocalPart();
            }
            return factory.createLiteral(label, intern(dtIri));
        }
        return factory.createLiteral(label);
    }

    private String shortStringContent(char q) throws IOException {
        term.setLength(0);
        while (true) {
            int c = peek();
            if ((char) c == q) { take(); return term.toString(); }
            if (c == -1 || isLineEnd(c)) throw error("Unterminated string literal");
            term.appendCodePoint(c == '\\' ? escape(true) : scalar());
        }
    }

    private String longStringContent(char q) throws IOException {
        term.setLength(0);
        while (true) {
            int c = peek();
            if (c == -1) throw error("Unterminated long string literal");
            if ((char) c == q) {
                take();
                if (peek() == (int) q) {
                    take();
                    if (peek() == (int) q) { take(); return term.toString(); } // closing """
                    term.append(q); // two quotes, not closing
                }
                term.append(q); // one quote, not closing
            } else {
                term.appendCodePoint(c == '\\' ? escape(true) : scalar());
            }
        }
    }

    private String language() throws IOException {
        term.setLength(0);
        if (!letter(peek())) throw error("Language tag must start with an ASCII letter");
        while (letter(peek()))                term.append((char) take());
        while (peek() == '-') {
            term.append((char) take());
            if (!letter(peek()) && !digit(peek())) throw error("Expected alphanumeric language subtag after '-'");
            while (letter(peek()) || digit(peek())) term.append((char) take());
        }
        return term.toString();
    }


    private Literal numericLiteral() throws IOException {
        term.setLength(0);
        int c = peek();
        if (c == '+' || c == '-') term.append((char) take()); // sign
        if (!digit(peek()) && peek() != '.') throw error("Expected digit or '.' in numeric literal");
        return numericBody();
    }

    /** Called when leading '.' is seen in object position (potential {@code .5}). */
    private Value numericLeadingDot() throws IOException {
        // We haven't consumed the '.' yet. Check next char for digit.
        // Peek at buffer position +1 if available; otherwise take+pushBack.
        boolean followedByDigit;
        if (pushback == NO_PUSHBACK && bufOffset + 1 < bufLimit) {
            followedByDigit = digit(buffer[bufOffset + 1]);
        } else {
            take(); // consume '.'
            followedByDigit = digit(peek());
            pushBack();
        }
        if (!followedByDigit) throw error("Unexpected '.' in object position");
        return numericLiteral();
    }

    private Literal numericBody() throws IOException {
        boolean hasDot = false, hasExp = false;
        // Integer part
        while (digit(peek())) term.append((char) take());
        // Decimal dot (only if followed by digit or exponent — e.g. "1.e5" is valid)
        if (peek() == '.') {
            take(); // tentatively consume
            if (digit(peek())) {
                term.append('.');
                hasDot = true;
                while (digit(peek())) term.append((char) take());
            } else if (peek() == 'e' || peek() == 'E') {
                // e.g. "1.e5" — empty fractional part is allowed by the grammar
                term.append('.');
                hasDot = true;
            } else {
                pushBack(); // not a decimal dot
            }
        }
        // Exponent
        int e = peek();
        if (e == 'e' || e == 'E') {
            take(); term.append((char) e); hasExp = true;
            int sign = peek();
            if (sign == '+' || sign == '-') { take(); term.append((char) sign); }
            if (!digit(peek())) throw error("Expected digits after exponent marker");
            while (digit(peek())) term.append((char) take());
        }
        String text = term.toString();
        if (text.isEmpty() || text.equals("+") || text.equals("-"))
            throw error("Invalid numeric literal: '" + text + "'");
        if (hasExp) return factory.createLiteral(text, XSD_DOUBLE);
        if (hasDot) return factory.createLiteral(text, XSD_DECIMAL);
        return factory.createLiteral(text, XSD_INTEGER);
    }

    /**
     * Reads the local-name part of a prefixed name (after the {@code :}).
     * Handles {@code %XX} percent-encoded sequences and {@code \X} backslash escapes.
     * Internal dots are included only when followed by another local-name character.
     */
    protected String readLocalPart() throws IOException {
        term.setLength(0);
        // '-' is a PN_CHARS character but NOT a valid PN_LOCAL start character
        if (peek() == '-') throw error("Local name cannot begin with '-'");
        while (true) {
            int c = peek();
            if (c == '%') {
                take(); term.append('%');
                int h1 = take(), h2 = take();
                if (hexValue(h1) < 0 || hexValue(h2) < 0) throw error("Invalid %XX encoding in local name");
                term.append((char) h1); term.append((char) h2);
            } else if (c == '\\') {
                take();
                int esc = take();
                if (!isLocalNameEscapeChar(esc)) throw error("Invalid '\\' escape in local name: '\\" + (char) esc + "'");
                term.append((char) esc);
            } else if (c == '.') {
                // Include '.' only when followed by another local-name char
                take();
                int next = peek();
                if (localNameChar(next) || next == '.' || next == '%' || next == '\\'
                        || Character.isHighSurrogate((char) next)) {
                    term.append('.');
                } else {
                    pushBack();
                    break;
                }
            } else if (localNameChar(c) || Character.isHighSurrogate((char) c)) {
                // Read full Unicode code point (handles surrogate pairs for chars > U+FFFF)
                int cp = scalar();
                if (!localNameChar(cp)) break;
                term.appendCodePoint(cp);
            } else {
                break;
            }
        }
        return term.toString();
    }


    /**
     * Reads a name token (prefix label, keyword, or {@code a}).
     * Stops at {@code :}, whitespace, or non-name characters.
     * Internal dots are included only when followed by another name character.
     */
    protected String readName() throws IOException {
        term.setLength(0);
        int first = scalar();
        if (!nameStart(first) && !digit(first))
            throw error("Expected name start character, got: '" + (char) first + "'");
        term.appendCodePoint(first);
        while (true) {
            int c = peek();
            if (nameCharacter(c) || Character.isHighSurrogate((char) c)) {
                term.appendCodePoint(scalar());
            } else if (c == '.') {
                // Include '.' only if next char is also a name char (e.g. "ex.org:foo")
                if (pushback == NO_PUSHBACK && bufOffset + 1 < bufLimit
                        && nameCharacter(buffer[bufOffset + 1])) {
                    take(); term.append('.');
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return term.toString();
    }


    /**
     * Emits a parsed triple. Override in TriG subclasses to include named graph context.
     */
    protected void emitTriple(Resource s, IRI p, Value o) {
        model.add(s, p, o);
    }


    protected IRI intern(String text) {
        IRI cached = iriCache.get(text);
        if (cached != null) return cached;
        IRI result = factory.createIRI(text);
        if (iriCache.size() == IRI_CACHE_LIMIT) iriCache.clear();
        iriCache.put(text, result);
        return result;
    }

    private int escape(boolean inLiteral) throws IOException {
        expect('\\');
        int type = take();
        if (type == 'u' || type == 'U') return unicodeEscape(type == 'u' ? 4 : 8);
        if (!inLiteral) throw error("Only Unicode escapes (\\uXXXX / \\UXXXXXXXX) are allowed in IRIs");
        return switch (type) {
            case 't'  -> '\t';
            case 'b'  -> '\b';
            case 'n'  -> '\n';
            case 'r'  -> '\r';
            case 'f'  -> '\f';
            case '"'  -> '"';
            case '\'' -> '\'';
            case '\\' -> '\\';
            default   -> throw error("Invalid literal escape sequence: '\\" + (char) type + "'");
        };
    }

    private int unicodeEscape(int digits) throws IOException {
        long result = 0;
        for (int i = 0; i < digits; i++) {
            int hex = hexValue(peek());
            if (hex < 0) throw error("Expected hexadecimal digit in Unicode escape");
            take();
            result = (result << 4) | hex;
        }
        if (result > Character.MAX_CODE_POINT || (result >= 0xD800 && result <= 0xDFFF)) {
            throw error("Unicode escape 0x" + Long.toHexString(result) + " is not a valid scalar value");
        }
        return (int) result;
    }


    /** Skips whitespace (space, tab, CR, LF) and line comments ({@code # ...}). */
    protected void ws() throws IOException {
        while (true) {
            int c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                take();
            } else if (c == '#') {
                while (peek() != -1 && !isLineEnd(peek())) take();
            } else {
                break;
            }
        }
    }

    private int scalar() throws IOException {
        int first = peek();
        if (first == -1) throw error("Unexpected end of input");
        if (Character.isLowSurrogate((char) first)) throw error("Unpaired low surrogate");
        take();
        if (!Character.isHighSurrogate((char) first)) return first;
        int second = peek();
        if (second == -1 || !Character.isLowSurrogate((char) second)) throw error("Unpaired high surrogate");
        take();
        return Character.toCodePoint((char) first, (char) second);
    }

    protected int peek() throws IOException {
        if (pushback != NO_PUSHBACK) return pushback;
        if (bufOffset == bufLimit && !eof) {
            checkInterrupted();
            int count = reader.read(buffer, 0, buffer.length);
            if (count == 0) throw new IOException("Reader returned zero characters for a nonempty buffer");
            eof       = count < 0;
            bufOffset = 0;
            bufLimit  = Math.max(count, 0);
        }
        return bufOffset == bufLimit ? -1 : buffer[bufOffset];
    }

    protected int take() throws IOException {
        int c;
        if (pushback != NO_PUSHBACK) {
            c        = pushback;
            pushback = NO_PUSHBACK;
        } else {
            c = peek();
            if (c == -1) return -1;
            bufOffset++;
        }
        updatePosition(c);
        return c;
    }

    /**
     * Pushes back a {@code '.'} character. Used exclusively for dot-in-local-name
     * and decimal-dot disambiguation. Only one character can be pushed back at a time.
     */
    protected void pushBack() {
        pushback = '.';
        if (column > 1) column--;
    }

    protected void expect(int expected) throws IOException {
        int actual = peek();
        if (actual != expected) {
            throw error("Expected '" + (char) expected + "' but got "
                    + (actual == -1 ? "end of input" : "'" + (char) actual + "'"));
        }
        take();
    }

    private void updatePosition(int c) {
        if (c == '\r') {
            line++; column = 1;
        } else if (c == '\n') {
            if (!afterCr) { line++; column = 1; }
        } else {
            column++;
        }
        afterCr = (c == '\r');
    }

    protected void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw error("Parsing interrupted");
    }

    private static boolean forbiddenIriChar(int c) {
        return c <= 0x20 || "<>\"{}|^`\\".indexOf(c) >= 0;
    }

    private static boolean isAbsoluteIri(String text) {
        if (text == null || text.isEmpty() || !isAsciiAlpha(text.charAt(0))) return false;
        int i = 1, n = text.length();
        while (i < n && isSchemeChar(text.charAt(i))) i++;
        return i < n && text.charAt(i) == ':';
    }

    protected static boolean isAsciiAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isSchemeChar(char c) {
        return isAsciiAlpha(c) || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
    }

    protected static boolean letter(int c) { return isAsciiAlpha(c); }
    protected static boolean digit(int c)  { return c >= '0' && c <= '9'; }

    private static boolean isLineEnd(int c)    { return c == '\r' || c == '\n'; }
    private static boolean isWhitespace(int c) { return c == ' ' || c == '\t' || c == '\r' || c == '\n'; }

    protected static boolean nameStart(int c) { return c == '_' || letter(c) || unicodeNameStart(c); }

    protected static boolean unicodeNameStart(int c) {
        return between(c, 0xC0, 0xD6)     || between(c, 0xD8, 0xF6)
            || between(c, 0xF8, 0x2FF)    || between(c, 0x370, 0x37D)
            || between(c, 0x37F, 0x1FFF)  || between(c, 0x200C, 0x200D)
            || between(c, 0x2070, 0x218F) || between(c, 0x2C00, 0x2FEF)
            || between(c, 0x3001, 0xD7FF) || between(c, 0xF900, 0xFDCF)
            || between(c, 0xFDF0, 0xFFFD) || between(c, 0x10000, 0xEFFFF);
    }

    protected static boolean nameCharacter(int c) {
        return nameStart(c) || digit(c) || c == '-' || c == 0xB7
            || between(c, 0x300, 0x36F) || between(c, 0x203F, 0x2040);
    }

    private static boolean localNameChar(int c) {
        return nameCharacter(c) || c == ':';
    }

    private static boolean isLocalNameEscapeChar(int c) {
        return "_~.-!$&'()*+,;=/?#@%".indexOf(c) >= 0;
    }

    private static int hexValue(int c) {
        if (digit(c)) return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return (c >= 'A' && c <= 'F') ? c - 'A' + 10 : -1;
    }

    private static boolean between(int c, int lo, int hi) { return c >= lo && c <= hi; }


    protected ParsingException error(String message) {
        return new ParsingException(location(message));
    }

    protected String location(String message) {
        return message + " at line " + line + ", column " + column;
    }
}
