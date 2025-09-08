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
 * This class is responsible for deterministically re-labeling blank nodes and
 * sorting all RDF statements to produce a canonical representation of a dataset.
 */
public class Rdfc10Canonicalizer {

    private final CanonicalOption.HashAlgorithm hashAlgorithm;
    private final int maxCallsHashNDegreeQuads;
    private final StatementUtils statementUtils;
    private int callsHashNDegreeQuads = 0;

    /**
     * Constructs a new Rdfc10Canonicalizer.
     *
     * @param hashAlgorithm The hashing algorithm to use, e.g., SHA-256 or SHA-384.
     * @param maxCalls The maximum number of recursive calls to the Hash N-Degree Quads algorithm
     * to prevent infinite loops on complex cyclic graphs.
     * @param valueFactory The factory for creating RDF values, used by StatementUtils.
     */
    public Rdfc10Canonicalizer(CanonicalOption.HashAlgorithm hashAlgorithm, int maxCalls, ValueFactory valueFactory) {
        this.hashAlgorithm = hashAlgorithm;
        this.maxCallsHashNDegreeQuads = maxCalls;
        this.statementUtils = new StatementUtils(valueFactory);
    }


    /**
     * Canonicalizes all statements within a given {@link Model}.
     * This is the main entry point for the canonicalization process.
     *
     * @param model The input model to canonicalize.
     * @return A list of canonicalized and sorted statements.
     */
    public List<Statement> canonicalize(Model model) {
        return canonicalize(model.stream());
    }


    /**
     * Internal canonicalization method that processes a stream of statements.
     * This method handles all the steps of the RDFC-1.0 algorithm, including:
     * <ol>
     * <li>Creating a map of blank nodes to their associated quads.</li>
     * <li>Generating a canonical replacement map for blank nodes using
     * the Hash First Degree and Hash N-Degree Quads algorithms.</li>
     * <li>Replacing the blank nodes in the statements.</li>
     * <li>Sorting the final list of statements.</li>
     * </ol>
     * @param statements A stream of statements to canonicalize.
     * @return A list of canonicalized and sorted statements.
     */
    private List<Statement> canonicalize(Stream<Statement> statements) {
        List<Statement> stmtList = statements.toList();

        callsHashNDegreeQuads = 0;

        Map<String, Set<Statement>> blankNodeToQuads = createBNodeToQuadsMap(stmtList);

        if (blankNodeToQuads.isEmpty()) {
            return stmtList.stream()
                    .sorted((s1, s2) -> StatementUtils.toNQuad(s1).compareTo(StatementUtils.toNQuad(s2)))
                    .toList();
        }

        Map<String, String> canonicalReplacementMap = createCanonicalMap(blankNodeToQuads);

        return replaceBlankNodesAndSort(stmtList, canonicalReplacementMap);
    }

    /**
     * Creates a map where each blank node identifier is a key, and its value is a set
     * of all statements (quads) in which the blank node appears as a subject, object, or graph URI.
     *
     * @param statements The list of statements to process.
     * @return A map linking blank node identifiers to their associated quads.
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
     * Performs the core canonicalization logic to create a map of blank node
     * replacements. This method uses the "Hash First Degree Quads" and "Hash N-Degree Quads"
     * algorithms to determine a canonical identifier for each blank node.
     *
     * @param blankNodeToQuads A map of blank nodes to their associated quads.
     * @return A map from old blank node identifiers to new canonical ones.
     */
    private Map<String, String> createCanonicalMap(Map<String, Set<Statement>> blankNodeToQuads) {
        Map<String, String> canonicalIssuer = new HashMap<>();
        int canonicalCounter = 0;

        Map<String, Set<String>> hashToBlankNodes = new HashMap<>();

        for (String blankNode : blankNodeToQuads.keySet()) {
            String hash = hashFirstDegreeQuads(blankNode, blankNodeToQuads);
            hashToBlankNodes.computeIfAbsent(hash, k -> new HashSet<>()).add(blankNode);
        }

        List<String> sortedHashes = new ArrayList<>(hashToBlankNodes.keySet());
        Collections.sort(sortedHashes);

        for (String hash : sortedHashes) {
            Set<String> blankNodes = hashToBlankNodes.get(hash);

            if (blankNodes.size() == 1) {
                String blankNode = blankNodes.iterator().next();
                canonicalIssuer.put(blankNode, SerializationConstants.C14N + canonicalCounter++);
            } else {
                Map<String, String> nDegreeHashes = new HashMap<>();

                for (String blankNode : blankNodes) {
                    if (!canonicalIssuer.containsKey(blankNode)) {
                        TemporaryIssuer temporaryIssuer = new TemporaryIssuer();
                        String nDegreeHash = hashNDegreQuads(blankNode, blankNodeToQuads, canonicalIssuer, temporaryIssuer);
                        nDegreeHashes.put(blankNode, nDegreeHash);
                    }
                }

                List<Map.Entry<String, String>> sortedEntries = nDegreeHashes.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .toList();

                for (Map.Entry<String, String> entry : sortedEntries) {
                    if (!canonicalIssuer.containsKey(entry.getKey())) {
                        canonicalIssuer.put(entry.getKey(), SerializationConstants.C14N + canonicalCounter++);
                    }
                }
            }
        }

        return canonicalIssuer;
    }

