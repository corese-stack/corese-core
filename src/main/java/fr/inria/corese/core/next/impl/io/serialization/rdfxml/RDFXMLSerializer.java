package fr.inria.corese.core.next.impl.io.serialization.rdfxml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.serializer.*;
import fr.inria.corese.core.next.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.common.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.option.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

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
 * <li>Serialization of blank nodes using rdf:nodeID or nested elements.</li>
 * <li>Serialization of literals with language tags or datatypes.</li>
 * </ul>
 * <p>Advanced features such as handling XML schemata, specific RDF/XML graph structures (e.g., rdf:Bag, rdf:Seq, rdf:Alt),
 * and full blank node syntax optimization are simplified in this version.</p>
 */
public class RDFXMLSerializer implements RDFSerializer {

    private static final Logger logger = LoggerFactory.getLogger(RDFXMLSerializer.class);

    private final Model model;
    private final IOOptions config;
    private final PrefixHandler prefixHandler;
    private final Map<Resource, String> blankNodeIds;
    private int blankNodeCounter = 0;
    private List<Statement> cachedStatements;

    /**
     * Constructs a new {@code XmlSerializer} instance with the specified model and default configuration.
     * The default configuration is obtained from {@link RDFXMLSerializerOptions#defaultConfig()}.
     *
     * @param model the {@link Model} to serialize. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public RDFXMLSerializer(Model model) {
        this(model, RDFXMLSerializerOptions.defaultConfig());
    }

    /**
     * Constructs a new {@code XmlSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param config the {@link RDFXMLSerializerOptions} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    public RDFXMLSerializer(Model model, IOOptions config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");

        if(config instanceof UsesPrefixOptions usesPrefixOptions
                && usesPrefixOptions.usePrefixes()) {
            this.prefixHandler = usesPrefixOptions.getPrefixHandler();
        } else {
            this.prefixHandler = new PrefixHandler(false);
            this.prefixHandler.setPrefix(RDF.getVocabularyPreferredPrefix(), RDF.getVocabularyNamespace());
        }
        this.blankNodeIds = new HashMap<>();
    }

    /**
     * Writes the model to the given writer in RDF/XML format.
     *
     * @param writer the {@link Writer} to which the RDF/XML output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        try (Writer bufferedWriter = new BufferedWriter(writer)) {
            this.cachedStatements = model.stream().toList();

            writeXmlDeclaration(bufferedWriter);
            writeRdfRootElement(bufferedWriter);
        } catch (IOException e) {
            throw new SerializationException("Failed to write RDF/XML output", "RDF/XML", e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid data for RDF/XML format: " + e.getMessage(), "RDF/XML", e);
        }
    }

    @Override
    public RDFFormat getRDFFormat() {
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
        logger.info("actually used Namespaces: {}", actuallyUsedNamespaces);
        ArrayList<String> namespacelist = new ArrayList<>(actuallyUsedNamespaces);
        if (this.config instanceof PrettyPrintOptions prettyPrintOptions
                && prettyPrintOptions.getPrefixOrdering() == PrefixOrderingEnum.ALPHABETICAL) {
            namespacelist.sort(
                    (ns1, ns2) ->
                            prefixHandler.getPrefix(ns1).compareTo(prefixHandler.getPrefix(ns2)));
        }

        for(String namespace : namespacelist) {
            String prefix = this.prefixHandler.getPrefix(namespace);
            writer.write(String.format(" %s%s=\"%s\"", SerializationConstants.XMLNS_PREFIX, prefix, escapeXmlAttribute(namespace)));
        }
    }

    /**
     * Collects all namespaces used in the model (subjects, predicates, objects, contexts)
     * and attempts to assign prefixes if auto-declaration is enabled and they are not already mapped.
     */
    private Set<String> collectUsedNamespaces() {
        // Collecting namespaces of all IRIs in the data
        Set<String> potentialNamespaces = this.cachedStatements.stream()
                .flatMap(stmt -> {
                    List<Value> values = new ArrayList<>(Arrays.asList(
                            stmt.getSubject(),
                            stmt.getPredicate(),
                            stmt.getObject()
                    ));
                    if (stmt.getContext() != null) {
                        values.add(stmt.getContext());
                    }
                    if(stmt.getObject().isLiteral()
                            && ((Literal) stmt.getObject()).getDatatype() != null) {
                        values.add(((Literal) stmt.getObject()).getDatatype());
                    }
                    return values.stream();
                })
                .filter(Objects::nonNull)
                .filter(Value::isIRI)
                .map(v -> IRIUtils.guessNamespace(v.stringValue()))
                .collect(Collectors.toSet());
        logger.info("{}", potentialNamespaces);

        // potential namespaces can contain different candidates that are based on each other. We keep the shortest
        Set<String> copyPotentialNamespaces = Set.copyOf(potentialNamespaces);
        potentialNamespaces = potentialNamespaces
                .stream()
                .filter(potentialNamespace -> copyPotentialNamespaces
                        .stream()
                        .noneMatch(otherPotentialNamespace -> (! otherPotentialNamespace.equals(potentialNamespace)) && potentialNamespace.startsWith(otherPotentialNamespace)))
                .collect(Collectors.toSet());
        logger.info("{}", potentialNamespaces);

        potentialNamespaces.forEach(namespace -> {
            logger.info("{} is in PrefixHandler = {}", namespace, this.prefixHandler.hasNamespace(namespace));
            if (! this.prefixHandler.hasNamespace(namespace) &&
                    // removing known namespaces from the list of potential namespaces
                    this.prefixHandler.getNamespaces()
                            .stream()
                            .noneMatch(knownNamespace -> (knownNamespace.startsWith(namespace)))) {
                String prefix = getSuggestedPrefix(namespace);
                if (prefix != null) {
                    addPrefixMapping(namespace, prefix);
                }
            }
        });

        return potentialNamespaces;
    }


