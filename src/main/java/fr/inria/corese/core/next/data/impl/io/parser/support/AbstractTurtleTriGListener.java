package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import fr.inria.corese.core.next.data.api.support.term.IRIUtils;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.common.text.RdfText;

import static fr.inria.corese.core.next.data.api.support.term.IRIUtils.isAbsoluteIRI;
import static fr.inria.corese.core.next.data.api.support.term.IRIUtils.normalizeURI;

/**
 * Base class for RDF parsers (Turtle, TriG) providing common functionality.
 * Implements IRI resolution according to RFC 3986, Unicode escape handling,
 * prefix management, and RDF term creation.
 *
 */
public abstract class AbstractTurtleTriGListener {

    public final Model model;
    public final ValueFactory factory;
    public final PrefixHandler prefixHandler;

    public String baseURI;

    public Resource currentSubject;
    public IRI currentPredicate;

    /**
     * Constructs a parser listener with the specified model, factory and base URI.
     *
     * @param model   RDF model to populate with parsed statements
     * @param factory factory for creating RDF terms (IRIs, blank nodes, literals)
     * @param baseURI base URI for resolving relative IRI references
     */
    protected AbstractTurtleTriGListener(Model model, ValueFactory factory, String baseURI) {
        this.model = model;
        this.factory = factory;
        this.baseURI = baseURI;
        this.prefixHandler = new PrefixHandler(true);

        initializeBasePrefix();
    }

    /**
     * Registers the base URI as the empty prefix namespace.
     */
    public void initializeBasePrefix() {
    }

    /**
     * Extracts IRI from angle brackets and processes Unicode escape sequences.
     *
     * @param text raw IRI text including angle brackets
     * @return unescaped IRI string
     * @throws ParsingException if the IRI contains invalid characters after escape processing
     */
    public String extractAndUnescapeIRI(String text) {
        String iri = text.substring(1, text.length() - 1);
        return unescapeIRI(iri);
    }

    /**
     * Updates the base URI and registers it in prefix mappings.
     *
     * @param newBase new base URI to set
     */
    public void updateBaseURI(String newBase) {
        this.baseURI = resolveIRIAgainstBase(newBase);
        validateIRI(this.baseURI);
    }

