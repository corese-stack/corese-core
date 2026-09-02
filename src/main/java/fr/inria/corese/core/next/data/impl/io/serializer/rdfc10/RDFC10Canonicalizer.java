package fr.inria.corese.core.next.data.impl.io.serializer.rdfc10;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.impl.io.serializer.support.SerializationConstants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation of the RDF Dataset Canonicalization algorithm (RDFC-1.0) as specified by the W3C.
 *
 * <p>This algorithm deterministically re-labels blank nodes with canonical identifiers
 * (e.g., {@code _:c14n0}, {@code _:c14n1}) and sorts all RDF statements in canonical N-Quads order.
 * It provides graph isomorphism detection, deterministic blank node relabeling, and supports both
 * SHA-256 and SHA-384 cryptographic hashing algorithms.</p>
 *
 * @see <a href="https://www.w3.org/TR/rdf-canon/">RDF Dataset Canonicalization (W3C Recommendation)</a>
 */
public class RDFC10Canonicalizer {

    private final RDFC10SerializerOptions.HashAlgorithm hashAlgorithm;
    private final int maxCallsHashNDegreeQuads;
    private final int depthFactor;
    private final StatementUtils statementUtils;
    private int callsHashNDegreeQuads = 0;

    /**
     * Context record encapsulating shared state during recursive N-degree blank node hashing.
     *
     * @param bnodeToQuads       Mapping from blank node identifiers to their associated quads.
     * @param firstDegreeHashes  Mapping from blank node identifiers to their computed 1-degree hashes.
     * @param canonicalIssuer    The main issuer containing finalized canonical blank node mappings.
     */
    private record NDegreeContext(
            Map<String, List<Statement>> bnodeToQuads,
            Map<String, String> firstDegreeHashes,
            CanonicalIssuer canonicalIssuer
    ) {}

    /**
     * Constructs a new {@code RDFC10Canonicalizer} with the default depth factor (5).
     *
     * @param hashAlgorithm The cryptographic hashing algorithm (SHA-256 or SHA-384).
     * @param maxCalls      Maximum allowed recursive permutations to prevent denial of service.
     * @param valueFactory  Factory used to instantiate RDF terms and statements.
     */
    public RDFC10Canonicalizer(RDFC10SerializerOptions.HashAlgorithm hashAlgorithm, int maxCalls, ValueFactory valueFactory) {
        this(hashAlgorithm, maxCalls, 5, valueFactory);
    }