    /**
     * Retrieves the prefixed name for a given IRI string.
     * This method now prioritizes the longest matching namespace to ensure correct prefix application.
     *
     * @param iriString The full IRI.
     * @return The prefixed name (e.g., "foaf:name") or null if no suitable prefix is found.
     */
    private String getPrefixedNameInternal(String iriString) {
        logger.info("getPrefixedNameInternal({})", iriString);
        String longestMatchingNamespace = null;
        String correspondingPrefix = null;
        int longestMatchLength = -1;

        logger.info("{}", this.prefixHandler.getPrefixMap());
        for (String namespace : this.prefixHandler.getNamespaces()) {
            logger.info("{} contains {} ? {}", iriString, namespace, iriString.startsWith(namespace));
            if (iriString.startsWith(namespace)) {
                String prefix = this.prefixHandler.getPrefix(namespace);
                logger.info("{} = {}", namespace, prefix);
                if (namespace.length() > longestMatchLength) {
                    longestMatchLength = namespace.length();
                    longestMatchingNamespace = namespace;
                    correspondingPrefix = prefix;
                }
            }
        }

        if (longestMatchingNamespace != null) {
            String localName = iriString.substring(longestMatchingNamespace.length());
            logger.info("{} = {} : {}", iriString, longestMatchingNamespace, localName);
            if (localName.isEmpty()) {
                return correspondingPrefix + SerializationConstants.COLON;
            }
            return correspondingPrefix + SerializationConstants.COLON + localName;
        }
        return null;
    }

