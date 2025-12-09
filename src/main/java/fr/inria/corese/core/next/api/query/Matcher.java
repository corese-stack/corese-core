package fr.inria.corese.core.next.api.query;

/**
 * High-level configuration for value and triple matching during SPARQL evaluation.
 *
 * This interface abstracts over the low-level KGRAM {@code Matcher}.
 */
public interface Matcher {
    /**
     * Matching modes for RDF term comparison.
     *
     * These correspond loosely to the KGRAM integer modes.
     */
    enum Mode {
        UNDEFINED,  // internal / default
        STRICT,     // strict equality
        SUBSUME,    // exploit type subsumption (RDFS/OWL)
        GENERAL,    // allow generalization
        MIX,        // subsume + generalize
        RELAX,      // accept any type
        INFERENCE   // exploit rule-based inference
    }

    /**
     * @return the current matching mode.
     */
    Mode getMode();

    /**
     * Sets the matching mode for this matcher.
     *
     * @param mode the mode to use
     */
    void setMode(Mode mode);
}
