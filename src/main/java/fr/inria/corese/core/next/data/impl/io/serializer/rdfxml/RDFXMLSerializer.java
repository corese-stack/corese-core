package fr.inria.corese.core.next.data.impl.io.serializer.rdfxml;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.io.serializer.*;
import fr.inria.corese.core.next.data.api.io.serializer.option.*;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import fr.inria.corese.core.next.data.api.support.term.IRIUtils;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import fr.inria.corese.core.next.data.api.io.serializer.option.PrefixOrdering;
import fr.inria.corese.core.next.data.impl.io.serializer.support.SerializationConstants;

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
public class RDFXMLSerializer implements RDFSerializer {

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
            this.prefixHandler = new PrefixHandler(false);
            this.prefixHandler.copyFrom(usesPrefixOptions.getPrefixMapping());
        } else {
            this.prefixHandler = new PrefixHandler(false);
            // These namespaces are part of the RDF/XML standard
            this.prefixHandler.setPrefix(RDF.getVocabularyPreferredPrefix(), RDF.getVocabularyNamespace());
            this.prefixHandler.setPrefix(XSD.getVocabularyPreferredPrefix(), XSD.getVocabularyNamespace());
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
        ArrayList<String> namespacelist = new ArrayList<>(actuallyUsedNamespaces);

        if(this.config instanceof UsesPrefixOptions usesPrefixOptions
        && usesPrefixOptions.autoDeclarePrefixes()) {

            namespacelist.forEach(namespace -> {
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
            namespacelist.sort(
                    (ns1, ns2) ->
                        prefixHandler.getPrefix(ns1).compareTo(prefixHandler.getPrefix(ns2))
            );
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
        String longestMatchingNamespace = null;
        String correspondingPrefix = null;
        int longestMatchLength = -1;

        for (String namespace : this.prefixHandler.getNamespaces()) {
            if (iriString.startsWith(namespace)) {
                String prefix = this.prefixHandler.getPrefix(namespace);
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