    /**
     * Adds a prefix-namespace URI mapping to the internal mappings.
     * Handles potential conflicts to ensure uniqueness.
     *
     * @param namespaceURI The namespace URI.
     * @param prefix       The associated prefix.
     */
    private void addPrefixMapping(String namespaceURI, String prefix) {
        if (this.prefixHandler.hasNamespace(namespaceURI)) {
            if (this.prefixHandler.getPrefix(namespaceURI).equals(prefix)) {
                return;
            } else {

                if (logger.isWarnEnabled()) {
                    logger.warn("Namespace URI '{}' is already mapped to prefix '{}'. Cannot map to new prefix '{}'. " +
                                    "Existing mapping for this namespace will be retained.",
                            namespaceURI, this.prefixHandler.getPrefix(namespaceURI), prefix);
                }
                return;
            }
        }

        String effectivePrefix = prefix;
        if (this.prefixHandler.hasPrefix(prefix)) {
            if (! this.prefixHandler.getNamespace(prefix).equals(namespaceURI)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Prefix '{}' is already mapped to namespace '{}'. Cannot map to new namespace '{}'. " +
                                    "A new unique prefix will be generated for '{}'.",
                            prefix, this.prefixHandler.getNamespace(prefix), namespaceURI, namespaceURI);
                }
                effectivePrefix = generateUniquePrefix(prefix);
            }
        }

        this.prefixHandler.setPrefix(effectivePrefix, namespaceURI);
    }

    /**
     * Generates a unique prefix based on a given base string, ensuring it's not already in use.
     * This method appends numbers to the base prefix until a unique one is found.
     *
     * @param basePrefix The desired base prefix (e.g., "foaf").
     * @return A unique prefix (e.g., "foaf", "foaf1", "foaf2").
     */
    private String generateUniquePrefix(String basePrefix) {
        String candidate = basePrefix;
        int i = 0;
        while (this.prefixHandler.hasPrefix(candidate)) {
            candidate = basePrefix + (++i);
        }
        return candidate;
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
            writer.write(String.format("%s %s=\"%s\">", SerializationConstants.RDF_DESCRIPTION_START, SerializationConstants.RDF_ABOUT_ATTRIBUTE, escapeXmlAttribute(subject.stringValue())));
        } else if (subject.isBNode()) {
            writer.write(String.format("%s %s=\"%s\">", SerializationConstants.RDF_DESCRIPTION_START, SerializationConstants.RDF_NODEID_ATTRIBUTE, getBlankNodeId(subject)));
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
        String predicateString = predicate.stringValue();
        String prefixedPredicateName = getPrefixedNameInternal(predicateString);
        String elementName;

        if (prefixedPredicateName != null && !prefixedPredicateName.endsWith(SerializationConstants.COLON)) {
            elementName = prefixedPredicateName;
        } else {
            elementName = predicateString;
            logger.warn("Predicate IRI '{}' cannot be expressed as a valid prefixed element name. Using full IRI as element name in RDF/XML.", predicateString);
        }

        writer.write(currentIndent);
        writer.write(String.format("<%s", elementName));

        if (object.isIRI()) {
            writer.write(String.format(" %s=\"%s\"/>", SerializationConstants.RDF_RESOURCE_ATTRIBUTE, escapeXmlAttribute(object.stringValue())));
            if(this.config instanceof LineEndingOptions lineEndingOptions) {
                writer.write(lineEndingOptions.getLineEnding());
            }
        } else if (object.isBNode()) {
            writer.write(String.format(" %s=\"%s\"/>", SerializationConstants.RDF_NODEID_ATTRIBUTE, getBlankNodeId((Resource) object)));
            if(this.config instanceof LineEndingOptions lineEndingOptions) {
                writer.write(lineEndingOptions.getLineEnding());
            }
        } else if (object.isLiteral()) {
            Literal literal = (Literal) object;

            literal.getLanguage().ifPresent(lang -> {
                try {
                    writer.write(String.format(" %s=\"%s\">", SerializationConstants.XML_LANG_ATTRIBUTE, escapeXmlAttribute(lang)));
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to write xml:lang attribute", e);
                }
            });

            if (!literal.getLanguage().isPresent() && shouldWriteDatatype(literal)) {
                String datatypeUri = literal.getDatatype().stringValue();
                String prefixedDatatype = getPrefixedNameInternal(datatypeUri);
                writer.write(String.format(" %s=\"%s\">", SerializationConstants.RDF_DATATYPE_ATTRIBUTE, escapeXmlAttribute(prefixedDatatype != null ? prefixedDatatype : datatypeUri)));
            } else if (!literal.getLanguage().isPresent()) {
                writer.write(">");
            }

            writer.write(escapeXmlContent(literal.stringValue()));

            writer.write(String.format("</%s>", elementName));
            if(this.config instanceof LineEndingOptions lineEndingOptions) {
                writer.write(lineEndingOptions.getLineEnding());
            }
        } else {
            throw new IllegalArgumentException("Unsupported value type for RDF/XML serialization: " + object.getClass().getName());
        }
    }

    /**
     * Retrieves or generates a stable blank node ID.
     *
     * @param bNode the blank node.
     * @return a stable ID for the blank node.
     */
    private String getBlankNodeId(Resource bNode) {
        return blankNodeIds.computeIfAbsent(bNode, k -> {
            if (this.config instanceof BlankNodeIdGenerationOptions bnGenOptions && bnGenOptions.stableBlankNodeIds()) {
                return "b" + (blankNodeCounter++);
            } else {
                return bNode.stringValue().substring(2);
            }
        });
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
                && (datatypePolicyOptions.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.ALWAYS_TYPED
                    || (!datatype.equals(XSD.xsdString.getIRI())
                        && datatypePolicyOptions.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.MINIMAL));
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
                logger.warn("Malformed URI encountered while suggesting prefix: {}", namespace, e);
                base = "p";
            }
        }

        base = base.replaceAll("[^a-zA-Z0-9]", SerializationConstants.EMPTY_STRING).toLowerCase();
        if (base.isEmpty()) base = "p";

        String candidate = base;
        int i = 0;
        while (this.prefixHandler.hasPrefix(candidate) && !this.prefixHandler.getPrefix(candidate).equals(namespace)) {
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append(SerializationConstants.AMP_ENTITY);
                    break;
                case '<':
                    sb.append(SerializationConstants.LT_ENTITY);
                    break;
                case '>':
                    sb.append(SerializationConstants.GT_ENTITY);
                    break;
                case '"':
                    sb.append(SerializationConstants.QUOT_ENTITY);
                    break;
                case '\'':
                    sb.append(SerializationConstants.APOS_ENTITY);
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a string for use as XML element content.
     * Replaces characters like '&', '<', '>' with their XML entity equivalents.
     *
     * @param value The string to escape.
     * @return The escaped string.
     */
    private String escapeXmlContent(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append(SerializationConstants.AMP_ENTITY);
                    break;
                case '<':
                    sb.append(SerializationConstants.LT_ENTITY);
                    break;
                case '>':
                    sb.append(SerializationConstants.GT_ENTITY);
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }


}
