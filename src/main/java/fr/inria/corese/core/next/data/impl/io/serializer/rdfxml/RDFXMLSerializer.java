package fr.inria.corese.core.next.data.impl.io.serializer.rdfxml;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.io.serializer.option.BlankNodeIdGenerationOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.DatatypePolicyOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.LineEndingOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.PrettyPrintOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.UsesPrefixOptions;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import fr.inria.corese.core.next.data.api.io.serializer.option.PrefixOrdering;
import fr.inria.corese.core.next.data.impl.io.serializer.support.SerializationConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

/**
 * Serializes a {@link Model} to RDF/XML format.
 * This class provides a method to write the statements of a model to a {@link Writer}
 * in accordance with the RDF/XML specification, considering configuration options.
 *
 * <p>This implementation handles:</p>
 * <ul>
 * <li>Declaration and usage of XML namespaces for IRIs.</li>
 * <li>Basic pretty-printing (indentation).</li>
 * <li>Serialization of triples as rdf:Description elements with properties.</li>
 * <li>Serialization of blank nodes using rdf:nodeID identifiers.</li>
 * <li>Serialization of literals with language tags or datatypes.</li>
 * </ul>
 * <p>Advanced features such as handling XML schemata, specific RDF/XML graph structures (e.g., rdf:Bag, rdf:Seq, rdf:Alt),
 * and full blank node syntax optimization are simplified in this version.</p>
 */
public class RDFXMLSerializer implements RDFSerializer {

    private static final Set<String> RESERVED_RDF_PROPERTIES = Set.of(
            "RDF", "Description", "ID", "about", "parseType", "resource", "nodeID", "datatype",
            "aboutEach", "aboutEachPrefix", "bagID", "li");

    private final Map<String, QName> propertyNames = new HashMap<>();
    private final Document nameValidator = newNameValidator();
    private final Model model;
    private final IOOptions config;
    private final PrefixHandler prefixHandler;
    private final Map<Resource, String> blankNodeIds;
    private final Set<String> usedBlankNodeIds = new HashSet<>();
    private int blankNodeCounter = 0;
    private List<Statement> cachedStatements;

