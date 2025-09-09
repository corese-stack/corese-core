package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.io.serialization.util.StatementUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation of the RDFC-1.0 canonicalization algorithm as specified by W3C.
 * This class deterministically re-labels blank nodes and sorts all RDF statements
 * to produce a canonical representation of a dataset.
 */
public class Rdfc10Canonicalizer {

    private final CanonicalOption.HashAlgorithm hashAlgorithm;
    private final int maxCallsHashNDegreeQuads;
    private final StatementUtils statementUtils;
    private int callsHashNDegreeQuads = 0;

    /**
     * Constructs a new Rdfc10Canonicalizer with specified configuration.
     *
     * @param hashAlgorithm The hashing algorithm to use for canonicalization (SHA-256 or SHA-384).
     * @param maxCalls      The maximum number of recursive calls to the Hash N-Degree Quads algorithm
     * to prevent infinite loops on complex cyclic graphs.
     * @param valueFactory  The factory for creating RDF values, used by StatementUtils for
     * blank node replacement and serialization.
     */
    public Rdfc10Canonicalizer(CanonicalOption.HashAlgorithm hashAlgorithm, int maxCalls, ValueFactory valueFactory) {
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "Hash algorithm cannot be null");
        this.maxCallsHashNDegreeQuads = maxCalls;
        this.statementUtils = new StatementUtils(valueFactory);
    }

    /**
     * Canonicalizes all statements within a given Model.
     * This is the main entry point for the canonicalization process.
     * The process involves:
     * 1. Identifying all blank nodes and their associated statements.
     * 2. Creating canonical identifiers for blank nodes.
     * 3. Replacing original blank node IDs with canonical ones.
     * 4. Sorting the resulting statements lexicographically.
     *
     * @param model The input model to canonicalize. Must not be null.
     * @return A list of canonicalized and sorted statements ready for serialization.
     * @throws SerializationException if canonicalization fails due to algorithmic constraints
     * or invalid input data.
     */
    public List<Statement> canonicalize(Model model) {
        Objects.requireNonNull(model, "Model cannot be null");
        return canonicalize(model.stream());
    }

    /**
     * Internal canonicalization method that processes a stream of statements.
     * This method handles all the steps of the RDFC-1.0 algorithm in sequence.
     *
     * @param statements A stream of statements to canonicalize.
     * @return A list of canonicalized and sorted statements.
     */
    private List<Statement> canonicalize(Stream<Statement> statements) {
        List<Statement> stmtList = statements.toList();

        // Reset the recursive call counter for each canonicalization operation
        callsHashNDegreeQuads = 0;

        // Step 1: Create a mapping of blank nodes to their associated statements
        Map<String, Set<Statement>> blankNodeToQuads = createBNodeToQuadsMap(stmtList);

        // If no blank nodes are found, simply sort and return the original statements
        if (blankNodeToQuads.isEmpty()) {
            return stmtList.stream()
                    .sorted(Comparator.comparing(StatementUtils::toNQuad))
                    .toList();
        }

        // Step 2: Generate a canonical replacement mapping for blank nodes
        Map<String, String> canonicalReplacementMap = createCanonicalMap(blankNodeToQuads);

        // Step 3: Apply the replacement and sort the final statements
        return replaceBlankNodesAndSort(stmtList, canonicalReplacementMap);
    }

    /**
     * Creates a map where each blank node identifier is associated with all statements
     * (quads) in which it appears as a subject, object, or graph name.
     * This is the foundation for the Hash First Degree Quads algorithm.
     *
     * @param statements The list of statements to process.
     * @return A map linking blank node identifiers to their associated statements.
     */
    private Map<String, Set<Statement>> createBNodeToQuadsMap(List<Statement> statements) {
        Map<String, Set<Statement>> blankNodeToQuads = new HashMap<>();

        for (Statement stmt : statements) {
            if (stmt == null) continue;

            if (StatementUtils.isBlankNode(stmt.getSubject())) {
                String blankNodeId = StatementUtils.getBlankNodeId(stmt.getSubject());
                blankNodeToQuads.computeIfAbsent(blankNodeId, k -> new HashSet<>()).add(stmt);
            }

            if (StatementUtils.isBlankNode(stmt.getObject())) {
                String blankNodeId = StatementUtils.getBlankNodeId(stmt.getObject());
                blankNodeToQuads.computeIfAbsent(blankNodeId, k -> new HashSet<>()).add(stmt);
            }

            if (stmt.getContext() != null && StatementUtils.isBlankNode(stmt.getContext())) {
                String blankNodeId = StatementUtils.getBlankNodeId(stmt.getContext());
                blankNodeToQuads.computeIfAbsent(blankNodeId, k -> new HashSet<>()).add(stmt);
            }
        }

        return blankNodeToQuads;
    }

    /**
     * Performs the core canonicalization logic to create a map of blank node replacements.
     * This method implements the main flow of the RDFC-1.0 algorithm.
     *
     * @return A deterministic mapping from original blank node identifiers to canonical ones.
     */
    private Map<String, String> createCanonicalMap(Map<String, Set<Statement>> bnodeToQuads) {
        Map<String, String> canonicalIssuer = new HashMap<>();
        int counter = 0;

        // Step 1: Calculate first-degree hashes for all blank nodes
        Map<String, String> firstDegreeHashes = new HashMap<>();
        for (String bnode : bnodeToQuads.keySet()) {
            String hash = hashFirstDegreeQuads(bnode, bnodeToQuads);
            firstDegreeHashes.put(bnode, hash);
        }

        // Step 2: Create hash groups
        Map<String, List<String>> hashToNodes = new HashMap<>();
        for (String node : bnodeToQuads.keySet()) {
            String hash = firstDegreeHashes.get(node);
            hashToNodes.computeIfAbsent(hash, k -> new ArrayList<>()).add(node);
        }

        // Step 3: Separate into single-node and multi-node groups
        List<String> singleNodeHashes = new ArrayList<>();
        List<String> multiNodeHashes = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : hashToNodes.entrySet()) {
            if (entry.getValue().size() == 1) {
                singleNodeHashes.add(entry.getKey());
            } else {
                multiNodeHashes.add(entry.getKey());
            }
        }

        // Sort hashes within their groups
        Collections.sort(singleNodeHashes);
        Collections.sort(multiNodeHashes);

        // Step 4: Process single-node groups first
        for (String hash : singleNodeHashes) {
            String node = hashToNodes.get(hash).get(0);
            canonicalIssuer.put(node, SerializationConstants.C14N + counter++);
        }

        // Step 5: Process multi-node groups using N-degree hashing
        for (String hash : multiNodeHashes) {
            List<String> nodes = hashToNodes.get(hash);

            Map<String, String> nDegreeHashes = new HashMap<>();
            for (String node : nodes) {
                TemporaryIssuer tempIssuer = new TemporaryIssuer();
                String nDegreeHash = hashNDegreeQuads(node, bnodeToQuads, canonicalIssuer, tempIssuer);
                nDegreeHashes.put(node, nDegreeHash);
            }

            nodes.sort((n1, n2) -> {
                int cmp = nDegreeHashes.get(n1).compareTo(nDegreeHashes.get(n2));
                if (cmp != 0) return cmp;
                return n1.compareTo(n2);
            });

            for (String node : nodes) {
                canonicalIssuer.put(node, SerializationConstants.C14N + counter++);
            }
        }

        return canonicalIssuer;
    }

    /**
     * Implements the "Hash First Degree Quads" algorithm from the RDFC-1.0 specification.
     * It computes a hash for a blank node based on canonical representations of all statements
     * in which it appears. It replaces the blank node itself with a standardized placeholder.
     *
     * @param blankNode        The blank node identifier to hash.
     * @param blankNodeToQuads The map of blank nodes to their associated statements.
     * @return A cryptographic hash representing the blank node's first-degree context.
     */
    private String hashFirstDegreeQuads(String blankNode, Map<String, Set<Statement>> blankNodeToQuads) {
        Set<Statement> quads = blankNodeToQuads.get(blankNode);
        List<String> nquads = new ArrayList<>();

        for (Statement quad : quads) {
            String nquad = quadToNQuad(quad, blankNode, SerializationConstants.CANONICAL_BNODE_PLACEHOLDER);
            nquads.add(nquad);
        }

        Collections.sort(nquads);
        String toHash = String.join(SerializationConstants.EMPTY_STRING, nquads);

        return hash(toHash);
    }

    /**
     * Implements the "Hash N-Degree Quads" algorithm for resolving blank node permutations.
     * This recursive method handles cases where multiple blank nodes have identical
     * first-degree hashes by considering their relationships to other blank nodes.
     *
     * @param identifier       The blank node identifier currently being processed.
     * @param blankNodeToQuads The map of blank nodes to their associated statements.
     * @param canonicalIssuer  Map of already-assigned canonical identifiers.
     * @param issuer           Temporary identifier issuer for the current recursion path.
     * @return A hash representing the N-degree context of the blank node.
     * @throws SerializationException if the maximum recursion depth is exceeded.
     */
    private String hashNDegreeQuads(String identifier, Map<String, Set<Statement>> blankNodeToQuads,
                                    Map<String, String> canonicalIssuer, TemporaryIssuer issuer) {

        if (++callsHashNDegreeQuads > maxCallsHashNDegreeQuads) {
            throw new SerializationException(
                    "Maximum calls to Hash N-Degree Quads exceeded: " + maxCallsHashNDegreeQuads,
                    "Rdfc10Canonicalizer"
            );
        }

        // Collect all related blank nodes from all quads containing this node
        Set<String> relatedBlankNodes = new HashSet<>();
        for (Statement quad : blankNodeToQuads.get(identifier)) {
            relatedBlankNodes.addAll(getRelatedBlankNodes(quad, identifier));
        }

        // Calculate hashes for each related blank node
        List<String> relatedHashes = new ArrayList<>();
        for (String relatedNode : relatedBlankNodes) {
            String relatedHash;

            if (canonicalIssuer.containsKey(relatedNode)) {
                // Use canonical ID if already assigned
                relatedHash = canonicalIssuer.get(relatedNode);
            } else if (issuer.hasIssued(relatedNode)) {
                // Use temporary ID if already issued
                relatedHash = issuer.issue(relatedNode);
            } else {
                // Recursively calculate N-degree hash
                TemporaryIssuer newIssuer = issuer.copy();
                relatedHash = hashNDegreeQuads(relatedNode, blankNodeToQuads, canonicalIssuer, newIssuer);
            }

            relatedHashes.add(relatedHash);
        }

        // Sort the related hashes
        Collections.sort(relatedHashes);

        // Build the final hash input
        StringBuilder hashInput = new StringBuilder();
        hashInput.append(hashFirstDegreeQuads(identifier, blankNodeToQuads));
        for (String relatedHash : relatedHashes) {
            hashInput.append(relatedHash);
        }

        return hash(hashInput.toString());
    }

    /**
     * Converts a statement to canonical N-Quad format for hashing, replacing
     * a specific blank node with a placeholder string.
     *
     * @param quad             The statement to convert.
     * @param blankNodeToReplace The blank node identifier to replace.
     * @param replacement      The placeholder string to use for replacement.
     * @return A canonical N-Quad string with placeholder substitution.
     */
    private String quadToNQuad(Statement quad, String blankNodeToReplace, String replacement) {
        StringBuilder sb = new StringBuilder();

        // Handle subject
        if (StatementUtils.isBlankNode(quad.getSubject())) {
            String bnodeId = StatementUtils.getBlankNodeId(quad.getSubject());
            sb.append(bnodeId.equals(blankNodeToReplace) ? replacement : SerializationConstants.CANONICAL_BNODE_PREFIX);
        } else {
            sb.append(StatementUtils.serializeForComparison(quad.getSubject()));
        }
        sb.append(SerializationConstants.SPACE);

        // Predicate
        sb.append(StatementUtils.serializeForComparison(quad.getPredicate())).append(SerializationConstants.SPACE);

        // Handle object
        if (StatementUtils.isBlankNode(quad.getObject())) {
            String bnodeId = StatementUtils.getBlankNodeId(quad.getObject());
            sb.append(bnodeId.equals(blankNodeToReplace) ? replacement : SerializationConstants.CANONICAL_BNODE_PREFIX);
        } else {
            sb.append(StatementUtils.serializeForComparison(quad.getObject()));
        }

        // Handle context
        if (quad.getContext() != null) {
            sb.append(SerializationConstants.SPACE);
            if (StatementUtils.isBlankNode(quad.getContext())) {
                String bnodeId = StatementUtils.getBlankNodeId(quad.getContext());
                sb.append(bnodeId.equals(blankNodeToReplace) ? replacement : SerializationConstants.CANONICAL_BNODE_PREFIX);
            } else {
                sb.append(StatementUtils.serializeForComparison(quad.getContext()));
            }
        }

        sb.append(SerializationConstants.SPACE).append(SerializationConstants.POINT);
        return sb.toString();
    }

    /**
     * Identifies all blank nodes in a statement that are related to but different from
     * a specified blank node. This is used to explore the graph context during N-degree hashing.
     *
     * @param quad             The statement to examine.
     * @param excludeBlankNode The blank node to exclude from the results.
     * @return A set of blank node identifiers related to the excluded node.
     */
    private Set<String> getRelatedBlankNodes(Statement quad, String excludeBlankNode) {
        Set<String> relatedBlankNodes = new HashSet<>();

        // Check subject position
        if (StatementUtils.isBlankNode(quad.getSubject())) {
            String id = StatementUtils.getBlankNodeId(quad.getSubject());
            if (!id.equals(excludeBlankNode)) {
                relatedBlankNodes.add(id);
            }
        }

        // Check object position
        if (StatementUtils.isBlankNode(quad.getObject())) {
            String id = StatementUtils.getBlankNodeId(quad.getObject());
            if (!id.equals(excludeBlankNode)) {
                relatedBlankNodes.add(id);
            }
        }

        // Check context position
        if (quad.getContext() != null && StatementUtils.isBlankNode(quad.getContext())) {
            String id = StatementUtils.getBlankNodeId(quad.getContext());
            if (!id.equals(excludeBlankNode)) {
                relatedBlankNodes.add(id);
            }
        }

        return relatedBlankNodes;
    }

    /**
     * Replaces blank node identifiers in statements and sorts them lexicographically.
     * This is the final step of the canonicalization process.
     *
     * @param statements   The original statements to process.
     * @param canonicalMap The map of blank node replacements.
     * @return A sorted list of statements with canonical blank node identifiers.
     */
    private List<Statement> replaceBlankNodesAndSort(List<Statement> statements, Map<String, String> canonicalMap) {

        List<Statement> replaced = statements.stream()
                .map(stmt -> statementUtils.replaceBlankNodes(stmt, canonicalMap))
                .toList();

        return replaced.stream()
                .sorted(Comparator.comparing(StatementUtils::toNQuad))
                .toList();
    }

    /**
     * Computes a cryptographic hash of the input data using the configured algorithm.
     *
     * @param data The string data to hash.
     * @return A hexadecimal string representation of the hash.
     * @throws SerializationException if the hash algorithm is unavailable.
     */
    private String hash(String data) {
        try {
            String algorithm = hashAlgorithm == CanonicalOption.HashAlgorithm.SHA_384 ?
                    SerializationConstants.SHA_384 : SerializationConstants.SHA_256;
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SerializationException("Hash algorithm not available: " + e.getMessage(),
                    "Rdfc10Canonicalizer", e);
        } catch (Exception e) {
            throw new SerializationException("Hash computation failed for data: " + data,
                    "Rdfc10Canonicalizer", e);
        }
    }

    /**
     * Converts a byte array to its hexadecimal string representation.
     *
     * @param bytes The byte array to convert.
     * @return A hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format(SerializationConstants.HEX_FORMAT, b));
        }
        return result.toString();
    }

    /**
     * Helper class for managing temporary identifiers during recursive hashing.
     * It ensures that each exploration path maintains independent temporary labeling
     * to avoid contamination between different permutation explorations.
     */
    private static class TemporaryIssuer {
        private Map<String, String> issued = new HashMap<>();
        private int counter = 0;

        /**
         * Issues a temporary identifier for a blank node.
         * If the node already has a temporary ID, it returns the existing one.
         *
         * @param identifier The blank node identifier to issue an ID for.
         * @return A temporary canonical identifier.
         */
        public String issue(String identifier) {
            return issued.computeIfAbsent(identifier, k -> SerializationConstants.CANONICAL_BNODE_PREFIX + counter++);
        }

        /**
         * Checks if a temporary identifier has been issued for a blank node.
         *
         * @param identifier The blank node identifier to check.
         * @return true if a temporary ID exists, false otherwise.
         */
        public boolean hasIssued(String identifier) {
            return issued.containsKey(identifier);
        }

        /**
         * Creates an independent copy of this TemporaryIssuer.
         * This is crucial for maintaining path isolation during recursive exploration.
         *
         * @return A new TemporaryIssuer instance with the same state.
         */
        public TemporaryIssuer copy() {
            TemporaryIssuer copy = new TemporaryIssuer();
            copy.issued = new HashMap<>(this.issued);
            copy.counter = this.counter;
            return copy;
        }
    }
}