    /**
     * Implements the "Hash First Degree Quads" algorithm from the RDFC-1.0 specification.
     * This method computes a hash for a blank node based on the canonical representation
     * of all quads in which it appears, replacing the blank node itself with a placeholder.
     *
     * @param blankNode The blank node identifier to hash.
     * @param blankNodeToQuads The map of blank nodes to their associated quads.
     * @return A string representing the hash.
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
     * Implements the "Hash N-Degree Quads" algorithm from the RDFC-1.0 specification.
     * This is a recursive algorithm that resolves permutations of blank nodes with
     * the same first-degree hash by considering their related blank nodes and recursively
     * hashing their graph contexts.
     *
     * @param identifier The blank node identifier to hash.
     * @param blankNodeToQuads The map of blank nodes to their associated quads.
     * @param canonicalIssuer The map of blank nodes that have already been assigned a canonical ID.
     * @param issuer The temporary identifier issuer for the current permutation path.
     * @return A string representing the hash.
     * @throws SerializationException if the maximum number of recursive calls is exceeded.
     */
    private String hashNDegreQuads(String identifier, Map<String, Set<Statement>> blankNodeToQuads,
                                   Map<String, String> canonicalIssuer, TemporaryIssuer issuer) {

        if (++callsHashNDegreeQuads > maxCallsHashNDegreeQuads) {
            throw new SerializationException("Maximum calls to Hash N-Degree Quads exceeded: " + maxCallsHashNDegreeQuads, "Rdfc10Canonicalizer");
        }

        Map<String, Set<String>> hashToRelatedBlankNodes = new HashMap<>();
        Set<Statement> quads = blankNodeToQuads.get(identifier);

        for (Statement quad : quads) {
            Set<String> relatedBlankNodes = getRelatedBlankNodes(quad, identifier);

            for (String relatedBlankNode : relatedBlankNodes) {
                String hash;
                if (canonicalIssuer.containsKey(relatedBlankNode)) {
                    hash = canonicalIssuer.get(relatedBlankNode);
                } else if (issuer.hasIssued(relatedBlankNode)) {
                    hash = issuer.issue(relatedBlankNode);
                } else {
                    hash = hashFirstDegreeQuads(relatedBlankNode, blankNodeToQuads);
                }
                hashToRelatedBlankNodes.computeIfAbsent(hash, k -> new HashSet<>()).add(relatedBlankNode);
            }
        }

        StringBuilder dataToHash = new StringBuilder();
        List<String> sortedHashes = new ArrayList<>(hashToRelatedBlankNodes.keySet());
        Collections.sort(sortedHashes);

        for (String hash : sortedHashes) {
            dataToHash.append(hash);
            Set<String> blankNodeList = hashToRelatedBlankNodes.get(hash);

            if (blankNodeList.size() > 1) {
                List<String> hashPathList = new ArrayList<>();

                for (String relatedBlankNode : blankNodeList) {
                    if (canonicalIssuer.containsKey(relatedBlankNode)) {
                        hashPathList.add(canonicalIssuer.get(relatedBlankNode));
                    } else {
                        TemporaryIssuer tempIssuer = issuer.copy();
                        tempIssuer.issue(relatedBlankNode);
                        String hashPath = hashNDegreQuads(relatedBlankNode, blankNodeToQuads, canonicalIssuer, tempIssuer);
                        hashPathList.add(hashPath);
                    }
                }

                Collections.sort(hashPathList);
                dataToHash.append(String.join(SerializationConstants.EMPTY_STRING, hashPathList));
            } else {
                String blankNode = blankNodeList.iterator().next();
                if (canonicalIssuer.containsKey(blankNode)) {
                    dataToHash.append(canonicalIssuer.get(blankNode));
                } else {
                    dataToHash.append(issuer.issue(blankNode));
                }
            }
        }

        return hash(dataToHash.toString());
    }


    /**
     * Converts a single quad to a canonical N-Quad string representation for hashing,
     * replacing the specified blank node with a placeholder.
     *
     * @param quad The statement to convert.
     * @param blankNode The blank node to replace.
     * @param replacement The placeholder string to use for the blank node.
     * @return A canonical N-Quad string.
     */
    private String quadToNQuad(Statement quad, String blankNode, String replacement) {
        StringBuilder sb = new StringBuilder();

        if (StatementUtils.isBlankNode(quad.getSubject()) && StatementUtils.getBlankNodeId(quad.getSubject()).equals(blankNode)) {
            sb.append(replacement);
        } else {
            sb.append(StatementUtils.serializeForComparison(quad.getSubject()));
        }
        sb.append(SerializationConstants.SPACE);

        sb.append(StatementUtils.serializeForComparison(quad.getPredicate()));
        sb.append(SerializationConstants.SPACE);

        if (StatementUtils.isBlankNode(quad.getObject()) && StatementUtils.getBlankNodeId(quad.getObject()).equals(blankNode)) {
            sb.append(replacement);
        } else {
            sb.append(StatementUtils.serializeForComparison(quad.getObject()));
        }

        if (quad.getContext() != null) {
            sb.append(SerializationConstants.SPACE);
            if (StatementUtils.isBlankNode(quad.getContext()) && StatementUtils.getBlankNodeId(quad.getContext()).equals(blankNode)) {
                sb.append(replacement);
            } else {
                sb.append(StatementUtils.serializeForComparison(quad.getContext()));
            }
        }

        sb.append(SerializationConstants.SPACE).append(SerializationConstants.POINT);
        return sb.toString();
    }

