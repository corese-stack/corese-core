package fr.inria.corese.core.next.impl.io.serialization.base;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.io.serialization.option.*;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
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
 * Abstract base class for RDF serializers based on TriG and Turtle syntax.
 * This class contains the common logic for serializing RDF models
 * into formats that support prefixes, nested blank nodes,
 * RDF collections, and compact triple serialization.
 * Subclasses must implement format-specific methods
 * (context handling, specific escaping rules).
 *
 * <p>Note: Many features related to compact syntax, pretty-printing, and advanced
 * prefix management are specific to Turtle Trig formats and require the
 * provided {@link AbstractSerializerOption} to be an instance of
 * {@link AbstractSerializerOption} at runtime. An {@link IllegalStateException}
 * will be thrown if an incompatible configuration is used for such features.</p>
 */
public abstract class AbstractGraphSerializer implements RDFSerializer {

    /**
     * Logger for this class, used to log potential issues or information during serialization.
     */
    protected static final Logger logger = LoggerFactory.getLogger(AbstractGraphSerializer.class);

    protected final Model model;
    protected final AbstractSerializerOption option;
    protected final Map<String, String> iriToPrefixMapping;
    protected final Map<String, String> prefixToIriMapping;
    protected final Set<Resource> consumedBlankNodes;
    protected final Set<Resource> currentlyWritingBlankNodes;

    /**
     * Constructs a new abstract TriG/Turtle serializer instance.
     *
     * @param model  the {@link Model} to serialize. Must not be null.
     * @param option the {@link AbstractSerializerOption} to use for serialization. Must not be null.
     * @throws NullPointerException if the provided model or configuration is null.
     */
    protected AbstractGraphSerializer(Model model, AbstractSerializerOption option) {
        this.model = Objects.requireNonNull(model, "The model cannot be null");
        this.option = Objects.requireNonNull(option, "The configuration cannot be null");
        this.iriToPrefixMapping = new HashMap<>();
        this.prefixToIriMapping = new HashMap<>();
        this.consumedBlankNodes = new HashSet<>();
        this.currentlyWritingBlankNodes = new HashSet<>();
        initializePrefixes();
    }

    /**
     * Helper method to safely cast the generic config to AbstractTFamilyConfig.
     * This should be called before accessing any methods specific to AbstractTFamilyConfig.
     *
     * @return The config cast to AbstractTFamilyConfig.
     * @throws IllegalStateException if the config is not an instance of AbstractTFamilyConfig.
     */
    private AbstractTFamilyOption getTFamilyOption() {
        if (!(option instanceof AbstractTFamilyOption)) {
            throw new IllegalStateException("Current serializer configuration is not an instance of AbstractTFamilyOption. " +
                    "Features like prefixes, compact syntax, and pretty-printing are only available for T-Family formats.");
        }
        return (AbstractTFamilyOption) option;
    }

    /**
     * Initializes prefix mappings by adding custom prefixes from the configuration.
     */
    private void initializePrefixes() {
        if (option instanceof AbstractTFamilyOption && getTFamilyOption().usePrefixes()) {
            for (Map.Entry<String, String> entry : getTFamilyOption().getCustomPrefixes().entrySet()) {
                addPrefixMapping(entry.getValue(), entry.getKey());
            }
        }
    }

    /**
     * Writes the model to the given writer in the specific format.
     *
     * @param writer the {@link Writer} to which the output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        try (Writer bufferedWriter = new BufferedWriter(writer)) {
            writeHeader(bufferedWriter);

            Set<Resource> precomputedInlineBlankNodes = precomputeInlineBlankNodesAndLists();
            consumedBlankNodes.addAll(precomputedInlineBlankNodes);

            doWriteStatements(bufferedWriter);

            bufferedWriter.flush();
        } catch (IOException e) {
            throw new SerializationException("Failed to write to stream for format " + getFormatName(), getFormatName(), e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid data for format " + getFormatName() + ": " + e.getMessage(), getFormatName(), e);
        }
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return the format name (e.g., "TriG", "Turtle").
     */
    protected abstract String getFormatName();

