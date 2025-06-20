package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.serialization.config.BlankNodeStyleEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serializes a {@link Model} to Turtle format with comprehensive syntax support.
 * This class provides a method to write the declarations of a model to a {@link Writer}
 * in accordance with the Turtle specification, taking into account configuration options.
 *
 * <p>This implementation handles:</p>
 * <ul>
 * <li>Declaration and usage of prefixes for IRIs, including auto-declaration and sorting.</li>
 * <li>The 'a' shortcut for 'rdf:type'.</li>
 * <li>Escaping of special characters in literals (single-line and multi-line) and IRIs.</li>
 * <li>Basic pretty-printing (indentation, end-of-line dots).</li>
 * <li>Management of literal datatype policies (minimal or always typed).</li>
 * <li>Serialization of compact triples (semicolons, commas) to group subjects and predicates.</li>
 * <li>Serialization of nested blank nodes using the '[]' syntax.</li>
 * <li>Serialization of RDF collections (lists) using the '()' syntax.</li>
 * <li>Detection and prevention of infinite loops during serialization of nested blank nodes and lists.</li>
 * <li>Sorting of subjects and predicates if configured.</li>
 * </ul>
 * <p>Advanced features such as strict adherence to maximum line length
 * and generation of stable blank node identifiers are not fully implemented in this version.</p>
 */
public class TurtleSerializer implements IRdfSerializer {

    /**
     * Logger for this class, used to log potential issues or information during serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(TurtleSerializer.class);

    private final Model model;
    private final FormatConfig config;
    private final Map<String, String> iriToPrefixMapping;
    private final Map<String, String> prefixToIriMapping;
    // Set to track blank nodes already serialized inline or as part of a list
    private final Set<Resource> consumedBlankNodes;
    // Set to track blank nodes currently being serialized to detect cycles
    private final Set<Resource> currentlyWritingBlankNodes;

    /**
     * Constructs a new {@code TurtleSerializer} instance with the specified model and default configuration.
     * The default configuration is returned by {@link FormatConfig#turtleConfig()}.
     *
     * @param model the {@link Model} to serialize. Must not be null.
     * @throws NullPointerException if the provided model is null.
     */
    public TurtleSerializer(Model model) {
        this(model, FormatConfig.turtleConfig());
    }

    /**
     * Constructs a new {@code TurtleSerializer} instance with the specified model and custom configuration.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param config the {@link ISerializationConfig} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    public TurtleSerializer(Model model, ISerializationConfig config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.config = (FormatConfig) Objects.requireNonNull(config, "Configuration cannot be null");
        this.iriToPrefixMapping = new HashMap<>();
        this.prefixToIriMapping = new HashMap<>();
        this.consumedBlankNodes = new HashSet<>();
        this.currentlyWritingBlankNodes = new HashSet<>();
        initializePrefixes();
    }

    /**
     * Initializes prefix mappings by adding custom prefixes from the configuration.
     */
    private void initializePrefixes() {
        if (config.usePrefixes()) {
            for (Map.Entry<String, String> entry : config.getCustomPrefixes().entrySet()) {
                addPrefixMapping(entry.getValue(), entry.getKey());
            }
        }
    }

