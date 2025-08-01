package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.base.AbstractLineBasedSerializer;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serializes a Corese {@link Model} into a canonical RDF format.
 * This serializer ensures a deterministic output by re-labeling blank nodes
 * and sorting all statements. The output format is similar to N-Quads,
 * but with canonical blank node identifiers and a guaranteed order.
 *
 * <p>This implementation provides a simplified blank node canonicalization
 * based on lexicographical sorting of blank node fingerprints, followed by
 * re-labeling and sorting of all triples. It extends {@link AbstractLineBasedSerializer}
 * to reuse common writing utilities but overrides the main {@code write} method
 * to implement the canonicalization logic.</p>
 */
public class CanonicalSerializer extends AbstractLineBasedSerializer {

    private static final Logger logger = LoggerFactory.getLogger(CanonicalSerializer.class);
    private final ValueFactory valueFactory;

    /**
     * Constructs a new {@code CanonicalSerializer} instance with the specified model and default configuration.
     * The default configuration is obtained from {@link CanonicalOption#defaultConfig()}.
     *
     * @param model        the {@link Model} to be serialized. Must not be null.
     * @param valueFactory the {@link ValueFactory} to use for creating RDF elements. Must not be null.
     * @throws NullPointerException if the provided model or valueFactory is null.
     */
    public CanonicalSerializer(Model model, ValueFactory valueFactory) {
        this(model, CanonicalOption.defaultConfig(), valueFactory);
    }