    /**
     * Finds all blank nodes in a given quad that are related to (but not the same as)
     * the excluded blank node.
     *
     * @param quad The quad to inspect.
     * @param excludeBlankNode The blank node to exclude from the results.
     * @return A set of blank node identifiers related to the excluded blank node.
     */
    private Set<String> getRelatedBlankNodes(Statement quad, String excludeBlankNode) {
        Set<String> relatedBlankNodes = new HashSet<>();

        if (StatementUtils.isBlankNode(quad.getSubject())) {
            String id = StatementUtils.getBlankNodeId(quad.getSubject());
            if (!id.equals(excludeBlankNode)) {
                relatedBlankNodes.add(id);
            }
        }

        if (StatementUtils.isBlankNode(quad.getObject())) {
            String id = StatementUtils.getBlankNodeId(quad.getObject());
            if (!id.equals(excludeBlankNode)) {
                relatedBlankNodes.add(id);
            }
        }

        if (quad.getContext() != null && StatementUtils.isBlankNode(quad.getContext())) {
            String id = StatementUtils.getBlankNodeId(quad.getContext());
            if (!id.equals(excludeBlankNode)) {
                relatedBlankNodes.add(id);
            }
        }

        return relatedBlankNodes;
    }

    /**
     * Replaces the old blank node identifiers in a list of statements with their new
     * canonical identifiers and then sorts the resulting statements.
     *
     * @param statements The list of statements to process.
     * @param replacementMap The map from old blank node IDs to new canonical IDs.
     * @return A sorted list of statements with canonical blank node IDs.
     */
    private List<Statement> replaceBlankNodesAndSort(List<Statement> statements, Map<String, String> replacementMap) {
        return statements.stream()
                .map(stmt -> {
                    Statement replaced = statementUtils.replaceBlankNodes(stmt, replacementMap);
                    if (replaced == null) {
                        throw new IllegalStateException("Failed to replace blank nodes in statement: " + stmt);
                    }
                    return replaced;
                })
                .sorted(Comparator.comparing(StatementUtils::toNQuad))
                .toList();
    }

    /**
     * Computes a cryptographic hash of the given data string using the configured
     * hash algorithm (SHA-256 or SHA-384).
     *
     * @param data The data string to hash.
     * @return A hexadecimal string representation of the hash.
     * @throws SerializationException if the hash algorithm is not available or if hashing fails.
     */
    private String hash(String data) {
        try {
            String algorithm = hashAlgorithm == CanonicalOption.HashAlgorithm.SHA_384 ? SerializationConstants.SHA_384 : SerializationConstants.SHA_256;
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SerializationException("Hash algorithm not available: " + e.getMessage(), "Rdfc10Canonicalizer", e);
        } catch (Exception e) {
            throw new SerializationException("Hash computation failed for data: " + data, "Rdfc10Canonicalizer", e);
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
     * Helper class for temporary identifier issuing during canonicalization.
     * This is used during the recursive "Hash N-Degree Quads" algorithm to
     * assign unique, temporary blank node identifiers within a single path.
     */
    private static class TemporaryIssuer {
        private Map<String, String> issued = new HashMap<>();
        private int counter = 0;

        /**
         * Issues a new temporary identifier for the given blank node identifier.
         * If an identifier has already been issued for this blank node, it returns the existing one.
         *
         * @param identifier The blank node identifier to issue an ID for.
         * @return A temporary canonical identifier.
         */
        public String issue(String identifier) {
            if (!issued.containsKey(identifier)) {
                issued.put(identifier, SerializationConstants.CANONICAL_BNODE_PREFIX + counter++);
            }
            return issued.get(identifier);
        }

        /**
         * Checks if a temporary identifier has already been issued for the given blank node.
         * @param identifier The blank node identifier to check.
         * @return {@code true} if an identifier has been issued, {@code false} otherwise.
         */
        public boolean hasIssued(String identifier) {
            return issued.containsKey(identifier);
        }

        /**
         * Creates a copy of the current TemporaryIssuer instance. This is crucial for
         * the recursive hashing algorithm to explore different permutation paths independently.
         * @return A new instance with the same state.
         */
        public TemporaryIssuer copy() {
            TemporaryIssuer copy = new TemporaryIssuer();
            copy.issued = new HashMap<>(this.issued);
            copy.counter = this.counter;
            return copy;
        }
    }
}
