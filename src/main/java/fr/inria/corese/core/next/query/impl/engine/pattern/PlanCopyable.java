package fr.inria.corese.core.next.query.impl.engine.pattern;

/**
 * Internal graph-copy contract for compiled operators and execution-local adapters.
 * Implementations register the new object before copying links that may form cycles.
 * Immutable RDF values and syntax nodes may be shared; mutable containers may not.
 *
 * @param <T> copied implementation type
 */
public interface PlanCopyable<T> {
    /**
     * Copies this object in an identity-preserving graph-copy operation.
     *
     * @param copies current copy operation
     * @return independent copy registered with the supplied operation
     */
    T copyPlan(PlanCopy copies);
}
