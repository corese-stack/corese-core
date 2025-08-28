package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.option.CanonicalOption;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.io.serialization.util.StatementUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation of the RDFC-1.0 canonicalization algorithm as specified by W3C.
 */
public class Rdfc10CanonicalizerImpl implements Rdfc10Canonicalizer {

    private final CanonicalOption.HashAlgorithm hashAlgorithm;
    private final int maxCallsHashNDegreeQuads;
    private final ValueFactory valueFactory;
    private final StatementUtils statementUtils;
    private int callsHashNDegreeQuads = 0;

    public Rdfc10CanonicalizerImpl(CanonicalOption.HashAlgorithm hashAlgorithm, int maxCalls, ValueFactory valueFactory) {
        this.hashAlgorithm = hashAlgorithm;
        this.maxCallsHashNDegreeQuads = maxCalls;
        this.valueFactory = valueFactory;
        this.statementUtils = new StatementUtils(valueFactory);
    }


    @Override
    public List<Statement> canonicalize(Model model) {
        return canonicalize(model.stream());
    }


    /**
     * Internal canonicalization method
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
     * Add validation in createBNodeToQuadsMap
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
     * Create canonical replacement map
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
     * Hash First Degree Quads algorithm
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
     * Hash N-Degree Quads algorithm
     */
    private String hashNDegreQuads(String identifier, Map<String, Set<Statement>> blankNodeToQuads,
                                    Map<String, String> canonicalIssuer, TemporaryIssuer issuer) {

        if (++callsHashNDegreeQuads > maxCallsHashNDegreeQuads) {
            throw new SerializationException("Maximum calls to Hash N-Degree Quads exceeded: " + maxCallsHashNDegreeQuads, "Rdfc10CanonicalizerImpl");
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
     * Convert a quad to N-Quad format for hashing
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

        sb.append(SerializationConstants.SPACE_POINT);
        return sb.toString();
    }

    /**
     * Get related blank nodes from a quad
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
     * Improved blank node replacement with validation
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
     * Utility methods - removed duplicates, now using StatementUtils
     */
    private String hash(String data) {
        try {
            String algorithm = hashAlgorithm == CanonicalOption.HashAlgorithm.SHA_384 ? SerializationConstants.SHA_384 : SerializationConstants.SHA_256;
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SerializationException("Hash algorithm not available: " + e.getMessage(), "Rdfc10CanonicalizerImpl", e);
        } catch (Exception e) {
            throw new SerializationException("Hash computation failed for data: " + data, "Rdfc10CanonicalizerImpl", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format(SerializationConstants.HEX_FORMAT, b));
        }
        return result.toString();
    }

    /**
     * Helper class for temporary identifier issuing during canonicalization
     */
    private static class TemporaryIssuer {
        private Map<String, String> issued = new HashMap<>();
        private int counter = 0;

        public String issue(String identifier) {
            if (!issued.containsKey(identifier)) {
                issued.put(identifier, SerializationConstants.CANONICAL_BNODE_PREFIX + counter++);
            }
            return issued.get(identifier);
        }

        public boolean hasIssued(String identifier) {
            return issued.containsKey(identifier);
        }

        public TemporaryIssuer copy() {
            TemporaryIssuer copy = new TemporaryIssuer();
            copy.issued = new HashMap<>(this.issued);
            copy.counter = this.counter;
            return copy;
        }
    }
}
