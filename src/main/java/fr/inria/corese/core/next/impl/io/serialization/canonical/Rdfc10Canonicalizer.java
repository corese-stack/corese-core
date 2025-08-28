package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Model;

import java.util.List;

/**
 * Interface for a component that performs RDFC-1.0 canonicalization.
 * This component is responsible for re-labeling blank nodes and sorting statements
 * according to the RDFC-1.0 specification.
 */
public interface Rdfc10Canonicalizer {
    /**
     * Canonicalizes a stream of RDF statements from a given model.
     * The implementation will handle all steps of the RDFC-10 algorithm,
     * including dataset normalization, blank node identification, and
     * deterministic sorting.
     *
     * @param model The input model to canonicalize.
     * @return A list of canonicalized and sorted statements.
     */
    List<Statement> canonicalize(Model model);
}