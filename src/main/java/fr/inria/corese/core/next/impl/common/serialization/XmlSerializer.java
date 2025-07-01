package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.SerializerConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

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
public class XmlSerializer implements RdfSerializer {

    private static final Logger logger = LoggerFactory.getLogger(XmlSerializer.class);

    private final Model model;
    private final SerializerConfig config;
    private final Map<String, String> iriToPrefixMapping;
    private final Map<String, String> prefixToIriMapping;
    private final Map<Resource, String> blankNodeIds;
    private int blankNodeCounter = 0;
    private List<Statement> cachedStatements;

    /**
     * Constructs a new {@code XmlSerializer} instance with the specified model and default configuration.
     * The default configuration is returned by {@link SerializerConfig#rdfXmlConfig()}.
     *
     * @param model the {@link Model} to serialize. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public XmlSerializer(Model model) {
        this(model, SerializerConfig.rdfXmlConfig());
    }

    /**
     * Constructs a new {@code XmlSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param config the {@link SerializationConfig} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    public XmlSerializer(Model model, SerializationConfig config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = (SerializerConfig) Objects.requireNonNull(config, "Configuration cannot be null");
        this.iriToPrefixMapping = new HashMap<>();
        this.prefixToIriMapping = new HashMap<>();
        this.blankNodeIds = new HashMap<>();
        initializePrefixes();
    }

    /**
     * Initializes prefix mappings by adding custom prefixes from the configuration.
     * The custom prefixes map in SerializerConfig is expected to be {namespaceURI: prefix}.
     */
    private void initializePrefixes() {
        if (config.usePrefixes()) {
            for (Map.Entry<String, String> entry : config.getCustomPrefixes().entrySet()) {
                addPrefixMapping(entry.getKey(), entry.getValue());
            }
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

    /**
     * Writes the XML declaration at the beginning of the document.
     *
     * @param writer the {@link Writer} to which the declaration will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeXmlDeclaration(Writer writer) throws IOException {
        writer.write(SerializationConstants.XML_DECLARATION_START);
        writer.write(config.getLineEnding());
    }

    /**
     * Writes the root `<rdf:RDF>` element and its contents.
     * This includes namespace declarations and all statements.
     *
     * @param writer the {@link Writer} to which the root element will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeRdfRootElement(Writer writer) throws IOException {
        if (config.usePrefixes() && config.autoDeclarePrefixes()) {
            collectUsedNamespaces();
        }

        writer.write(SerializationConstants.RDF_ROOT_START);
        writeNamespaceAttributes(writer);
        writer.write(">");
        writer.write(config.getLineEnding());

        Map<Resource, List<Statement>> statementsBySubject = cachedStatements.stream()
                .collect(Collectors.groupingBy(Statement::getSubject));


        List<Resource> sortedSubjects = new ArrayList<>(statementsBySubject.keySet());
        if (config.sortSubjects()) {
            Collections.sort(sortedSubjects, Comparator.comparing(Value::stringValue));
        }

        for (Resource subject : sortedSubjects) {
            writeDescriptionElement(writer, subject, statementsBySubject.get(subject), config.getIndent());
        }

        writer.write(SerializationConstants.RDF_ROOT_END);
        writer.write(config.getLineEnding());
    }

    /**
     * Writes the namespace attributes (`xmlns:prefix="uri"`) for the `<rdf:RDF>` element.
     *
     * @param writer the {@link Writer} to which attributes will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeNamespaceAttributes(Writer writer) throws IOException {
        if (!iriToPrefixMapping.containsKey(SerializationConstants.RDF_NS)) {
            addPrefixMapping(SerializationConstants.RDF_NS, "rdf");
        }

        List<String> prefixes = new ArrayList<>(prefixToIriMapping.keySet());
        if (config.getPrefixOrdering() == PrefixOrderingEnum.ALPHABETICAL) {
            Collections.sort(prefixes);
        }

        for (String prefix : prefixes) {
            String namespaceURI = prefixToIriMapping.get(prefix);
            writer.write(String.format(" %s%s=\"%s\"", SerializationConstants.XMLNS_PREFIX, prefix, escapeXmlAttribute(namespaceURI)));
        }
    }

    /**
     * Collects all namespaces used in the model (subjects, predicates, objects, contexts)
     * and attempts to assign prefixes if auto-declaration is enabled and they are not already mapped.
     */
    private void collectUsedNamespaces() {
        Set<String> namespaces = this.cachedStatements.stream()
                .flatMap(stmt -> Arrays.asList(
                        stmt.getSubject(),
                        stmt.getPredicate(),
                        stmt.getObject()
                ).stream())
                .filter(Value::isIRI)
                .map(v -> getNamespace(v.stringValue()))
                .collect(Collectors.toSet());


        namespaces.forEach(namespace -> {
            if (!iriToPrefixMapping.containsKey(namespace)) {
                String prefix = getSuggestedPrefix(namespace);
                if (prefix != null) {
                    addPrefixMapping(namespace, prefix);
                }
            }
        });
    }


    /**
     * Retrieves the prefixed name for a given IRI string.
     * This method now prioritizes the longest matching namespace to ensure correct prefix application.
     *
     * @param iriString The full IRI.
     * @return The prefixed name (e.g., "foaf:name") or null if no suitable prefix is found.
     */
    private String getPrefixedNameInternal(String iriString) {
        String longestMatchingNamespace = null;
        String correspondingPrefix = null;
        int longestMatchLength = -1;

        for (Map.Entry<String, String> entry : iriToPrefixMapping.entrySet()) {
            String namespace = entry.getKey();
            String prefix = entry.getValue();

            if (iriString.startsWith(namespace)) {
                if (namespace.length() > longestMatchLength) {
                    longestMatchLength = namespace.length();
                    longestMatchingNamespace = namespace;
                    correspondingPrefix = prefix;
                }
            }
        }

        if (longestMatchingNamespace != null) {
            String localName = iriString.substring(longestMatchingNamespace.length());

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
        if (iriToPrefixMapping.containsKey(namespaceURI)) {
            if (iriToPrefixMapping.get(namespaceURI).equals(prefix)) {
                return;
            } else {

                if (logger.isWarnEnabled()) {
                    logger.warn("Namespace URI '{}' is already mapped to prefix '{}'. Cannot map to new prefix '{}'. " +
                                    "Existing mapping for this namespace will be retained.",
                            namespaceURI, iriToPrefixMapping.get(namespaceURI), prefix);
                }
                return;
            }
        }

        String effectivePrefix = prefix;
        if (prefixToIriMapping.containsKey(prefix)) {
            if (!prefixToIriMapping.get(prefix).equals(namespaceURI)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Prefix '{}' is already mapped to namespace '{}'. Cannot map to new namespace '{}'. " +
                                    "A new unique prefix will be generated for '{}'.",
                            prefix, prefixToIriMapping.get(prefix), namespaceURI, namespaceURI);
                }
                effectivePrefix = generateUniquePrefix(prefix);
            }
        }

        iriToPrefixMapping.put(namespaceURI, effectivePrefix);
        prefixToIriMapping.put(effectivePrefix, namespaceURI);
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
        while (prefixToIriMapping.containsKey(candidate)) {
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
        String nextIndent = currentIndent + config.getIndent();

        writer.write(currentIndent);
        if (subject.isIRI()) {
            writer.write(String.format("%s %s=\"%s\">", SerializationConstants.RDF_DESCRIPTION_START, SerializationConstants.RDF_ABOUT_ATTRIBUTE, escapeXmlAttribute(subject.stringValue())));
        } else if (subject.isBNode()) {
            writer.write(String.format("%s %s=\"%s\">", SerializationConstants.RDF_DESCRIPTION_START, SerializationConstants.RDF_NODEID_ATTRIBUTE, getBlankNodeId(subject)));
        }
        writer.write(config.getLineEnding());

        Map<IRI, List<Statement>> statementsByPredicate = statements.stream()
                .collect(Collectors.groupingBy(Statement::getPredicate));

        List<IRI> sortedPredicates = new ArrayList<>(statementsByPredicate.keySet());
        if (config.sortPredicates()) {
            Collections.sort(sortedPredicates, Comparator.comparing(Value::stringValue));
        }

        for (IRI predicate : sortedPredicates) {
            for (Statement stmt : statementsByPredicate.get(predicate)) {
                writePropertyElement(writer, stmt.getPredicate(), stmt.getObject(), nextIndent);
            }
        }

        writer.write(currentIndent);
        writer.write(SerializationConstants.RDF_DESCRIPTION_END);
        writer.write(config.getLineEnding());
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
            writer.write(config.getLineEnding());
        } else if (object.isBNode()) {
            writer.write(String.format(" %s=\"%s\"/>", SerializationConstants.RDF_NODEID_ATTRIBUTE, getBlankNodeId((Resource) object)));
            writer.write(config.getLineEnding());
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
            writer.write(config.getLineEnding());
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
            if (config.stableBlankNodeIds()) {
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

        return config.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.ALWAYS_TYPED ||
                (!datatype.stringValue().equals(SerializationConstants.XSD_STRING) &&
                        config.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.MINIMAL);
    }


    /**
     * Extracts the namespace URI part from an IRI string.
     * This is a common heuristic for RDF IRIs.
     *
     * @param iriString The full IRI.
     * @return The namespace URI part.
     */
    private String getNamespace(String iriString) {
        int hashIdx = iriString.lastIndexOf(SerializationConstants.HASH);
        int slashIdx = iriString.lastIndexOf(SerializationConstants.SLASH);

        if (hashIdx > -1) {
            return iriString.substring(0, hashIdx + 1);
        } else if (slashIdx > -1 && slashIdx < iriString.length() - 1) {
            int dotIdx = iriString.lastIndexOf(SerializationConstants.POINT);
            if (dotIdx > slashIdx) {
                return iriString.substring(0, slashIdx + 1);
            }
            return iriString.substring(0, slashIdx + 1);
        }
        return iriString;
    }

    /**
     * Suggests a prefix for a given namespace URI.
     * Attempts to derive a meaningful prefix or generates a unique one.
     *
     * @param namespace The namespace URI.
     * @return A suggested prefix, or null if suggestion is not possible.
     */
    private String getSuggestedPrefix(String namespace) {

        if (namespace.equals(SerializationConstants.RDF_NS)) return "rdf";
        if (namespace.equals(SerializationConstants.RDFS_NS)) return "rdfs";
        if (namespace.equals(SerializationConstants.XSD_NS)) return "xsd";
        if (namespace.equals(SerializationConstants.OWL_NS)) return "owl";
        if (namespace.equals(SerializationConstants.FOAF_NS)) return "foaf";


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
        while (prefixToIriMapping.containsKey(candidate) && !prefixToIriMapping.get(candidate).equals(namespace)) {
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