    /**
     * Writes the model to the given writer in Turtle format.
     *
     * @param writer the {@link Writer} to which the Turtle output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        try (Writer bufferedWriter = new BufferedWriter(writer)) {
            writeHeader(bufferedWriter);

            Set<Resource> precomputedInlineBlankNodes = precomputeInlineBlankNodesAndLists();
            consumedBlankNodes.addAll(precomputedInlineBlankNodes);

            if (config.useCompactTriples() && config.groupBySubject()) {
                writeOptimizedStatements(bufferedWriter);
            } else {
                writeSimpleStatements(bufferedWriter);
            }

        } catch (IOException e) {
            throw new SerializationException("Failed to write Turtle output", "Turtle", e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid data for Turtle format: " + e.getMessage(), "Turtle", e);
        }
    }

    /**
     * Writes the Turtle document header, including base IRI declaration and prefixes.
     *
     * @param writer the {@link Writer} to which the header will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeHeader(Writer writer) throws IOException {
        if (config.getBaseIRI() != null) {
            writer.write(String.format("@base <%s> .%s",
                    config.getBaseIRI(),
                    config.getLineEnding()));
        }

        if (config.usePrefixes() && config.autoDeclarePrefixes()) {
            collectUsedNamespaces();
        }

        writePrefixDeclarations(writer);
    }

    /**
     * Collects all namespaces used in the model and attempts to assign prefixes to them
     * if auto-declaration is enabled and they are not already mapped.
     */
    private void collectUsedNamespaces() {
        Set<String> namespaces = model.stream()
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
     * Writes prefix declarations to the writer, sorted if configured.
     *
     * @param writer the {@link Writer} to which prefixes will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writePrefixDeclarations(Writer writer) throws IOException {
        List<String> prefixes = new ArrayList<>(prefixToIriMapping.keySet());

        if (config.getPrefixOrdering() == PrefixOrderingEnum.ALPHABETICAL) {
            Collections.sort(prefixes);
        }

        for (String prefix : prefixes) {
            writer.write(String.format("@prefix %s: <%s> .%s",
                    prefix,
                    prefixToIriMapping.get(prefix),
                    config.getLineEnding()));
        }

        if (!prefixes.isEmpty() || config.getBaseIRI() != null) {
            writer.write(config.getLineEnding());
        }
    }

    /**
     * Serializes the model's statements in a simple manner, one per line, without grouping.
     * Triples already "consumed" by inline serialization are ignored.
     *
     * @param writer the {@link Writer} to which the statements will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeSimpleStatements(Writer writer) throws IOException {
        for (Statement stmt : model) {
            if (!isConsumed(stmt.getSubject())) {
                writeStatement(writer, stmt);
                writer.write(config.getLineEnding());
            }
        }
    }

    /**
     * Writes a single {@link Statement} to the writer in Turtle format.
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt   the {@link Statement} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeStatement(Writer writer, Statement stmt) throws IOException {
        String indent = config.prettyPrint() ? config.getIndent() : SerializationConstants.EMPTY_STRING;
        writer.write(indent);

        // Subject
        writeValue(writer, stmt.getSubject());
        writer.write(SerializationConstants.SPACE);

        // Predicate
        writePredicate(writer, stmt.getPredicate());
        writer.write(SerializationConstants.SPACE);

        // Object
        writeValue(writer, stmt.getObject());

        // Trailing dot
        if (config.trailingDot()) {
            writer.write(SerializationConstants.SPACE);
            writer.write(SerializationConstants.POINT);
        }

        logContextWarning(stmt);
    }

    /**
     * Writes the predicate to the writer, using the 'a' shortcut if configured and applicable.
     *
     * @param writer    the {@link Writer} to which the predicate will be written.
     * @param predicate the {@link Value} representing the predicate.
     * @throws IOException if an I/O error occurs.
     */
    private void writePredicate(Writer writer, Value predicate) throws IOException {
        if (config.useRdfTypeShortcut() && predicate.stringValue().equals(SerializationConstants.RDF_TYPE)) {
            writer.write(SerializationConstants.RDF_TYPE_SHORTCUT);
        } else {
            writeValue(writer, predicate);
        }
    }

    /**
     * Writes a single {@link Value} to the writer.
     * Handles literals, blank nodes, and IRIs.
     * This is the entry point for serializing nested blank nodes and lists.
     *
     * @param writer the {@link Writer} to which the value will be written.
     * @param value  the {@link Value} to write.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the provided value is null or of an unsupported type.
     */
    private void writeValue(Writer writer, Value value) throws IOException {
        validateValue(value);

        if (value.isIRI()) {
            writeIRI(writer, (IRI) value);
        } else if (value.isLiteral()) {
            writeLiteral(writer, (Literal) value);
        } else if (value.isBNode()) {
            Resource bNode = (Resource) value;

            if (currentlyWritingBlankNodes.contains(bNode)) {
                writer.write(SerializationConstants.BNODE_PREFIX + bNode.stringValue());
                return;
            }

            currentlyWritingBlankNodes.add(bNode);

            boolean handled = false;
            if (config.useCollections() && bNode.isBNode()) {
                handled = writeRDFList(writer, bNode);
            }

            if (!handled && config.getBlankNodeStyle() == BlankNodeStyleEnum.ANONYMOUS && bNode.isBNode()) {
                List<Statement> properties = model.stream()
                        .filter(stmt -> stmt.getSubject().equals(bNode))
                        .toList();

                if (!properties.isEmpty()) {
                    writeInlineBlankNode(writer, properties);
                    handled = true;
                }
            }

            if (!handled) {
                writer.write(SerializationConstants.BNODE_PREFIX + bNode.stringValue());
            }

            currentlyWritingBlankNodes.remove(bNode);
        } else {
            throw new IllegalArgumentException("Unsupported value type for Turtle serialization: " + value.getClass().getName());
        }
    }


    /**
     * Writes an {@link IRI} to the writer.
     * Attempts to use a prefixed name if possible, otherwise writes the full IRI in angle brackets.
     *
     * @param writer the {@link Writer} to which the IRI will be written.
     * @param iri    the {@link IRI} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeIRI(Writer writer, IRI iri) throws IOException {
        if (config.isStrictMode() && config.validateURIs()) {
            validateIRI(iri);
        }

        String prefixed = config.usePrefixes() ? getPrefixedName(iri.stringValue()) : null;

        if (prefixed != null) {
            writer.write(prefixed);
        } else {
            writer.write(String.format("<%s>", escapeTurtleIRI(iri.stringValue())));
        }
    }

    /**
     * Writes a {@link Literal} to the writer in Turtle format.
     * Applies escaping and datatype/language tag rules based on configuration.
     *
     * @param writer  the {@link Writer} to which the literal will be written.
     * @param literal the {@link Literal} to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeLiteral(Writer writer, Literal literal) throws IOException {
        String value = literal.stringValue();

        if (config.shouldUseTripleQuotes(value)) {
            writer.write(String.format("\"\"\"%s\"\"\"", escapeMultilineLiteral(value)));
        } else {
            writer.write(String.format("\"%s\"", escapeTurtleLiteral(value)));
        }

        literal.getLanguage().ifPresent(lang -> {
            try {
                writer.write(SerializationConstants.AT_SIGN + lang);
            } catch (IOException e) {
                throw new UncheckedIOException("Error writing language tag to stream", e);
            }
        });

        writeDatatype(writer, literal);
    }

    /**
     * Writes the datatype of a literal if the configured datatype policy allows it.
     *
     * @param writer  the {@link Writer} to which the datatype will be written.
     * @param literal the {@link Literal} whose datatype is to be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeDatatype(Writer writer, Literal literal) throws IOException {
        IRI datatype = literal.getDatatype();
        if (shouldWriteDatatype(literal)) {
            writer.write(SerializationConstants.DATATYPE_SEPARATOR);
            writeIRI(writer, datatype);
        }
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
     * Writes an inline blank node using the `[]` syntax.
     * The blank node's properties are serialized inside the brackets.
     *
     * @param writer     the {@link Writer} to which the blank node will be written.
     * @param properties the list of statements where the blank node is the subject.
     * @throws IOException if an I/O error occurs.
     */
    private void writeInlineBlankNode(Writer writer, List<Statement> properties) throws IOException {
        String currentIndent = config.prettyPrint() ? config.getIndent() : SerializationConstants.EMPTY_STRING;
        String propIndent = config.prettyPrint() ? currentIndent + config.getIndent() : SerializationConstants.EMPTY_STRING;

        writer.write(SerializationConstants.BLANK_NODE_START);

        boolean firstProperty = true;
        for (Statement stmt : properties) {
            if (stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_FIRST) ||
                    stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_REST)) {
                continue;
            }

            if (!firstProperty) {
                writer.write(SerializationConstants.SEMICOLON);
            }
            firstProperty = false;

            if (config.prettyPrint()) {
                writer.write(config.getLineEnding() + propIndent);
            } else {
                writer.write(SerializationConstants.SPACE);
            }

            writePredicate(writer, stmt.getPredicate());
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, stmt.getObject());
        }

        if (config.prettyPrint() && !properties.isEmpty() && !firstProperty) {
            writer.write(config.getLineEnding() + currentIndent);
        }

        writer.write(SerializationConstants.BLANK_NODE_END);
    }

    /**
     * Serializes the model's statements by grouping triples by subject, then by predicate,
     * using compact syntax (semicolons and commas) if configured.
     * Triples already "consumed" by inline serialization are ignored.
     *
     * @param writer the {@link Writer} to which the optimized statements will be written.
     * @throws IOException if an I/O error occurs.
     */
    private void writeOptimizedStatements(Writer writer) throws IOException {
        // Collect and group statements by subject
        Map<Resource, List<Statement>> bySubject = config.sortSubjects() ?
                new TreeMap<>(Comparator.comparing(Resource::stringValue)) :
                new LinkedHashMap<>();


        model.stream()
                .filter(stmt -> !isConsumed(stmt.getSubject()))
                .forEach(stmt -> bySubject.computeIfAbsent(stmt.getSubject(), k -> new ArrayList<>()).add(stmt));

        for (Map.Entry<Resource, List<Statement>> subjectEntry : bySubject.entrySet()) {
            String indent = config.prettyPrint() ? config.getIndent() : SerializationConstants.EMPTY_STRING;
            writer.write(indent);
            writeValue(writer, subjectEntry.getKey());
            writer.write(SerializationConstants.SPACE);

            // Group statements of the current subject by predicate
            Map<IRI, List<Statement>> byPredicate = config.sortPredicates() ?
                    new TreeMap<>(Comparator.comparing(IRI::stringValue)) :
                    new LinkedHashMap<>();

            subjectEntry.getValue().forEach(stmt -> byPredicate.computeIfAbsent(stmt.getPredicate(), k -> new ArrayList<>()).add(stmt));

            boolean firstPredicate = true;
            for (Map.Entry<IRI, List<Statement>> predicateEntry : byPredicate.entrySet()) {
                if (!firstPredicate) {
                    writer.write(SerializationConstants.SEMICOLON);
                    if (config.prettyPrint()) {
                        writer.write(config.getLineEnding() + indent + config.getIndent());
                    } else {
                        writer.write(SerializationConstants.SPACE);
                    }
                }
                firstPredicate = false;

                writePredicate(writer, predicateEntry.getKey());
                writer.write(SerializationConstants.SPACE);

                boolean firstObject = true;
                for (Statement stmt : predicateEntry.getValue()) {
                    if (!firstObject) {
                        writer.write(SerializationConstants.COMMA);
                        if (config.prettyPrint()) {
                            writer.write(config.getLineEnding() + indent + config.getIndent() + config.getIndent());
                        } else {
                            writer.write(SerializationConstants.SPACE);
                        }
                    }
                    firstObject = false;

                    writeValue(writer, stmt.getObject());
                }
            }

            writer.write(SerializationConstants.SPACE + SerializationConstants.POINT);
            writer.write(config.getLineEnding());
        }
    }

    /**
     * Attempts to serialize an RDF list if the given blank node is its head.
     * Marks all blank nodes in the list as consumed.
     *
     * @param writer   the {@link Writer} to which the list will be written.
     * @param listHead the blank node that might be the head of an RDF list.
     * @return {@code true} if an RDF list was serialized, {@code false} otherwise.
     * @throws IOException if an I/O error occurs.
     */
    private boolean writeRDFList(Writer writer, Resource listHead) throws IOException {
        List<Value> items = new ArrayList<>();
        Resource current = listHead;
        Set<Resource> listBlankNodes = new HashSet<>();

        if (currentlyWritingBlankNodes.contains(listHead)) {
            return false;
        }
        currentlyWritingBlankNodes.add(listHead);

        while (current != null && current.isBNode() && !currentlyWritingBlankNodes.contains(current)) {
            listBlankNodes.add(current);
            currentlyWritingBlankNodes.add(current);

            final Resource finalCurrentForLambda = current;
            List<Statement> statements = model.stream()
                    .filter(stmt -> stmt.getSubject().equals(finalCurrentForLambda))
                    .toList();

            if (statements.size() != 2) {
                current = null;
                break;
            }

            Optional<Value> first = statements.stream()
                    .filter(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_FIRST))
                    .map(Statement::getObject)
                    .findFirst();

            Optional<Value> rest = statements.stream()
                    .filter(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_REST))
                    .map(Statement::getObject)
                    .findFirst();

            if (!first.isPresent() || !rest.isPresent()) {
                current = null;
                break;
            }

            items.add(first.get());

            if (rest.get().stringValue().equals(SerializationConstants.RDF_NIL)) {
                current = null; // End of the list
            } else if (rest.get().isBNode()) {
                current = (Resource) rest.get();
            } else {
                current = null;
                break;
            }
        }
        currentlyWritingBlankNodes.remove(listHead);

        if (items.isEmpty() || current != null) {
            listBlankNodes.forEach(currentlyWritingBlankNodes::remove);
            return false;
        }

        consumedBlankNodes.addAll(listBlankNodes);

        writer.write(SerializationConstants.OPEN_PARENTHESIS);
        boolean firstItem = true;
        for (Value item : items) {
            if (!firstItem) writer.write(SerializationConstants.SPACE);
            firstItem = false;
            writeValue(writer, item);
        }
        writer.write(SerializationConstants.CLOSE_PARENTHESIS);
        return true;
    }

    /**
     * Determines if a value (subject, predicate, object) is a blank node that has already been
     * serialized inline (within a '[]' or '()') and should be ignored during top-level serialization.
     *
     * @param value the {@link Value} to check.
     * @return {@code true} if the value is a consumed blank node, {@code false} otherwise.
     */
    private boolean isConsumed(Value value) {
        return value.isBNode() && consumedBlankNodes.contains(value);
    }

    /**
     * Identifies and returns a set of blank nodes that can be serialized inline (either as `[]` or as `()` for lists).
     * These nodes will then be "consumed" to prevent their serialization as top-level triples.
     *
     * @return A {@link Set} of {@link Resource} representing the blank nodes that will be serialized inline.
     */
    private Set<Resource> precomputeInlineBlankNodesAndLists() {
        Set<Resource> precomputed = new HashSet<>();
        for (Statement stmt : model) {
            if (stmt.getSubject().isBNode()) {
                Resource bNodeSubject = stmt.getSubject();

                if (config.useCollections() && isRDFListHead(bNodeSubject)) {
                    Resource current = bNodeSubject;
                    Set<Resource> listNodes = new HashSet<>();
                    Set<Resource> visitedInPrecomp = new HashSet<>();
                    boolean isList = true;
                    while (current != null && current.isBNode() && !visitedInPrecomp.contains(current)) {
                        visitedInPrecomp.add(current);
                        listNodes.add(current);
                        final Resource finalCurrentForLambda = current;
                        List<Statement> listProps = model.stream()
                                .filter(s -> s.getSubject().equals(finalCurrentForLambda))
                                .toList();

                        if (listProps.size() != 2) {
                            isList = false;
                            break;
                        }

                        Optional<Value> first = listProps.stream()
                                .filter(s -> s.getPredicate().stringValue().equals(SerializationConstants.RDF_FIRST))
                                .map(Statement::getObject)
                                .findFirst();

                        Optional<Value> rest = listProps.stream()
                                .filter(s -> s.getPredicate().stringValue().equals(SerializationConstants.RDF_REST))
                                .map(Statement::getObject)
                                .findFirst();

                        if (!first.isPresent() || !rest.isPresent()) {
                            isList = false;
                            break;
                        }

                        if (rest.get().stringValue().equals(SerializationConstants.RDF_NIL)) {
                            current = null;
                        } else if (rest.get().isBNode()) {
                            current = (Resource) rest.get();
                        } else {
                            isList = false;
                            break;
                        }
                    }
                    if (isList && current == null) {
                        precomputed.addAll(listNodes);
                    }
                }
                if (config.getBlankNodeStyle() == BlankNodeStyleEnum.ANONYMOUS) {
                    List<Statement> properties = model.stream()
                            .filter(s -> s.getSubject().equals(bNodeSubject))
                            .toList();

                    boolean isPartOfList = properties.stream().anyMatch(s ->
                            s.getPredicate().stringValue().equals(SerializationConstants.RDF_FIRST) ||
                                    s.getPredicate().stringValue().equals(SerializationConstants.RDF_REST)
                    );

                    if (!properties.isEmpty() && !isPartOfList) {
                        precomputed.add(bNodeSubject);
                    }
                }
            }
        }
        return precomputed;
    }


    /**
     * Checks if a given blank node is the head of an RDF list.
     *
     * @param bNode the blank node to check.
     * @return true if it's the head of an RDF list, false otherwise.
     */
    private boolean isRDFListHead(Resource bNode) {

        boolean hasFirstAndRest = model.stream()
                .filter(stmt -> stmt.getSubject().equals(bNode))
                .anyMatch(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_FIRST))
                &&
                model.stream()
                        .filter(stmt -> stmt.getSubject().equals(bNode))
                        .anyMatch(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_REST));

        if (!hasFirstAndRest) return false;

        // Check if this blank node is the object of another rdf:rest triple
        boolean isObjectOfRest = model.stream()
                .filter(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_REST))
                .anyMatch(stmt -> stmt.getObject().equals(bNode));

        return hasFirstAndRest && !isObjectOfRest;
    }


    // --- Helpers for prefix resolution ---

    /**
     * Adds a prefix-namespace URI mapping to the internal mappings.
     * Handles potential conflicts to ensure uniqueness.
     *
     * @param namespaceURI The namespace URI.
     * @param prefix       The associated prefix.
     */
    private void addPrefixMapping(String namespaceURI, String prefix) {

        if (iriToPrefixMapping.containsKey(namespaceURI)) {
            if (!iriToPrefixMapping.get(namespaceURI).equals(prefix) && logger.isWarnEnabled()) {
                logger.warn("Namespace URI '{}' is already mapped to prefix '{}'. Cannot map to new prefix '{}'.",
                        namespaceURI, iriToPrefixMapping.get(namespaceURI), prefix);

            }
            return;
        }


        if (prefixToIriMapping.containsKey(prefix)) {
            if (!prefixToIriMapping.get(prefix).equals(namespaceURI) && logger.isWarnEnabled()) {
                String originalNamespace = prefixToIriMapping.get(prefix);
                logger.warn("Prefix '{}' is already mapped to namespace '{}'. Cannot map to new namespace '{}'. " +
                                "A new unique prefix will be generated for '{}'.",
                        prefix, originalNamespace, namespaceURI, namespaceURI);

            }
            return;
        }

        iriToPrefixMapping.put(namespaceURI, prefix);
        prefixToIriMapping.put(prefix, namespaceURI);
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
     * Attempts to find a prefixed name for an IRI from existing mappings.
     *
     * @param iriString The full IRI.
     * @return The prefixed name (e.g., "ex:someResource") or null if no suitable prefix is found.
     */
    private String getPrefixedName(String iriString) {
        for (Map.Entry<String, String> entry : iriToPrefixMapping.entrySet()) {
            String namespace = entry.getKey();
            String prefix = entry.getValue();

            if (iriString.startsWith(namespace)) {
                String localName = iriString.substring(namespace.length());
                if (localName.isEmpty()) {
                    if (!prefix.isEmpty()) {
                        return prefix + SerializationConstants.COLON;
                    } else {
                        continue;
                    }
                }
                return prefix + SerializationConstants.COLON + localName;
            }
        }
        return null;
    }

    /**
     * Suggests a prefix for a given namespace URI.
     * Attempts to derive a meaningful prefix or generates a unique one.
     *
     * @param namespace The namespace URI.
     * @return A suggested prefix, or null if suggestion is not possible.
     */
    private String getSuggestedPrefix(String namespace) {
        // Try common predefined prefixes
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
                URI uri = new URI(namespace);
                base = uri.getHost().replace(SerializationConstants.POINT, SerializationConstants.EMPTY_STRING);
            } catch (URISyntaxException e) {
                logger.warn("Malformed URI encountered while suggesting prefix: {}", namespace, e);
                base = "p";
            }
        }

        base = base.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.isEmpty()) base = "p";

        // Ensure uniqueness
        String candidate = base;
        int i = 0;
        while (prefixToIriMapping.containsKey(candidate) && !prefixToIriMapping.get(candidate).equals(namespace)) {
            candidate = base + (++i);
        }
        return candidate;
    }


    // --- Helpers for escaping and validation ---

    /**
     * Escapes special characters in Turtle string literals.
     * Handles backslashes, double quotes, and common control characters.
     * Unicode escape sequences are used for unprintable characters if `escapeUnicode` is true.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for a Turtle literal.
     */
    private String escapeTurtleLiteral(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                    sb.append(SerializationConstants.BACK_SLASH).append("n");
                    break;
                case '\r':
                    sb.append(SerializationConstants.BACK_SLASH).append("r");
                    break;
                case '\t':
                    sb.append(SerializationConstants.BACK_SLASH).append("t");
                    break;
                case '\b':
                    sb.append(SerializationConstants.BACK_SLASH).append("b");
                    break;
                case '\f':
                    sb.append(SerializationConstants.BACK_SLASH).append("f");
                    break;
                case '"':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.QUOTE);
                    break;
                case '\\':
                    sb.append(SerializationConstants.BACK_SLASH).append(SerializationConstants.BACK_SLASH);
                    break;
                default:
                    if (config.escapeUnicode() && (c <= 0x1F || c == 0x7F || (c >= 0x80 && c <= 0xFFFF))) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (Character.isHighSurrogate(c)) {
                        int codePoint = value.codePointAt(i);
                        if (Character.isValidCodePoint(codePoint)) {
                            sb.append(String.format("\\U%08X", codePoint));
                            i++;
                        } else {
                            sb.append(c);
                        }
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in multi-line literals (triple-quotes).
     * Primarily used to escape occurrences of `"""` within the literal.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string suitable for a multi-line Turtle literal.
     */
    private String escapeMultilineLiteral(String value) {

        return value.replace(SerializationConstants.QUOTE + SerializationConstants.QUOTE + SerializationConstants.QUOTE,
                SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE +
                        SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE +
                        SerializationConstants.BACK_SLASH + SerializationConstants.QUOTE);
    }

    /**
     * Escapes characters in an IRI string for Turtle output.
     * This method primarily focuses on control characters and problematic characters within angle brackets.
     *
     * @param iri The IRI to escape.
     * @return The escaped IRI.
     */
    private String escapeTurtleIRI(String iri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iri.length(); i++) {
            char c = iri.charAt(i);
            if (c < 0x20 || c == 0x7F || c == SerializationConstants.LT.charAt(0) || c == SerializationConstants.GT.charAt(0) || c == SerializationConstants.QUOTE.charAt(0) || c == '{' || c == '}' || c == '|' || c == '^' || c == '`' || c == SerializationConstants.BACK_SLASH.charAt(0)) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Validates RDF values before serialization.
     * Called only if strictMode is enabled.
     *
     * @param value The {@link Value} to validate.
     * @throws IllegalArgumentException if the value is null or invalid according to strict rules.
     */
    private void validateValue(Value value) {
        if (value == null) {
            logger.warn("Null value encountered where a non-null value was expected for Turtle serialization. This will lead to an IllegalArgumentException if strict mode is enabled.");
            throw new IllegalArgumentException("Value cannot be null in Turtle format when strictMode is enabled.");
        }

        if (config.isStrictMode() && value.isLiteral()) {
            validateLiteral((Literal) value);
        }
    }

    /**
     * Validates a {@link Literal} to ensure it conforms to RDF/Turtle rules.
     * Specifically checks for consistency between language tags and the rdf:langString datatype.
     * Called only if strictMode is enabled.
     *
     * @param literal The {@link Literal} to validate.
     * @throws IllegalArgumentException if the literal is invalid (e.g., language tag with wrong datatype,
     *                                  or rdf:langString literal without language tag).
     */
    private void validateLiteral(Literal literal) {
        IRI datatype = literal.getDatatype();

        if (literal.getLanguage().isPresent()) {
            if (datatype == null || !datatype.stringValue().equals(RDF.LANGSTRING.getIRI().stringValue())) {
                throw new IllegalArgumentException(
                        "A literal with a language tag must use the rdf:langString datatype. Found: " + (datatype != null ? datatype.stringValue() : "null"));
            }
        } else {
            if (datatype != null && datatype.stringValue().equals(RDF.LANGSTRING.getIRI().stringValue())) {
                throw new IllegalArgumentException(
                        "An rdf:langString literal must have a language tag.");
            }
        }
    }

    /**
     * Validates an {@link IRI} to ensure it conforms to Turtle rules.
     * Checks if the IRI string contains characters not allowed in unescaped Turtle
     * form within angle brackets (e.g., control characters, space).
     * Called only if strictMode and validateURIs are enabled.
     *
     * @param iri The {@link IRI} to validate.
     * @throws IllegalArgumentException if the IRI contains invalid characters.
     */
    private void validateIRI(IRI iri) {
        String iriString = iri.stringValue();

        if (iriString.contains(SerializationConstants.SPACE) ||
                iriString.contains(SerializationConstants.QUOTE) ||
                iriString.contains(SerializationConstants.LT) ||
                iriString.contains(SerializationConstants.GT)) {
            throw new IllegalArgumentException("IRI contains illegal characters (space, quotes, angle brackets) for unescaped Turtle form: " + iriString);
        }
    }

    /**
     * Logs a warning if a context (named graph) is present in a statement,
     * as the Turtle format does not support named graphs.
     *
     * @param stmt The statement to check.
     */
    private void logContextWarning(Statement stmt) {
        if (stmt.getContext() != null && logger.isWarnEnabled()) {
            logger.warn("Turtle format does not support named graphs. Context '{}' will be ignored for statement: {}",
                    stmt.getContext().stringValue(), stmt);
        }
    }
}