    /**
     * Constructs a new {@code RDFC10Canonicalizer} with full configuration options.
     *
     * @param hashAlgorithm The cryptographic hashing algorithm (SHA-256 or SHA-384).
     * @param maxCalls      Maximum allowed recursive permutations to prevent denial of service.
     * @param depthFactor   Depth multiplier factor for recursion limit based on blank node count.
     * @param valueFactory  Factory used to instantiate RDF terms and statements.
     */
    public RDFC10Canonicalizer(RDFC10SerializerOptions.HashAlgorithm hashAlgorithm, int maxCalls, int depthFactor, ValueFactory valueFactory) {
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "Hash algorithm cannot be null");
        this.maxCallsHashNDegreeQuads = maxCalls;
        this.depthFactor = depthFactor;
        this.statementUtils = new StatementUtils(valueFactory);
    }

    /**
     * Computes the canonical blank node replacement map for a {@link Model} according to W3C RDFC-1.0.
     *
     * @param model The model whose blank nodes will be mapped. Must not be null.
     * @return An unmodifiable map from original blank node identifiers to their canonical identifiers (e.g. c14n0).
     */
    public Map<String, String> canonicalMap(Model model) {
        Objects.requireNonNull(model, "Model cannot be null");
        List<Statement> stmtList = model.stream().toList();
        callsHashNDegreeQuads = 0;
        Map<String, List<Statement>> blankNodeToQuads = createBNodeToQuadsMap(stmtList);
        if (blankNodeToQuads.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(createCanonicalMap(blankNodeToQuads));
    }

    /**
     * Canonicalizes an RDF {@link Model} according to the W3C RDFC-1.0 specification.
     *
     * @param model The model to canonicalize. Must not be null.
     * @return An unmodifiable, deterministically ordered list of canonical statements.
     * @throws NullPointerException If the model is null.
     * @throws SerializationException If permutation limits or recursion limits are exceeded.
     */
    public List<Statement> canonicalize(Model model) {
        Objects.requireNonNull(model, "Model cannot be null");
        return canonicalize(model.stream());
    }

    /**
     * Canonicalizes a stream of RDF {@link Statement}s according to the W3C RDFC-1.0 specification.
     *
     * @param statements Stream of statements to canonicalize.
     * @return Deterministically ordered list of canonical statements.
     */
    private List<Statement> canonicalize(Stream<Statement> statements) {
        List<Statement> stmtList = statements.toList();
        callsHashNDegreeQuads = 0;

        // Step 1: Create a mapping of blank nodes to their associated statements
        Map<String, List<Statement>> blankNodeToQuads = createBNodeToQuadsMap(stmtList);

        if (blankNodeToQuads.isEmpty()) {
            return stmtList.stream()
                    .sorted(Comparator.comparing(StatementUtils::toNQuad))
                    .toList();
        }

        // Step 2: Generate canonical replacement mapping
        Map<String, String> canonicalReplacementMap = createCanonicalMap(blankNodeToQuads);

        // Step 3: Apply the replacement and sort the final statements
        return replaceBlankNodesAndSort(stmtList, canonicalReplacementMap);
    }

    /**
     * Builds a map associating each blank node identifier with all statements in which it appears.
     * Blank nodes appearing in subject, object, or named graph context positions are indexed.
     *
     * @param statements The statements to index.
     * @return A map of blank node IDs to their referencing statements.
     */
    private Map<String, List<Statement>> createBNodeToQuadsMap(List<Statement> statements) {
        Map<String, List<Statement>> blankNodeToQuads = new LinkedHashMap<>();

        for (Statement stmt : statements) {
            if (stmt == null) continue;

            if (StatementUtils.isBlankNode(stmt.getSubject())) {
                String blankNodeId = StatementUtils.getBlankNodeId(stmt.getSubject());
                blankNodeToQuads.computeIfAbsent(blankNodeId, k -> new ArrayList<>()).add(stmt);
            }

            if (StatementUtils.isBlankNode(stmt.getObject())) {
                String blankNodeId = StatementUtils.getBlankNodeId(stmt.getObject());
                blankNodeToQuads.computeIfAbsent(blankNodeId, k -> new ArrayList<>()).add(stmt);
            }

            if (stmt.getContext() != null && StatementUtils.isBlankNode(stmt.getContext())) {
                String blankNodeId = StatementUtils.getBlankNodeId(stmt.getContext());
                blankNodeToQuads.computeIfAbsent(blankNodeId, k -> new ArrayList<>()).add(stmt);
            }
        }

        return blankNodeToQuads;
    }

    /**
     * Executes the W3C RDFC-1.0 canonical mapping algorithm (Section 4.4).
     *
     * <ol>
     *   <li>Computes 1-degree hashes for all blank nodes.</li>
     *   <li>Issues canonical identifiers (e.g. _:c14n0) for nodes with unique 1-degree hashes in ascending order.</li>
     *   <li>Resolves ties and symmetric subgraphs for multi-node groups using N-degree recursive quad hashing.</li>
     * </ol>
     *
     * @param bnodeToQuads Map of blank node identifiers to their associated quads.
     * @return A mapping from original blank node IDs to their canonical identifiers.
     */
    @SuppressWarnings("java:S3776")
    private Map<String, String> createCanonicalMap(Map<String, List<Statement>> bnodeToQuads) {
        CanonicalIssuer canonicalIssuer = new CanonicalIssuer(SerializationConstants.C14N);

        // 4.4.3 Step 3: Calculate first-degree hashes for all blank nodes
        Map<String, String> firstDegreeHashes = new LinkedHashMap<>();
        Map<String, List<String>> hashToBlankNodes = new TreeMap<>();

        for (String bnode : bnodeToQuads.keySet()) {
            String hash = hashFirstDegreeQuads(bnode, bnodeToQuads);
            firstDegreeHashes.put(bnode, hash);
            hashToBlankNodes.computeIfAbsent(hash, k -> new ArrayList<>()).add(bnode);
        }

        // 4.4.3 Step 4: Issue canonical identifiers for blank nodes with a unique first degree hash
        // Hashes are processed in sorted order (TreeMap)
        List<String> multiNodeHashes = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : hashToBlankNodes.entrySet()) {
            if (entry.getValue().size() == 1) {
                canonicalIssuer.issue(entry.getValue().get(0));
            } else {
                multiNodeHashes.add(entry.getKey());
            }
        }

        // 4.4.3 Step 5: Process multi-node groups using N-degree hashing
        NDegreeContext context = new NDegreeContext(bnodeToQuads, firstDegreeHashes, canonicalIssuer);
        for (String hash : multiNodeHashes) {
            List<HashNDegreeResult> hashPathList = new ArrayList<>();

            for (String bnode : hashToBlankNodes.get(hash)) {
                if (canonicalIssuer.hasIssued(bnode)) {
                    continue;
                }

                CanonicalIssuer tempIssuer = new CanonicalIssuer("b");
                tempIssuer.issue(bnode);

                HashNDegreeResult result = hashNDegreeQuads(tempIssuer, bnode, 0, context);
                hashPathList.add(result);
            }

            hashPathList.sort(Comparator.comparing(HashNDegreeResult::hash));

            for (HashNDegreeResult result : hashPathList) {
                for (String existingId : result.issuer().getIssuedBlankNodes()) {
                    canonicalIssuer.issue(existingId);
                }
            }
        }

        return canonicalIssuer.getIssuedMap();
    }

    /**
     * Computes the 1-degree hash for a specific blank node (W3C Section 4.6).
     * Replaces the target blank node with {@code _:a} and all other blank nodes with {@code _:z},
     * sorts the serialized quads lexicographically, and hashes the concatenated result.
     *
     * @param blankNode        The identifier of the blank node to hash.
     * @param blankNodeToQuads The map of blank nodes to their referencing quads.
     * @return Hexadecimal string representing the 1-degree hash.
     */
    private String hashFirstDegreeQuads(String blankNode, Map<String, List<Statement>> blankNodeToQuads) {
        List<Statement> quads = blankNodeToQuads.get(blankNode);
        if (quads == null) {
            return hash("");
        }

        List<String> nquads = new ArrayList<>();
        for (Statement quad : quads) {
            nquads.add(serializeQuad(quad, blankNode));
        }

        Collections.sort(nquads);
        return hash(String.join("\n", nquads) + "\n");
    }

    /**
     * Serializes a single quad into canonical N-Quads syntax for 1-degree hashing.
     *
     * @param quad                 The quad statement.
     * @param referenceBlankNodeId The blank node identifier being evaluated (mapped to {@code _:a}).
     * @return Formatted N-Quad string.
     */
    private String serializeQuad(Statement quad, String referenceBlankNodeId) {
        String subject = getNodeString(quad.getSubject(), referenceBlankNodeId);
        String predicate = getNodeString(quad.getPredicate(), referenceBlankNodeId);
        String object = getNodeString(quad.getObject(), referenceBlankNodeId);
        String graph = (quad.getContext() != null) ? " " + getNodeString(quad.getContext(), referenceBlankNodeId) : "";

        return subject + " " + predicate + " " + object + graph + " .";
    }

    /**
     * Converts an RDF value to its canonical N-Quads representation during 1-degree quad hashing.
     *
     * @param value                The value to format.
     * @param referenceBlankNodeId The blank node being evaluated.
     * @return {@code _:a} if the value matches the reference blank node, {@code _:z} if it is another blank node,
     *         or the canonical string representation of the literal or IRI.
     */
    private String getNodeString(Value value, String referenceBlankNodeId) {
        if (StatementUtils.isBlankNode(value)) {
            String id = StatementUtils.getBlankNodeId(value);
            return id.equals(referenceBlankNodeId) ? "_:a" : "_:z";
        }
        return StatementUtils.serializeForComparison(value);
    }

    /**
     * Result record containing the cryptographic hash and temporary issuer from an N-degree hash computation.
     *
     * @param hash   The computed N-degree hash.
     * @param issuer The temporary issuer reflecting the path explored.
     */
    private record HashNDegreeResult(String hash, CanonicalIssuer issuer) {}

    /**
     * Computes the N-degree hash for a blank node to break symmetries in isomorphic subgraphs (W3C Section 4.7).
     * Explores neighbor blank node permutations, prunes suboptimal paths, and recurses until all related nodes are labeled.
     *
     * @param issuer      The temporary issuer tracking issued identifiers along the current exploration path.
     * @param blankNodeId The blank node identifier being hashed.
     * @param depth       Current recursion depth.
     * @param context     The canonicalization execution context.
     * @return A {@link HashNDegreeResult} containing the deterministic hash and updated temporary issuer.
     */
    @SuppressWarnings("java:S3776")
    private HashNDegreeResult hashNDegreeQuads(CanonicalIssuer issuer, String blankNodeId, int depth, NDegreeContext context) {

        if (++callsHashNDegreeQuads > maxCallsHashNDegreeQuads) {
            throw new SerializationException(
                    "Permutation limit reached, too many permutations",
                    "RDFC10Canonicalizer"
            );
        }

        if (depth >= this.depthFactor * context.bnodeToQuads().size()) {
            throw new SerializationException(
                    "Depth factor reached, too many recursions",
                    "RDFC10Canonicalizer"
            );
        }

        CanonicalIssuer refIssuer = issuer;
        Map<String, List<String>> relatedHashToRelatedBNIdMap = new TreeMap<>();

        List<Statement> quads = context.bnodeToQuads().getOrDefault(blankNodeId, List.of());
        for (Statement quad : quads) {
            processQuadEntry(quad, refIssuer, blankNodeId, relatedHashToRelatedBNIdMap, "s", quad.getSubject(), context);
            processQuadEntry(quad, refIssuer, blankNodeId, relatedHashToRelatedBNIdMap, "o", quad.getObject(), context);
            if (quad.getContext() != null) {
                processQuadEntry(quad, refIssuer, blankNodeId, relatedHashToRelatedBNIdMap, "g", quad.getContext(), context);
            }
        }

        StringBuilder data = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : relatedHashToRelatedBNIdMap.entrySet()) {
            String hash = entry.getKey();
            data.append(hash);

            String chosenPath = "";
            CanonicalIssuer chosenIssuer = null;
            List<List<String>> permutations = permute(new ArrayList<>(entry.getValue()));

            for (List<String> permutation : permutations) {
                CanonicalIssuer issuerCopy = new CanonicalIssuer(refIssuer);
                StringBuilder currentPathBuilder = new StringBuilder();
                List<String> recursionList = new ArrayList<>();

                for (String relatedBNId : permutation) {
                    if (context.canonicalIssuer().hasIssued(relatedBNId)) {
                        currentPathBuilder.append("_:").append(context.canonicalIssuer().get(relatedBNId));
                    } else {
                        if (!issuerCopy.hasIssued(relatedBNId)) {
                            recursionList.add(relatedBNId);
                        }
                        currentPathBuilder.append("_:").append(issuerCopy.issue(relatedBNId));
                    }

                    if (!chosenPath.isEmpty() && currentPathBuilder.length() >= chosenPath.length()
                            && currentPathBuilder.toString().compareTo(chosenPath) > 0) {
                        break;
                    }
                }

                for (String relatedBNId : recursionList) {
                    HashNDegreeResult result = hashNDegreeQuads(issuerCopy, relatedBNId, depth + 1, context);

                    currentPathBuilder.append("_:").append(issuerCopy.issue(relatedBNId));
                    currentPathBuilder.append("<").append(result.hash()).append(">");
                    issuerCopy = result.issuer();

                    if (!chosenPath.isEmpty() && currentPathBuilder.length() >= chosenPath.length()
                            && currentPathBuilder.toString().compareTo(chosenPath) > 0) {
                        break;
                    }
                }

                String currentPath = currentPathBuilder.toString();
                if (chosenPath.isEmpty() || currentPath.compareTo(chosenPath) < 0) {
                    chosenPath = currentPath;
                    chosenIssuer = issuerCopy;
                }
            }

            data.append(chosenPath);
            refIssuer = chosenIssuer;
        }

        return new HashNDegreeResult(hash(data.toString()), refIssuer);
    }

    /**
     * Inspects a quad component to find adjacent blank nodes and computes their positional hashes.
     *
     * @param quad                       The quad statement.
     * @param issuer                     Current temporary issuer.
     * @param blankNodeId                The reference blank node being explored.
     * @param relatedHashToRelatedBNIdMap Destination map grouping adjacent blank nodes by related hash.
     * @param position                   Positional tag ({@code "s"} for subject, {@code "o"} for object, {@code "g"} for graph).
     * @param relatedNode                The term at the given position.
     * @param context                    The canonicalization context.
     */
    private void processQuadEntry(Statement quad, CanonicalIssuer issuer, String blankNodeId,
                                  Map<String, List<String>> relatedHashToRelatedBNIdMap,
                                  String position, Value relatedNode,
                                  NDegreeContext context) {
        if (relatedNode == null || !StatementUtils.isBlankNode(relatedNode)) {
            return;
        }

        String relatedBNId = StatementUtils.getBlankNodeId(relatedNode);
        if (!relatedBNId.equals(blankNodeId)) {
            String relatedHash = hashRelatedBlankNode(relatedBNId, quad, issuer, position, context);
            relatedHashToRelatedBNIdMap.computeIfAbsent(relatedHash, k -> new ArrayList<>()).add(relatedBNId);
        }
    }

    /**
     * Computes the hash of an adjacent blank node relative to a quad position and predicate (W3C Section 4.8).
     *
     * @param relatedBNId The adjacent blank node identifier.
     * @param quad        The connecting quad statement.
     * @param issuer      The current temporary issuer.
     * @param position    Positional tag ({@code "s"}, {@code "o"}, or {@code "g"}).
     * @param context     The canonicalization context.
     * @return Hexadecimal string representing the related blank node hash.
     */
    private String hashRelatedBlankNode(String relatedBNId, Statement quad, CanonicalIssuer issuer,
                                        String position, NDegreeContext context) {
        StringBuilder input = new StringBuilder();
        input.append(position);

        if (!"g".equals(position)) {
            input.append(StatementUtils.serializeForComparison(quad.getPredicate()));
        }

        if (context.canonicalIssuer().hasIssued(relatedBNId) || issuer.hasIssued(relatedBNId)) {
            String id = context.canonicalIssuer().hasIssued(relatedBNId)
                    ? context.canonicalIssuer().get(relatedBNId)
                    : issuer.get(relatedBNId);
            input.append("_:").append(id);
        } else {
            input.append(context.firstDegreeHashes().get(relatedBNId));
        }

        return hash(input.toString());
    }

    /**
     * Recursively computes all permutations of a given list.
     *
     * @param <T>      Element type.
     * @param original Input list.
     * @return List containing all permutations.
     */
    private <T> List<List<T>> permute(List<T> original) {
        if (original.isEmpty()) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }

        T firstElement = original.remove(0);
        List<List<T>> returnValue = new ArrayList<>();
        List<List<T>> permutations = permute(original);

        for (List<T> smallerPermutated : permutations) {
            for (int index = 0; index <= smallerPermutated.size(); index++) {
                List<T> temp = new ArrayList<>(smallerPermutated);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }

    /**
     * Replaces blank node identifiers with their canonical labels and sorts all statements lexicographically.
     *
     * @param statements   The original dataset statements.
     * @param canonicalMap Map from original blank node IDs to canonical identifiers (e.g. {@code _:c14n0}).
     * @return Sorted, canonical list of statements.
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
     * Computes the cryptographic digest of the given input string using the configured algorithm (SHA-256 or SHA-384).
     *
     * @param data The UTF-8 string data to hash.
     * @return Lowercase hexadecimal string of the digest.
     * @throws SerializationException If the cryptographic algorithm is unavailable.
     */
    private String hash(String data) {
        try {
            String algorithm = hashAlgorithm == RDFC10SerializerOptions.HashAlgorithm.SHA_384 ?
                    SerializationConstants.SHA_384 : SerializationConstants.SHA_256;
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SerializationException("Hash algorithm not available: " + e.getMessage(),
                    "RDFC10Canonicalizer", e);
        }
    }

    /**
     * Formats a byte array into a lowercase hexadecimal string.
     *
     * @param bytes Array of bytes.
     * @return Formatted hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format(SerializationConstants.HEX_FORMAT, b));
        }
        return result.toString();
    }

    /**
     * Manages deterministic blank node identifier issuance (e.g. {@code _:c14n0}, {@code _:c14n1} or temporary {@code _:b0}, {@code _:b1}).
     * Provides deep-copy capability for path isolation during permutation exploration.
     */
    public static class CanonicalIssuer {
        private final String prefix;
        private final Map<String, String> issued = new LinkedHashMap<>();
        private int counter = 0;

        /**
         * Constructs a new issuer with the specified prefix (e.g. {@code "c14n"} or {@code "b"}).
         *
         * @param prefix The identifier prefix.
         */
        public CanonicalIssuer(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Copy constructor creating an independent deep clone of the issuer state.
         *
         * @param other The issuer to copy.
         */
        public CanonicalIssuer(CanonicalIssuer other) {
            this.prefix = other.prefix;
            this.issued.putAll(other.issued);
            this.counter = other.counter;
        }

        /**
         * Issues a new canonical/temporary identifier for a blank node, or returns its existing one.
         *
         * @param bnodeId Original blank node ID.
         * @return Assigned canonical identifier without prefix {@code "_:"}.
         */
        public String issue(String bnodeId) {
            if (issued.containsKey(bnodeId)) {
                return issued.get(bnodeId);
            }
            String id = prefix + counter++;
            issued.put(bnodeId, id);
            return id;
        }

        /**
         * Checks if an identifier has already been issued for the given blank node.
         *
         * @param bnodeId Original blank node ID.
         * @return true if already issued, false otherwise.
         */
        public boolean hasIssued(String bnodeId) {
            return issued.containsKey(bnodeId);
        }

        /**
         * Retrieves the issued identifier for a blank node.
         *
         * @param bnodeId Original blank node ID.
         * @return The issued identifier or null.
         */
        public String get(String bnodeId) {
            return issued.get(bnodeId);
        }

        /**
         * Returns the list of blank node IDs in the order they were issued.
         *
         * @return List of original blank node IDs.
         */
        public List<String> getIssuedBlankNodes() {
            return new ArrayList<>(issued.keySet());
        }

        /**
         * Returns an unmodifiable map of all issued blank node mappings.
         *
         * @return Map of original ID to issued ID.
         */
        public Map<String, String> getIssuedMap() {
            return Collections.unmodifiableMap(issued);
        }
    }
}