    /**
     * Abstract method for the main statement writing,
     * to be implemented by subclasses to handle format-specific details.
     *
     * @param writer the {@link Writer} to which the statements will be written.
     * @throws IOException            if an I/O error occurs.
     * @throws SerializationException if a format-specific serialization error occurs.
     */
    protected abstract void doWriteStatements(Writer writer) throws IOException, SerializationException;

    /**
     * Writes the document header, including base IRI declaration and prefixes.
     *
     * @param writer the {@link Writer} to which the header will be written.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeHeader(Writer writer) throws IOException {
        if (option.getBaseIRI() != null) {
            writer.write(String.format("@base <%s> .%s",
                    option.getBaseIRI(),
                    option.getLineEnding()));
        }

        if (option instanceof AbstractSerializerOption
                && getTFamilyOption().usePrefixes()
                && getTFamilyOption().autoDeclarePrefixes()) {
            collectUsedNamespaces();
        }

        writePrefixDeclarations(writer);
    }

    /**
     * Collects all namespaces used in the model and attempts to assign prefixes to them
     * if auto-declaration is enabled and they are not already mapped.
     */
    protected void collectUsedNamespaces() {
        AbstractSerializerOption tFamilyConfig = getTFamilyOption();

        Set<String> namespaces = model.stream()
                .flatMap(stmt -> {
                    List<Value> values = new ArrayList<>(Arrays.asList(
                            stmt.getSubject(),
                            stmt.getPredicate(),
                            stmt.getObject()
                    ));
                    if (stmt.getContext() != null) {
                        values.add(stmt.getContext());
                    }
                    return values.stream();
                })
                .filter(Objects::nonNull)
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
    protected void writePrefixDeclarations(Writer writer) throws IOException {
        AbstractTFamilyOption tFamilyConfig = getTFamilyOption();

        List<String> prefixes = new ArrayList<>(prefixToIriMapping.keySet());

        if (tFamilyConfig.getPrefixOrdering() == PrefixOrderingEnum.ALPHABETICAL) {
            Collections.sort(prefixes);
        }

        for (String prefix : prefixes) {
            writer.write(String.format("@prefix %s: <%s> .%s",
                    prefix,
                    prefixToIriMapping.get(prefix),
                    option.getLineEnding()));
        }

        if (!prefixes.isEmpty() || option.getBaseIRI() != null) {
            writer.write(option.getLineEnding());
        }
    }

    /**
     * Serializes the model's statements in a simple manner, one per line, without grouping.
     * Triples already "consumed" by inline serialization are ignored.
     *
     * @param writer the {@link Writer} to which the statements will be written.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeSimpleStatements(Writer writer) throws IOException {
        for (Statement stmt : model) {
            if (!isConsumed(stmt.getSubject())) {
                writeStatement(writer, stmt);
                writer.write(option.getLineEnding());
            }
        }
    }

    /**
     * Writes a single {@link Statement} to the writer.
     * This method is designed to be called by statement writing methods (simple or optimized).
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt   the {@link Statement} to write.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeStatement(Writer writer, Statement stmt) throws IOException {
        AbstractTFamilyOption tFamilyConfig = getTFamilyOption();

        String indent = tFamilyConfig.prettyPrint() ? tFamilyConfig.getIndent() : SerializationConstants.EMPTY_STRING;
        writer.write(indent);

        // Subject
        writeValue(writer, stmt.getSubject());
        writer.write(SerializationConstants.SPACE);

        // Predicate
        writePredicate(writer, stmt.getPredicate());
        writer.write(SerializationConstants.SPACE);

        // Object
        writeValue(writer, stmt.getObject());

        writer.write(SerializationConstants.SPACE);
        writer.write(SerializationConstants.POINT);
    }

    /**
     * Writes the predicate to the writer, using the 'a' shortcut if configured and applicable.
     *
     * @param writer    the {@link Writer} to which the predicate will be written.
     * @param predicate the {@link Value} representing the predicate.
     * @throws IOException if an I/O error occurs.
     */
    protected void writePredicate(Writer writer, Value predicate) throws IOException {
        AbstractTFamilyOption tFamilyConfig = getTFamilyOption();
        if (tFamilyConfig.useRdfTypeShortcut() && predicate.stringValue().equals(SerializationConstants.RDF_TYPE)) {
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
    protected void writeValue(Writer writer, Value value) throws IOException {
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
            if (option instanceof AbstractSerializerOption && getTFamilyOption().useCollections() && bNode.isBNode()) {
                handled = writeRDFList(writer, bNode);
            }

            if (!handled && option instanceof AbstractSerializerOption && getTFamilyOption().getBlankNodeStyle() == BlankNodeStyleEnum.ANONYMOUS && bNode.isBNode()) { // getBlankNodeStyle is on AbstractTFamilyConfig
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
            throw new IllegalArgumentException("Unsupported value type for " + getFormatName() + " serialization: " + value.getClass().getName());
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
    protected void writeIRI(Writer writer, IRI iri) throws IOException {
        if (option.isStrictMode() && option.validateURIs()) {
            validateIRI(iri);
        }

        String prefixed = null;
        if (option instanceof AbstractSerializerOption && getTFamilyOption().usePrefixes()) {
            prefixed = getPrefixedName(iri.stringValue());
        }


        if (prefixed != null) {
            writer.write(prefixed);
        } else {
            writer.write(String.format("<%s>", escapeIRIString(iri.stringValue())));
        }
    }

    /**
     * Writes a {@link Literal} to the writer.
     * Applies escaping and datatype/language tag rules based on configuration.
     *
     * @param writer  the {@link Writer} to which the literal will be written.
     * @param literal the {@link Literal} to write.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeLiteral(Writer writer, Literal literal) throws IOException {
        String value = literal.stringValue();

        boolean useTripleQuotes = false;
        if (option instanceof AbstractSerializerOption) {
            useTripleQuotes = getTFamilyOption().shouldUseTripleQuotes(value);
        }


        if (useTripleQuotes) {
            writer.write(String.format("\"\"\"%s\"\"\"", escapeMultilineLiteralString(value)));
        } else {
            writer.write(String.format("\"%s\"", escapeLiteralString(value)));
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
    protected void writeDatatype(Writer writer, Literal literal) throws IOException {
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
    protected boolean shouldWriteDatatype(Literal literal) {
        if (literal.getLanguage().isPresent()) {
            return false;
        }

        IRI datatype = literal.getDatatype();
        if (datatype == null) {
            return false;
        }

        return option.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.ALWAYS_TYPED ||
                (!datatype.stringValue().equals(SerializationConstants.XSD_STRING) &&
                        option.getLiteralDatatypePolicy() == LiteralDatatypePolicyEnum.MINIMAL);
    }

    /**
     * Writes an inline blank node using the '[]' syntax.
     * The blank node's properties are serialized inside the brackets.
     *
     * @param writer     the {@link Writer} to which the blank node will be written.
     * @param properties the list of statements where the blank node is the subject.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeInlineBlankNode(Writer writer, List<Statement> properties) throws IOException {
        AbstractTFamilyOption tFamilyConfig = getTFamilyOption();

        String currentIndent = tFamilyConfig.prettyPrint() ? tFamilyConfig.getIndent() : SerializationConstants.EMPTY_STRING;
        String propIndent = tFamilyConfig.prettyPrint() ? currentIndent + tFamilyConfig.getIndent() : "";

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

            if (tFamilyConfig.prettyPrint()) {
                writer.write(option.getLineEnding() + propIndent);
            } else {
                writer.write(SerializationConstants.SPACE);
            }

            writePredicate(writer, stmt.getPredicate());
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, stmt.getObject());
        }

        if (tFamilyConfig.prettyPrint() && !properties.isEmpty() && !firstProperty) {
            writer.write(option.getLineEnding() + currentIndent);
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
    protected void writeOptimizedStatements(Writer writer) throws IOException {
        AbstractTFamilyOption tFamilyConfig = getTFamilyOption();

        Map<Resource, List<Statement>> bySubject = tFamilyConfig.sortSubjects() ?
                new TreeMap<>(Comparator.comparing(Resource::stringValue)) :
                new LinkedHashMap<>();

        model.stream()
                .filter(stmt -> !isConsumed(stmt.getSubject()))
                .forEach(stmt -> bySubject.computeIfAbsent(stmt.getSubject(), k -> new ArrayList<>()).add(stmt));

        for (Map.Entry<Resource, List<Statement>> subjectEntry : bySubject.entrySet()) {
            String indent = tFamilyConfig.prettyPrint() ? tFamilyConfig.getIndent() : SerializationConstants.EMPTY_STRING;
            writer.write(indent);
            writeValue(writer, subjectEntry.getKey());
            writer.write(SerializationConstants.SPACE);

            Map<IRI, List<Statement>> byPredicate = tFamilyConfig.sortPredicates() ?
                    new TreeMap<>(Comparator.comparing(IRI::stringValue)) :
                    new LinkedHashMap<>();

            subjectEntry.getValue().forEach(stmt -> byPredicate.computeIfAbsent(stmt.getPredicate(), k -> new ArrayList<>()).add(stmt));

            boolean firstPredicate = true;
            for (Map.Entry<IRI, List<Statement>> predicateEntry : byPredicate.entrySet()) {
                if (!firstPredicate) {
                    writer.write(SerializationConstants.SEMICOLON);
                    if (tFamilyConfig.prettyPrint()) {
                        writer.write(option.getLineEnding() + indent + tFamilyConfig.getIndent());
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
                        if (tFamilyConfig.prettyPrint()) {
                            writer.write(option.getLineEnding() + indent + tFamilyConfig.getIndent() + tFamilyConfig.getIndent());
                        } else {
                            writer.write(SerializationConstants.SPACE);
                        }
                    }
                    firstObject = false;

                    writeValue(writer, stmt.getObject());
                }
            }

            writer.write(SerializationConstants.SPACE + SerializationConstants.POINT);
            writer.write(option.getLineEnding());
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
    protected boolean writeRDFList(Writer writer, Resource listHead) throws IOException {
        AbstractSerializerOption tFamilyConfig = getTFamilyOption();

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
                current = null;
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
    protected boolean isConsumed(Value value) {
        return value.isBNode() && consumedBlankNodes.contains(value);
    }

    /**
     * Identifies and returns a set of blank nodes that can be serialized inline (either as '[]' or as '()' for lists).
     * These nodes will then be "consumed" to prevent their serialization as top-level triples.
     *
     * @return A {@link Set} of {@link Resource} representing the blank nodes that will be serialized inline.
     */
    protected Set<Resource> precomputeInlineBlankNodesAndLists() {
        AbstractTFamilyOption tFamilyConfig = getTFamilyOption();

        Set<Resource> precomputed = new HashSet<>();
        for (Statement stmt : model) {
            if (stmt.getSubject().isBNode()) {
                Resource bNodeSubject = stmt.getSubject();
                if (tFamilyConfig.useCollections() && isRDFListHead(bNodeSubject)) {
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
                if (tFamilyConfig.getBlankNodeStyle() == BlankNodeStyleEnum.ANONYMOUS) {
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
    protected boolean isRDFListHead(Resource bNode) {
        boolean hasFirstAndRest = model.stream()
                .filter(stmt -> stmt.getSubject().equals(bNode))
                .anyMatch(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_FIRST))
                &&
                model.stream()
                        .filter(stmt -> stmt.getSubject().equals(bNode))
                        .anyMatch(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_REST));

        boolean isObjectOfRest = model.stream()
                .filter(stmt -> stmt.getPredicate().stringValue().equals(SerializationConstants.RDF_REST))
                .anyMatch(stmt -> stmt.getObject().equals(bNode));

        return hasFirstAndRest && !isObjectOfRest;
    }


    /**
     * Adds a prefix-namespace URI mapping to the internal mappings.
     * Handles potential conflicts to ensure uniqueness.
     *
     * @param namespaceURI The namespace URI.
     * @param prefix       The associated prefix.
     */
    protected void addPrefixMapping(String namespaceURI, String prefix) {
        if (iriToPrefixMapping.containsKey(namespaceURI)) {
            if (logger.isWarnEnabled() && !iriToPrefixMapping.get(namespaceURI).equals(prefix)) {
                logger.warn("Namespace URI '{}' is already mapped to prefix '{}'. Cannot map to new prefix '{}'.",
                        namespaceURI, iriToPrefixMapping.get(namespaceURI), prefix);
            }
            return;
        }

        if (prefixToIriMapping.containsKey(prefix)) {
            if (logger.isWarnEnabled() && !prefixToIriMapping.get(prefix).equals(namespaceURI)) {
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
    protected String getNamespace(String iriString) {
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
    protected String getPrefixedName(String iriString) {
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
    protected String getSuggestedPrefix(String namespace) {
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
     * Abstract method to escape special characters in an IRI string for the specific format.
     *
     * @param iri The IRI to escape.
     * @return The escaped IRI.
     */
    protected abstract String escapeIRIString(String iri);

    /**
     * Abstract method to escape special characters in string literals.
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string.
     */
    protected abstract String escapeLiteralString(String value);

    /**
     * Abstract method to escape special characters in multi-line literals (triple-quotes).
     *
     * @param value The string value of the literal to escape.
     * @return The escaped string.
     */
    protected abstract String escapeMultilineLiteralString(String value);

    /**
     * Validates RDF values before serialization.
     * Called only if strictMode is enabled.
     *
     * @param value The {@link Value} to validate.
     * @throws IllegalArgumentException if the value is null or invalid according to strict rules.
     */
    protected void validateValue(Value value) {
        if (value == null) {
            logger.warn("Null value encountered where a non-null value was expected for {} serialization. This will lead to an IllegalArgumentException if strict mode is enabled.", getFormatName());
            throw new IllegalArgumentException("Value cannot be null in {} format when strictMode is enabled." + getFormatName());
        }

        if (option.isStrictMode() && value.isLiteral()) {
            validateLiteral((Literal) value);
        }
    }

    /**
     * Validates a {@link Literal} to ensure it conforms to RDF/format rules.
     * Specifically checks for consistency between language tags and the rdf:langString datatype.
     * Called only if strictMode is enabled.
     *
     * @param literal The {@link Literal} to validate.
     * @throws IllegalArgumentException if the literal is invalid (e.g., language tag with wrong datatype,
     *                                  or rdf:langString literal without language tag).
     */
    protected void validateLiteral(Literal literal) {
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
     * Validates an {@link IRI} to ensure it conforms to format rules.
     * Checks if the IRI string contains characters not allowed in unescaped form
     * within angle brackets (e.g., control characters, space).
     * Called only if strictMode and validateURIs are enabled.
     *
     * @param iri The {@link IRI} to validate.
     * @throws IllegalArgumentException if the IRI contains invalid characters.
     */
    protected void validateIRI(IRI iri) {
        String iriString = iri.stringValue();

        if (iriString.contains(SerializationConstants.SPACE) ||
                iriString.contains(SerializationConstants.QUOTE) ||
                iriString.contains(SerializationConstants.LT) ||
                iriString.contains(SerializationConstants.GT)) {
            throw new IllegalArgumentException("IRI contains illegal characters (space, quotes, angle brackets) for the unescaped form of " + getFormatName() + ": " + iriString);
        }
    }
}
