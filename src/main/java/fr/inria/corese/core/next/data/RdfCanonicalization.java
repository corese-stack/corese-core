package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10Canonicalizer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10SerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.StatementUtils;

import java.util.List;
import java.util.Objects;

/** Public entry point for RDF Dataset Canonicalization 1.0 operations. */
public final class RdfCanonicalization {

    /** Hash algorithms defined by RDF Dataset Canonicalization 1.0. */
    public enum HashAlgorithm {
        SHA_256,
        SHA_384
    }

    private RdfCanonicalization() {
    }

    /**
     * Canonicalizes a model with the standard RDFC 1.0 configuration.
     *
     * @param model model to canonicalize
     * @return canonical, lexicographically ordered statements
     */
    public static List<Statement> canonicalize(Model model) {
        return canonicalize(model, RDFC10SerializerOptions.defaultConfig());
    }

    /**
     * Canonicalizes a model while limiting Hash N-Degree Quads calls.
     *
     * @param model model to canonicalize
     * @param maxCalls maximum permitted recursive Hash N-Degree Quads calls
     * @return canonical, lexicographically ordered statements
     */
    public static List<Statement> canonicalize(Model model, int maxCalls) {
        return canonicalize(model, RDFC10SerializerOptions.builder().permutationLimit(maxCalls).build());
    }

    /**
     * Canonicalizes a model with a specified hash algorithm.
     *
     * @param model model to canonicalize
     * @param hashAlgorithm hash algorithm to use (SHA-256 or SHA-384)
     * @return canonical, lexicographically ordered statements
     */
    public static List<Statement> canonicalize(Model model, HashAlgorithm hashAlgorithm) {
        Objects.requireNonNull(hashAlgorithm, "hashAlgorithm");
        RDFC10SerializerOptions.HashAlgorithm internalAlgorithm = switch (hashAlgorithm) {
            case SHA_256 -> RDFC10SerializerOptions.HashAlgorithm.SHA_256;
            case SHA_384 -> RDFC10SerializerOptions.HashAlgorithm.SHA_384;
        };
        return canonicalize(model, RDFC10SerializerOptions.builder().hashAlgorithm(internalAlgorithm).build());
    }

    /**
     * Canonicalizes a model with custom RDFC 1.0 options.
     *
     * @param model model to canonicalize
     * @param options custom RDFC 1.0 options
     * @return canonical, lexicographically ordered statements
     */
    private static List<Statement> canonicalize(Model model, RDFC10SerializerOptions options) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(options, "options");
        return new RDFC10Canonicalizer(
                options.getHashAlgorithm(),
                options.getPermutationLimit(),
                options.getDepthFactor(),
                Values.factory()
        ).canonicalize(model);
    }

    /**
     * Formats one statement as an N-Quads line, as used by RDFC 1.0 ordering.
     *
     * @param statement statement to format
     * @return N-Quads representation
     */
    public static String toNQuad(Statement statement) {
        return StatementUtils.toNQuad(Objects.requireNonNull(statement, "statement"));
    }
}