    /**
     * Constructs a new {@code RDFXMLSerializer} instance with the specified model and default configuration.
     * The default configuration is obtained from {@link RDFXMLSerializerOptions#defaultConfig()}.
     *
     * @param model the {@link Model} to serialize. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public RDFXMLSerializer(Model model) {
        this(model, RDFXMLSerializerOptions.defaultConfig());
    }

    /**
     * Constructs a new {@code RDFXMLSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param config the {@link RDFXMLSerializerOptions} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    public RDFXMLSerializer(Model model, IOOptions config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");

        this.prefixHandler = new PrefixHandler(false);
        model.getNamespaces().forEach(ns -> registerPrefix(ns.getPrefix(), ns.getNamespace()));
        if (config instanceof UsesPrefixOptions options && options.usePrefixes()) {
            options.getPrefixMapping().getPrefixMap().forEach(this::registerPrefix);
        }
        // RDF/XML syntax and XML namespace bindings cannot be overridden by data/configuration.
        this.prefixHandler.setPrefix(RDF.getVocabularyPreferredPrefix(), RDF.getVocabularyNamespace());
        this.prefixHandler.setPrefix(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        this.blankNodeIds = new HashMap<>();
    }

    private boolean isAllowedXmlPrefix(String prefix) {
        return prefix != null && !prefix.isEmpty()
                && !"xml".equalsIgnoreCase(prefix)
                && !"xmlns".equalsIgnoreCase(prefix)
                && isXmlName(prefix);
    }

    private static Document newNameValidator() {
        try {
            return DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot initialize XML name validation", e);
        }
    }

    private boolean isXmlName(String name) {
        if (name == null || name.isEmpty()) return false;
        try {
            // Stay compatible with runtimes implementing an older edition of XML 1.0.
            nameValidator.createElementNS("urn:corese:xml-name", "p:" + name);
            return true;
        } catch (DOMException e) {
            return false;
        }
    }

    private void registerPrefix(String prefix, String namespace) {
        if (isAllowedXmlPrefix(prefix) && !namespace.isEmpty()
                && !XMLConstants.XML_NS_URI.equals(namespace)
                && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
            prefixHandler.setPrefix(prefix, namespace);
        }
    }

    /**
     * Writes the model to the given writer in RDF/XML format.
     *
     * @param writer the {@link Writer} to which the RDF/XML output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        Objects.requireNonNull(writer, "writer");
        Writer bufferedWriter = new BufferedWriter(writer);
        try {
            this.cachedStatements = model.stream().toList();
            propertyNames.clear();
            cachedStatements.stream().map(statement -> statement.getPredicate().stringValue())
                    .distinct().sorted().forEach(iri -> propertyNames.put(iri, resolvePropertyName(iri)));

            writeXmlDeclaration(bufferedWriter);
            writeRdfRootElement(bufferedWriter);
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new SerializationException("Failed to write RDF/XML output", "RDF/XML", e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid data for RDF/XML format: " + e.getMessage(), "RDF/XML", e);
        }
    }

    @Override
    public RDFFormat getFormat() {
        return RDFFormat.RDFXML;
    }

    /**
     * Writes the XML declaration at the beginning of the document.
     *
     * @param writer the {@link Writer} to which the declaration will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeXmlDeclaration(Writer writer) throws IOException {
        writer.write(SerializationConstants.XML_DECLARATION_START);
        if(this.config instanceof LineEndingOptions lineEndingOptions) {
            writer.write(lineEndingOptions.getLineEnding());
        }
    }

    /**
     * Writes the root `<rdf:RDF>` element and its contents.
     * This includes namespace declarations and all statements.
     *
     * @param writer the {@link Writer} to which the root element will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeRdfRootElement(Writer writer) throws IOException {
        Set<String> actuallyUsedNamespaces = new HashSet<>();
        actuallyUsedNamespaces.add(RDF.getVocabularyNamespace());
        propertyNames.values().stream().filter(name -> !name.getPrefix().isEmpty())
                .map(QName::getNamespaceURI).forEach(actuallyUsedNamespaces::add);
        if (this.config instanceof UsesPrefixOptions usesPrefixOptions
                &&  usesPrefixOptions.usePrefixes()
                && usesPrefixOptions.autoDeclarePrefixes()) {
            actuallyUsedNamespaces.addAll(collectUsedNamespaces());
        }

        writer.write(SerializationConstants.RDF_ROOT_START);
        writeNamespaceAttributes(writer, actuallyUsedNamespaces);
        writer.write(">");
        if(this.config instanceof LineEndingOptions lineEndingOptions) {
            writer.write(lineEndingOptions.getLineEnding());
        }

        Map<Resource, List<Statement>> statementsBySubject = cachedStatements.stream()
                .collect(Collectors.groupingBy(Statement::getSubject));


        List<Resource> sortedSubjects = new ArrayList<>(statementsBySubject.keySet());
        if (this.config instanceof PrettyPrintOptions prettyPrintOptions
                && prettyPrintOptions.sortSubjects()) {
            Collections.sort(sortedSubjects, Comparator.comparing(Value::stringValue));
        }

        String zeroIndent = "";
        for (Resource subject : sortedSubjects) {
            if(this.config instanceof PrettyPrintOptions prettyPrintOptions) {
                writeDescriptionElement(writer, subject, statementsBySubject.get(subject), prettyPrintOptions.getIndent());
            } else {
                writeDescriptionElement(writer, subject, statementsBySubject.get(subject), zeroIndent);
            }
        }

        writer.write(SerializationConstants.RDF_ROOT_END);
        if(this.config instanceof LineEndingOptions lineEndingOptions) {
            writer.write(lineEndingOptions.getLineEnding());
        }
    }

    /**
     * Writes the namespace attributes (`xmlns:prefix="uri"`) for the `<rdf:RDF>` element.
     *
     * @param writer the {@link Writer} to which attributes will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeNamespaceAttributes(Writer writer, Set<String> actuallyUsedNamespaces) throws IOException {
        ArrayList<String> namespaces = new ArrayList<>(actuallyUsedNamespaces);
        namespaces.sort(Comparator.naturalOrder());

        if(this.config instanceof UsesPrefixOptions usesPrefixOptions
        && usesPrefixOptions.autoDeclarePrefixes()) {

            namespaces.forEach(namespace -> {
                if (! this.prefixHandler.hasNamespace(namespace)) {
                    String prefix = getSuggestedPrefix(namespace);
                    if (prefix != null) {
                        this.prefixHandler.setPrefix(prefix, namespace);
                    }
                }
            });
        }

        if (this.config instanceof PrettyPrintOptions prettyPrintOptions
                && prettyPrintOptions.getPrefixOrdering() == PrefixOrdering.ALPHABETICAL) {
            namespaces.sort(
                    (ns1, ns2) ->
                        prefixHandler.getPrefix(ns1).compareTo(prefixHandler.getPrefix(ns2))
            );
        }

        for(String namespace : namespaces) {
            String prefix = this.prefixHandler.getPrefix(namespace);
            writer.write(" " + SerializationConstants.XMLNS_PREFIX + prefix + "=\"" + escapeXmlAttribute(namespace) + "\"");
        }
    }

    /**
     * Collects namespaces from model IRIs when automatic prefix discovery is enabled.
     */
    private Set<String> collectUsedNamespaces() {
        // Collecting namespaces of all IRIs in the data
        return this.cachedStatements.stream()
                .flatMap(stmt -> {
                    List<Value> values = new ArrayList<>(Arrays.asList(
                            stmt.getSubject(),
                            stmt.getPredicate(),
                            stmt.getObject()
                    ));
                    if (stmt.getContext() != null) {
                        values.add(stmt.getContext());
                    }
                    if (stmt.getObject().isLiteral()
                            && ((Literal) stmt.getObject()).getDatatype() != null) {
                        values.add(((Literal) stmt.getObject()).getDatatype());
                    }
                    return values.stream();
                })
                .filter(Objects::nonNull)
                .filter(Value::isIRI)
                .map(v -> IRIUtils.guessNamespace(v.stringValue()))
                .filter(namespace -> !namespace.isEmpty() && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace))
                .collect(Collectors.toSet());
    }


    /** Resolves a predicate to an XML name without changing its RDF IRI. */
    private QName resolvePropertyName(String iri) {
        if (iri.startsWith(RDF.getVocabularyNamespace())
                && RESERVED_RDF_PROPERTIES.contains(iri.substring(RDF.getVocabularyNamespace().length()))) {
            throw new IllegalArgumentException("Reserved RDF/XML syntax cannot be used as a property: " + iri);
        }
        if (config instanceof UsesPrefixOptions options && options.usePrefixes()) {
            String namespace = prefixHandler.getNamespaces().stream()
                    .filter(iri::startsWith)
                    .filter(ns -> isXmlName(iri.substring(ns.length())))
                    .max(Comparator.comparingInt(String::length)).orElse(null);
            if (namespace != null) {
                return new QName(namespace, iri.substring(namespace.length()), prefixHandler.getPrefix(namespace));
            }
        }
        String namespace = IRIUtils.guessNamespace(iri);
        String localName = iri.substring(namespace.length());
        if (!isXmlName(localName) || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
            int split = propertyNameSplit(iri);
            namespace = iri.substring(0, split);
            localName = iri.substring(split);
        }
        if (namespace.isEmpty() || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
            throw new IllegalArgumentException("Invalid namespace for RDF/XML property: " + iri);
        }
        String prefix = "";
        if (XMLConstants.XML_NS_URI.equals(namespace)) {
            prefix = XMLConstants.XML_NS_PREFIX;
        } else if (config instanceof UsesPrefixOptions options
                && options.usePrefixes() && options.autoDeclarePrefixes()) {
            prefix = getSuggestedPrefix(namespace);
            prefixHandler.setPrefix(prefix, namespace);
        }
        return new QName(namespace, localName, prefix);
    }

    /** Finds an XML-compatible NCName suffix while keeping a nonempty namespace. */
    private int propertyNameSplit(String iri) {
        int split = Math.max(iri.lastIndexOf(':') + 1,
                Math.max(iri.lastIndexOf('/') + 1, iri.lastIndexOf('#') + 1));
        while (split < iri.length() && (!isXmlName(iri.substring(split))
                || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(iri.substring(0, split)))) {
            split += Character.charCount(iri.codePointAt(split));
        }
        if (split == 0 || split == iri.length()) {
            throw new IllegalArgumentException("Predicate IRI cannot be represented as an RDF/XML property name: " + iri);
        }
        return split;
    }

    /**
     * Writes an `<rdf:Description>` element for a given subject.
     * This element contains all properties (predicates and objects) for that subject.
     *
     * @param writer        the {@link Writer} to which the element will be written.
     * @param subject       the {@link Resource} representing the subject.
     * @param statements    the list of statements with this subject.
     * @param currentIndent the current indentation string.
     * @throws IOException if an I/O error occurs.
     */
    private void writeDescriptionElement(Writer writer, Resource subject, List<Statement> statements, String currentIndent) throws IOException {
        String nextIndent = currentIndent;
        if(this.config instanceof PrettyPrintOptions prettyPrintOptions) {
            nextIndent = currentIndent + prettyPrintOptions.getIndent();
        }

        writer.write(currentIndent);
        if (subject.isIRI()) {
            writer.write(SerializationConstants.RDF_DESCRIPTION_START + " " + SerializationConstants.RDF_ABOUT_ATTRIBUTE + "=\"" + escapeXmlAttribute(subject.stringValue()) + "\">");
        } else if (subject.isBNode()) {
            writer.write(SerializationConstants.RDF_DESCRIPTION_START + " " + SerializationConstants.RDF_NODEID_ATTRIBUTE + "=\"" + getBlankNodeId(subject) + "\">");
        }
        if(this.config instanceof LineEndingOptions lineEndingOptions) {
            writer.write(lineEndingOptions.getLineEnding());
        }

        Map<IRI, List<Statement>> statementsByPredicate = statements.stream()
                .collect(Collectors.groupingBy(Statement::getPredicate));

        List<IRI> sortedPredicates = new ArrayList<>(statementsByPredicate.keySet());
        if (this.config instanceof PrettyPrintOptions prettyPrintOptions && prettyPrintOptions.sortPredicates()) {
            Collections.sort(sortedPredicates, Comparator.comparing(Value::stringValue));
        }

        for (IRI predicate : sortedPredicates) {
            for (Statement stmt : statementsByPredicate.get(predicate)) {
                writePropertyElement(writer, stmt.getPredicate(), stmt.getObject(), nextIndent);
            }
        }

        writer.write(currentIndent);
        writer.write(SerializationConstants.RDF_DESCRIPTION_END);
        if(this.config instanceof LineEndingOptions lineEndingOptions) {
            writer.write(lineEndingOptions.getLineEnding());
        }
    }

    /**
     * Writes a property element (e.g., `<ex:propertyName>objectValue</ex:propertyName>`) for a triple.
     *
     * @param writer        the {@link Writer} to which the element will be written.
     * @param predicate     the {@link IRI} representing the predicate.
     * @param object        the {@link Value} representing the object.
     * @param currentIndent the current indentation string.
     * @throws IOException if an I/O error occurs.
     */
    private void writePropertyElement(Writer writer, IRI predicate, Value object, String currentIndent) throws IOException {
        QName name = propertyNames.get(predicate.stringValue());
        String elementName = name.getPrefix().isEmpty() ? name.getLocalPart()
                : name.getPrefix() + SerializationConstants.COLON + name.getLocalPart();

        writer.write(currentIndent);
        writer.write("<" + elementName);
        if (name.getPrefix().isEmpty()) {
            writer.write(" xmlns=\"" + escapeXmlAttribute(name.getNamespaceURI()) + "\"");
        }

        if (object.isIRI()) {
            writer.write(" " + SerializationConstants.RDF_RESOURCE_ATTRIBUTE + "=\"" + escapeXmlAttribute(object.stringValue()) + "\"/>");
        } else if (object.isBNode()) {
            writer.write(" " + SerializationConstants.RDF_NODEID_ATTRIBUTE + "=\"" + getBlankNodeId((Resource) object) + "\"/>");
        } else if (object.isLiteral()) {
            writeLiteralElement(writer, (Literal) object, elementName);
        } else {
            throw new IllegalArgumentException("Unsupported value type for RDF/XML serialization: " + object.getClass().getName());
        }
        if (this.config instanceof LineEndingOptions lineEndingOptions) {
            writer.write(lineEndingOptions.getLineEnding());
        }
    }

    private void writeLiteralElement(Writer writer, Literal literal, String elementName) throws IOException {
        writeLiteralAttributes(writer, literal);
        writer.write(escapeXmlContent(literal.stringValue()));
        writer.write("</" + elementName + ">");
    }

    private void writeLiteralAttributes(Writer writer, Literal literal) throws IOException {
        if (literal.getLanguage().isPresent()) {
            writer.write(" " + SerializationConstants.XML_LANG_ATTRIBUTE + "=\""
                    + escapeXmlAttribute(literal.getLanguage().orElseThrow()) + "\">");
        } else if (shouldWriteDatatype(literal)) {
            String datatypeUri = literal.getDatatype().stringValue();
            writer.write(" " + SerializationConstants.RDF_DATATYPE_ATTRIBUTE + "=\"" + escapeXmlAttribute(datatypeUri) + "\">");
        } else {
            writer.write(">");
        }
    }

    /**
     * Retrieves or generates a stable blank node ID.
     *
     * @param bNode the blank node.
     * @return a unique XML-compatible ID for the blank node.
     */
    private String getBlankNodeId(Resource bNode) {
        return blankNodeIds.computeIfAbsent(bNode, this::assignBlankNodeId);
    }

    private String assignBlankNodeId(Resource node) {
        if (!(config instanceof BlankNodeIdGenerationOptions options && options.stableBlankNodeIds())) {
            String label = node.stringValue();
            String candidate = label.startsWith("_:") ? label.substring(2) : label;
            if (isXmlName(candidate) && usedBlankNodeIds.add(candidate)) {
                return candidate;
            }
        }
        String candidate;
        do {
            candidate = "b" + blankNodeCounter++;
        } while (!usedBlankNodeIds.add(candidate));
        return candidate;
    }

    /**
     * Determines if a literal's datatype should be written based on the configuration.
     *
     * @param literal the {@link Literal} to check.
     * @return {@code true} if the datatype should be written, {@code false} otherwise.
     */
    private boolean shouldWriteDatatype(Literal literal) {
        if (literal.getLanguage().isPresent()) {
            return false;
        }

        IRI datatype = literal.getDatatype();
        if (datatype == null) {
            return false;
        }

        return config instanceof DatatypePolicyOptions datatypePolicyOptions
                && (datatypePolicyOptions.getLiteralDatatypePolicy() == LiteralDatatypePolicy.ALWAYS_TYPED
                    || (!datatype.equals(XSD.xsdString.getIRI())
                        && datatypePolicyOptions.getLiteralDatatypePolicy() == LiteralDatatypePolicy.MINIMAL));
    }

    /**
     * Suggests a prefix for a given namespace URI.
     * Attempts to derive a meaningful prefix or generates a unique one.
     *
     * @param namespace The namespace URI.
     * @return A suggested prefix, or null if suggestion is not possible.
     */
    private String getSuggestedPrefix(String namespace) {
        String base = namespace;
        if (base.endsWith(SerializationConstants.HASH) || base.endsWith(SerializationConstants.SLASH)) {
            base = base.substring(0, base.length() - 1);
        }
        int lastSlash = base.lastIndexOf(SerializationConstants.SLASH);
        int lastHash = base.lastIndexOf(SerializationConstants.HASH);
        int lastSegmentStart = Math.max(lastSlash, lastHash);
        if (lastSegmentStart != -1) {
            base = base.substring(lastSegmentStart + 1);
        }

        if (base.isEmpty()) {
            try {
                java.net.URI uri = new java.net.URI(namespace);
                base = uri.getHost();
                if (base != null) {
                    base = base.replace(SerializationConstants.POINT, SerializationConstants.EMPTY_STRING);
                } else {
                    base = "p";
                }
            } catch (java.net.URISyntaxException e) {
                base = "p";
            }
        }

        base = base.replaceAll("[^a-zA-Z0-9]", SerializationConstants.EMPTY_STRING).toLowerCase(Locale.ROOT);
        if (!isAllowedXmlPrefix(base)) base = "p";

        String candidate = base;
        int i = 0;
        while (this.prefixHandler.hasPrefix(candidate) && !namespace.equals(this.prefixHandler.getNamespace(candidate))) {
            candidate = base + (++i);
        }
        return candidate;
    }

    /**
     * Escapes a string for use as an XML attribute value.
     * Replaces characters like '&', '<', '>', '"', "'" with their XML entity equivalents.
     *
     * @param value The string to escape.
     * @return The escaped string.
     */
    private String escapeXmlAttribute(String value) {
        return escapeXml(value, true);
    }

    private String escapeXmlContent(String value) {
        return escapeXml(value, false);
    }

    /** Escapes XML markup and preserves whitespace that XML parsers would normalize. */
    private String escapeXml(String value, boolean attribute) {
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> appendXmlCharacter(result, cp, attribute));
        return result.toString();
    }

    private static void appendXmlCharacter(StringBuilder result, int cp, boolean attribute) {
        if (!isXmlCharacter(cp)) {
            throw new IllegalArgumentException("Character U+" + Integer.toHexString(cp) + " cannot be represented in XML 1.0");
        }
        switch (cp) {
            case '&' -> result.append(SerializationConstants.AMP_ENTITY);
            case '<' -> result.append(SerializationConstants.LT_ENTITY);
            case '>' -> result.append(SerializationConstants.GT_ENTITY);
            case '"' -> result.append(attribute ? SerializationConstants.QUOT_ENTITY : "\"");
            case '\'' -> result.append(attribute ? SerializationConstants.APOS_ENTITY : "'");
            case '\r' -> result.append("&#13;");
            case '\n' -> result.append(attribute ? "&#10;" : "\n");
            case '\t' -> result.append(attribute ? "&#9;" : "\t");
            default -> result.appendCodePoint(cp);
        }
    }

    private static boolean isXmlCharacter(int cp) {
        return cp == '\t' || cp == '\n' || cp == '\r'
                || (cp >= 0x20 && cp <= 0xD7FF)
                || (cp >= 0xE000 && cp <= 0xFFFD)
                || (cp >= 0x10000 && cp <= Character.MAX_CODE_POINT);
    }
}