    /**
     * Constructs a new {@code CanonicalSerializer} instance with the specified model and custom configuration.
     *
     * @param model        the {@link Model} to be serialized. Must not be null.
     * @param config       the {@link CanonicalOption} to use for serialization. Must not be null.
     * @param valueFactory the {@link ValueFactory} to use for creating RDF elements. Must not be null.
     * @throws NullPointerException if the provided model, config, or valueFactory is null.
     */
    public CanonicalSerializer(Model model, CanonicalOption config, ValueFactory valueFactory) {
        super(model, config);
        this.valueFactory = Objects.requireNonNull(valueFactory, "ValueFactory cannot be null");
        Objects.requireNonNull(config, "CanonicalOption cannot be null");
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return "Canonical RDF"
     */
    @Override
    protected String getFormatName() {
        return "Canonical RDF";
    }

    /**
     * Writes the context (named graph) part of a statement.
     * For Canonical RDF, contexts are included if present, following N-Quads style.
     *
     * @param writer the {@link Writer} to which the context will be written.
     * @param stmt   the {@link Statement} whose context should be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void writeContext(Writer writer, Statement stmt) throws IOException {
        Resource context = stmt.getContext();
        if (context != null) {
            writer.write(SerializationConstants.SPACE);
            writeValue(writer, context);
        }
    }

    /**
     * Writes the model to the given writer in a canonical form.
     * This involves:
     * 1. Collecting all statements.
     * 2. Identifying and re-labeling blank nodes deterministically.
     * 3. Creating a new set of statements with canonical blank node IDs.
     * 4. Sorting these canonical statements.
     * 5. Writing each sorted statement line by line.
     *
     * @param writer the {@link Writer} to which the output will be written.
     * @throws SerializationException if an I/O error occurs during writing or if invalid data is encountered.
     */
    @Override
    public void write(Writer writer) throws SerializationException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            List<Statement> originalStatements = new ArrayList<>();
            model.forEach(originalStatements::add);

            Set<BNode> blankNodes = new HashSet<>();
            for (Statement stmt : originalStatements) {
                if (stmt.getSubject().isBNode()) {
                    blankNodes.add((BNode) stmt.getSubject()); 
                }
                if (stmt.getObject().isBNode()) {
                    blankNodes.add((BNode) stmt.getObject()); 
                }
                if (stmt.getContext() != null && stmt.getContext().isBNode()) {
                    blankNodes.add((BNode) stmt.getContext()); 
                }
            }

            Map<BNode, BNode> canonicalBNodeMap = createCanonicalBNodeMap(blankNodes, originalStatements);

            List<Statement> canonicalStatements = new ArrayList<>();
            for (Statement originalStmt : originalStatements) {
                Resource subject = (Resource) mapValue(originalStmt.getSubject(), canonicalBNodeMap);
                IRI predicate = originalStmt.getPredicate();
                Value object = mapValue(originalStmt.getObject(), canonicalBNodeMap);
                Resource context = (Resource) mapValue(originalStmt.getContext(), canonicalBNodeMap);

                canonicalStatements.add(valueFactory.createStatement(subject, predicate, object, context));
            }

            Collections.sort(canonicalStatements, new CanonicalStatementComparator(canonicalBNodeMap));

            for (Statement stmt : canonicalStatements) {
                writeCanonicalStatement(bufferedWriter, stmt);
            }

        } catch (IOException e) {
            throw new SerializationException(getFormatName() + " serialization failed", getFormatName(), e);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid " + getFormatName() + " data: " + e.getMessage(), getFormatName(), e);
        }
    }

    /**
     * Maps a value (Resource, Literal, IRI) to its canonical form if it's a blank node.
     *
     * @param value             the original value.
     * @param canonicalBNodeMap the map from original blank nodes to canonical blank nodes.
     * @return the canonical value.
     */
    private Value mapValue(Value value, Map<BNode, BNode> canonicalBNodeMap) { 
        if (value != null && value.isBNode()) {
            return canonicalBNodeMap.getOrDefault((BNode) value, (BNode) value); 
        }
        return value;
    }

    /**
     * Creates a deterministic mapping from original blank node {@link BNode}s
     * to new, canonical blank node {@link BNode}s.
     * This simplified approach sorts blank nodes based on a string representation
     * of their associated triples.
     *
     * @param blankNodes the set of all blank nodes in the model.
     * @param statements the list of all statements in the model.
     * @return a map from original blank node {@link BNode} to canonical blank node {@link BNode}.
     */
    private Map<BNode, BNode> createCanonicalBNodeMap(Set<BNode> blankNodes, List<Statement> statements) { 
        Map<BNode, String> bNodeFingerprints = new HashMap<>(); 
        for (BNode bNode : blankNodes) { 
            List<String> relatedTriples = statements.stream()
                    .filter(stmt -> stmt.getSubject().equals(bNode) || stmt.getObject().equals(bNode) || (stmt.getContext() != null && stmt.getContext().equals(bNode)))
                    .map(Statement::toString)
                    .sorted()
                    .collect(Collectors.toList());
            bNodeFingerprints.put(bNode, String.join("|", relatedTriples));
        }

        List<BNode> sortedBNodes = new ArrayList<>(blankNodes);
        sortedBNodes.sort(Comparator.comparing(bNodeFingerprints::get));

        Map<BNode, BNode> canonicalBNodeMap = new HashMap<>();
        int i = 0;
        for (BNode bNode : sortedBNodes) { 
            canonicalBNodeMap.put(bNode, valueFactory.createBNode("b" + i));
            i++;
        }
        return canonicalBNodeMap;
    }

    /**
     * Writes a single canonical {@link Statement} to the writer.
     * This method is similar to the private `writeStatement` in the superclass,
     * but ensures it uses the canonicalized values.
     *
     * @param writer the {@link Writer} to which the statement will be written.
     * @param stmt   the {@link Statement} to write (already canonicalized).
     * @throws IOException if an I/O error occurs.
     */
    private void writeCanonicalStatement(Writer writer, Statement stmt) throws IOException {
        writeValue(writer, stmt.getSubject());
        writer.write(SerializationConstants.SPACE);
        writeValue(writer, stmt.getPredicate());
        writer.write(SerializationConstants.SPACE);
        writeValue(writer, stmt.getObject());

        writeContext(writer, stmt);

        if (config.trailingDot()) {
            writer.write(SerializationConstants.SPACE);
            writer.write(SerializationConstants.POINT);
        }

        writer.write(config.getLineEnding());
    }

    /**
     * A custom comparator for {@link Statement}s to ensure canonical ordering.
     * This comparator sorts statements based on subject, then predicate, then object, then context.
     * Blank nodes are compared using their canonical labels.
     */
    private class CanonicalStatementComparator implements Comparator<Statement> {
        private final Map<BNode, BNode> canonicalBNodeMap; 

        public CanonicalStatementComparator(Map<BNode, BNode> canonicalBNodeMap) { 
            this.canonicalBNodeMap = canonicalBNodeMap;
        }

        @Override
        public int compare(Statement s1, Statement s2) {
            int cmp = compareValues(s1.getSubject(), s2.getSubject());
            if (cmp != 0) return cmp;

            cmp = compareValues(s1.getPredicate(), s2.getPredicate());
            if (cmp != 0) return cmp;

            cmp = compareValues(s1.getObject(), s2.getObject());
            if (cmp != 0) return cmp;

            return compareValues(s1.getContext(), s2.getContext());
        }

        private int compareValues(Value v1, Value v2) {
            if (v1 == null && v2 == null) return 0;
            if (v1 == null) return -1;
            if (v2 == null) return 1;

            Value cV1 = v1.isBNode() ? canonicalBNodeMap.getOrDefault((BNode) v1, (BNode) v1) : v1;
            Value cV2 = v2.isBNode() ? canonicalBNodeMap.getOrDefault((BNode) v2, (BNode) v2) : v2;


            return cV1.stringValue().compareTo(cV2.stringValue());
        }
    }
}