    /**
     * Registers a namespace prefix with its corresponding IRI.
     *
     * @param prefix namespace prefix
     * @param iri    namespace IRI
     */
    public void registerPrefix(String prefix, String iri) {
        String resolvedIRI = resolveIRIAgainstBase(iri);
        validateIRI(resolvedIRI);
        prefixHandler.setPrefix(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Resolves an IRI reference to absolute form.
     * Handles prefixed names (QNames), relative IRIs, and absolute IRIs.
     *
     * @param raw raw IRI string
     * @return resolved absolute IRI
     * @throws ParsingException if the IRI cannot be resolved
     */
    @SuppressWarnings("java:S3776")
    public String resolveIRI(String raw) {
        try {

            raw = raw.trim();

            if (raw.equals(ParserConstants.RDF_TYPE_SHORTCUT)) {
                return RDF.type.getIRI().stringValue();
            }

            if (raw.equals(ParserConstants.COLON)) {
                String ns = prefixHandler.getNamespace(ParserConstants.EMPTY_STRING);
                if (ns == null) {
                    throw new ParsingException(
                            "Undeclared prefix: '' (empty prefix). " +
                                    "Use '@prefix : <namespace> .' to declare it.");
                }
                return ns;
            }

            if (raw.startsWith(ParserConstants.IRI_START) && raw.endsWith(ParserConstants.IRI_END)) {
                String iri = raw.substring(1, raw.length() - 1);
                iri = unescapeIRI(iri);
                validateIRI(iri);
                return iri.isEmpty() ? getEffectiveBaseURI() : resolveIRIAgainstBase(iri);
            }

            if (raw.contains(ParserConstants.COLON)) {
                String[] parts = raw.split(ParserConstants.COLON, 2);
                String prefix = parts[0];
                String localName = parts[1];

                if (prefixHandler.hasPrefix(prefix)) {
                    localName = unescapeIRI(localName);
                    String ns = prefixHandler.getNamespace(prefix);
                    if (ns != null) {
                        String result = ns + localName;
                        validateIRI(result);
                        return result;
                    }
                } else if (isAbsoluteIRI(raw)) {
                    return raw;
                } else {
                    throw new ParsingException("Undeclared prefix: " + prefix);
                }
            }

            return resolveIRIAgainstBase(raw);

        } catch (ParsingException e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    /**
     * Resolves a relative IRI reference against the base URI using RFC 3986 algorithm.
     *
     * @param iri IRI reference to resolve
     * @return resolved absolute IRI
     */
    public String resolveIRIAgainstBase(String iri) {
        String effectiveBase = getEffectiveBaseURI();

        if (isAbsoluteIRI(iri)) {
            return iri;
        }
        return IRIUtils.resolveIRIAgainstBase(effectiveBase, iri);
    }

    /**
     * Returns the effective base URI, using a default if none is set.
     *
     * @return effective base URI
     */
    public String getEffectiveBaseURI() {
        String effective = (baseURI != null && !baseURI.isEmpty()) ? baseURI : ParserConstants.getDefaultBaseURI();
        return normalizeURI(effective);
    }

    /**
     * Processes Unicode escape sequences in IRIs.
     *
     * @param rawIri IRI string potentially containing escape sequences
     * @return unescaped IRI string
     * @throws IllegalArgumentException if escape sequences are malformed or contain surrogates
     */
    @SuppressWarnings({"java:S3776", "java:S127"})
    public String unescapeIRI(String rawIri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawIri.length(); i++) {
            char c = rawIri.charAt(i);
            if (c == '\\' && i + 1 < rawIri.length()) {
                char next = rawIri.charAt(i + 1);
                if (next == 'u' || next == 'U') {
                    int len = (next == 'u') ? 4 : 8;
                    if (i + len + 1 <= rawIri.length()) {
                        String hex = rawIri.substring(i + 2, i + 2 + len);
                        int codePoint = Integer.parseInt(hex, 16);
                        if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                            throw new IllegalArgumentException("Surrogates not allowed: \\u" + hex);
                        }
                        sb.appendCodePoint(codePoint);
                        i += len + 1;
                    } else {
                        throw new IllegalArgumentException("Incomplete Unicode escape");
                    }
                } else {
                    sb.append(next);
                    i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Processes escape sequences in RDF string literals.
     * Handles standard escapes (\t, \n, \r, etc.) and Unicode escapes.
     *
     * @param text literal string including delimiters (quotes or triple-quotes)
     * @return unescaped string content
     * @throws IllegalArgumentException if escape sequences are malformed
     */
    @SuppressWarnings({"java:S3776", "java:S127"})
    public String unescapeString(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }

        boolean isMultiline = text.startsWith(ParserConstants.TRIPLE_QUOTE) || text.startsWith(ParserConstants.TRIPLE_APOSTROPHE);
        String content = isMultiline ? text.substring(3, text.length() - 3) : text.substring(1, text.length() - 1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(i + 1);
                switch (next) {
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
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
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\'':
                        sb.append('\'');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'u', 'U':
                        int len = (next == 'u') ? 4 : 8;
                        if (i + len + 1 <= content.length()) {
                            String hex = content.substring(i + 2, i + 2 + len);
                            int codePoint = Integer.parseInt(hex, 16);
                            if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                                throw new IllegalArgumentException("Invalid Unicode escape: Surrogate code points not allowed");
                            }
                            sb.appendCodePoint(codePoint);
                            i += len + 1;
                        } else {
                            throw new IllegalArgumentException("Incomplete Unicode escape sequence");
                        }
                        break;
                    default:
                        sb.append(c).append(next);
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
     * Adds an RDF statement to the model with exception handling.
     * Subclasses may override to support named graphs or other extensions.
     *
     * @param subject   statement subject
     * @param predicate statement predicate
     * @param object    statement object
     * @throws ParsingException if the statement cannot be added
     */
    public void safeAddStatement(Resource subject, IRI predicate, Value object) {
        try {
            model.add(subject, predicate, object);
        } catch (Exception e) {
            throw new ParsingException("Failed to add statement: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an RDF literal with optional language tag or datatype.
     *
     * @param label       literal lexical value
     * @param langTag     language tag (may be null)
     * @param datatypeIRI datatype IRI (may be null)
     * @return RDF literal
     */
    public Literal createLiteral(String label, String langTag, String datatypeIRI) {
        if (langTag != null) {
            return factory.createLiteral(label, langTag);
        }
        if (datatypeIRI != null) {
            return factory.createLiteral(label, factory.createIRI(datatypeIRI));
        }
        return factory.createLiteral(label);
    }

    /**
     * Creates a boolean literal.
     *
     * @param text boolean value as string ("true" or "false")
     * @return boolean literal with xsd:boolean datatype
     */
    public Literal createBooleanLiteral(String text) {
        return factory.createLiteral(text, XSDDatatype.BOOLEAN.getIRI());
    }

    /**
     * Creates a numeric literal with appropriate XSD datatype.
     *
     * @param text numeric value as string
     * @param type numeric type (INTEGER, DECIMAL, or DOUBLE)
     * @return numeric literal with corresponding XSD datatype
     */
    public Literal createNumericLiteral(String text, NumericType type) {
        return switch (type) {
            case DOUBLE -> factory.createLiteral(text, XSDDatatype.DOUBLE.getIRI());
            case DECIMAL -> factory.createLiteral(text, XSDDatatype.DECIMAL.getIRI());
            default -> factory.createLiteral(text, XSDDatatype.INTEGER.getIRI());
        };
    }

    /**
     * Validates that an IRI contains only valid characters after escape sequence processing.
     *
     * @param iri the IRI string to validate (after escape sequences have been processed)
     * @throws ParsingException if the IRI contains forbidden characters
     */
    private void validateIRI(String iri) throws ParsingException {
        if (iri == null || iri.isEmpty()) {
            return;
        }

        // Check each character in the IRI
        for (int i = 0; i < iri.length(); i++) {
            char c = iri.charAt(i);

            // Check for forbidden characters
            if (IRIUtils.isInvalidIRICharacter(c)) {
                String codePoint = String.format("U+%04X", (int) c);
                String charDesc = RdfText.describeCharacter(c);
                String displayIRI = RdfText.escapeForDisplay(iri);

                throw new ParsingException(
                        "Invalid character in IRI: " + codePoint + " (" + charDesc + ") " +
                                "at position " + i + ". " +
                                "IRI after escape processing: " + displayIRI + ". " +
                                "IRIs cannot contain space, control characters, or reserved characters."
                );
            }
        }
    }

    /**
     * Enumeration of numeric literal types corresponding to XSD datatypes.
     */
    public enum NumericType {
        /**
         * XSD integer type
         */
        INTEGER,
        /**
         * XSD decimal type
         */
        DECIMAL,
        /**
         * XSD double type
         */
        DOUBLE
    }
}
