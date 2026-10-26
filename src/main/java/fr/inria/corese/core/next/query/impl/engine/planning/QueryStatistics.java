package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.api.model.StorageStatistics;

/**
 * Supplies optional planning statistics without opening statement cursors.
 * Counts describe the state visible to the producer's storage session. They are
 * estimates for planning, not a snapshot or a guarantee about later execution.
 *
 * @author Fuqi Song, WImmics Inria I3S
 */
public interface QueryStatistics {

    /** Sentinel for unavailable statistics; zero is a valid empty-pattern count. */
    long UNKNOWN_CARDINALITY = -1;

    /**
     * Estimates an unbound triple pattern in its active dataset and graph scope.
     * Implementations must not open a cursor or a transaction for this operation.
     *
     * @param query query supplying the active dataset
     * @param graphNode enclosing GRAPH node, or {@code null} for the default graph
     * @param edge triple pattern whose constants constrain the estimate
     * @return nonnegative cardinality, or {@link #UNKNOWN_CARDINALITY} if unavailable
     */
    default long estimateCardinality(QueryPlan query, Node graphNode, Edge edge) {
        return UNKNOWN_CARDINALITY;
    }

    /**
     * Returns an optional total or distinct-term count for the heuristic model.
     *
     * @param type total or distinct subject/predicate/object count selector
     * @return count, or {@code -1} when unavailable without collecting statistics
     */
    default int getSize(int type) {
        return -1;
    }

    /**
     * Returns an optional position-specific count for the heuristic model.
     *
     * @param node expression node to estimate
     * @param type subject/predicate/object count selector
     * @return count, or {@code -1} when unavailable
     */
    default int getCount(QueryPlanNode node, int type) {
        return -1;
    }

    /**
     * Returns statistics for a specific predicate, or {@code null} if unavailable.
     *
     * @param query query supplying the active dataset
     * @param graphNode enclosing GRAPH node, or {@code null} for the default graph
     * @param predicate the predicate IRI to inspect
     * @return predicate statistics or {@code null} if unavailable
     */
    default PredicateStatistics getPredicateStatistics(QueryPlan query, Node graphNode, IRI predicate) {
        return null;
    }

    /**
     * Returns global storage statistics, or {@code null} if unavailable.
     *
     * @return storage statistics or {@code null} if unavailable
     */
    default StorageStatistics getStorageStatistics() {
        return null;
    }
}
