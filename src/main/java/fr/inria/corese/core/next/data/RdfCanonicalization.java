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

    private RdfCanonicalization() {
    }

    /**
     * Canonicalizes a model with the standard RDFC 1.0 configuration.
     *
     * @param model model to canonicalize
     * @return canonical, lexicographically ordered statements
     */
    public static List<Statement> canonicalize(Model model) {
        RDFC10SerializerOptions options = RDFC10SerializerOptions.defaultConfig();
        return canonicalize(model, options.getPermutationLimit());
    }

    /**
     * Canonicalizes a model while limiting Hash N-Degree Quads calls.
     *
     * @param model model to canonicalize
     * @param maxCalls maximum permitted recursive Hash N-Degree Quads calls
     * @return canonical, lexicographically ordered statements
     */
    public static List<Statement> canonicalize(Model model, int maxCalls) {
        Objects.requireNonNull(model, "model");
        RDFC10SerializerOptions options = RDFC10SerializerOptions.defaultConfig();
        return new RDFC10Canonicalizer(options.getHashAlgorithm(), maxCalls, Values.factory())
                .canonicalize(model);
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
