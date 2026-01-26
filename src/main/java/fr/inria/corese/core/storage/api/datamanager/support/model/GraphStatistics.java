package fr.inria.corese.core.storage.api.datamanager.support.model;

import java.util.Objects;

/**
 * Statistics about a graph's content and structure.
 */
public class GraphStatistics {

    private final long edgeCount;
    private final long nodeCount;
    private final long predicateCount;
    private final long contextCount;
    private final long subjectCount;
    private final long objectCount;
    private final long literalCount;

    /**
     * Private constructor - use Builder.
     */
    private GraphStatistics(Builder builder) {
        this.edgeCount = builder.edgeCount;
        this.nodeCount = builder.nodeCount;
        this.predicateCount = builder.predicateCount;
        this.contextCount = builder.contextCount;
        this.subjectCount = builder.subjectCount;
        this.objectCount = builder.objectCount;
        this.literalCount = builder.literalCount;
    }

    /**
     * Returns the total number of edges (triples) in the graph.
     *
     * @return Edge count
     */
    public long getEdgeCount() {
        return edgeCount;
    }

    /**
     * Returns the total number of unique nodes (subjects and objects).
     *
     * @return Node count
     */
    public long getNodeCount() {
        return nodeCount;
    }

    /**
     * Returns the number of unique predicates.
     *
     * @return Predicate count
     */
    public long getPredicateCount() {
        return predicateCount;
    }

    /**
     * Returns the number of named graphs (contexts).
     *
     * @return Context count
     */
    public long getContextCount() {
        return contextCount;
    }


    /**
     * Calculates graph density: edges / (nodes * predicates).
     *
     * @return Graph density between 0.0 and 1.0, or 0.0 if calculation not possible
     */
    public double getDensity() {
        if (nodeCount == 0 || predicateCount == 0) {
            return 0.0;
        }
        long maxPossibleEdges = nodeCount * predicateCount;
        return (double) edgeCount / maxPossibleEdges;
    }


    /**
     * Calculates average degree (number of edges) per node.
     *
     * @return Average degree per node, or 0.0 if no nodes
     */
    public double getAverageDegree() {
        if (nodeCount == 0) {
            return 0.0;
        }
        // Each edge connects to 2 nodes (subject and object)
        return (double) (edgeCount * 2) / nodeCount;
    }

    /**
     * Checks if the graph is empty.
     *
     * @return true if no edges
     */
    public boolean isEmpty() {
        return edgeCount == 0;
    }

    /**
     * Checks if the graph has any data.
     *
     * @return true if at least one edge exists
     */
    public boolean hasData() {
        return edgeCount > 0;
    }

    @Override
    public String toString() {
        return String.format(
                "GraphStatistics{edges=%d, nodes=%d, predicates=%d, contexts=%d, " +
                        "subjects=%d, objects=%d, literals=%d, density=%.4f, avgDegree=%.2f}",
                edgeCount, nodeCount, predicateCount, contextCount,
                subjectCount, objectCount, literalCount,
                getDensity(), getAverageDegree()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphStatistics that = (GraphStatistics) o;
        return edgeCount == that.edgeCount &&
                nodeCount == that.nodeCount &&
                predicateCount == that.predicateCount &&
                contextCount == that.contextCount &&
                subjectCount == that.subjectCount &&
                objectCount == that.objectCount &&
                literalCount == that.literalCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(edgeCount, nodeCount, predicateCount, contextCount,
                subjectCount, objectCount, literalCount);
    }

    /**
     * Creates a new builder.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for GraphStatistics.
     */
    public static class Builder {
        private long edgeCount = 0;
        private long nodeCount = 0;
        private long predicateCount = 0;
        private long contextCount = 0;
        private final long subjectCount = 0;
        private final long objectCount = 0;
        private final long literalCount = 0;

        /**
         * Sets the edge count.
         *
         * @param count Edge count
         * @return This builder
         */
        public Builder edgeCount(long count) {
            this.edgeCount = Math.max(0, count);
            return this;
        }

        /**
         * Sets the node count.
         *
         * @param count Node count
         * @return This builder
         */
        public Builder nodeCount(long count) {
            this.nodeCount = Math.max(0, count);
            return this;
        }

        /**
         * Sets the predicate count.
         *
         * @param count Predicate count
         * @return This builder
         */
        public Builder predicateCount(long count) {
            this.predicateCount = Math.max(0, count);
            return this;
        }

        /**
         * Sets the context count.
         *
         * @param count Context count
         * @return This builder
         */
        public Builder contextCount(long count) {
            this.contextCount = Math.max(0, count);
            return this;
        }


        /**
         * Builds the GraphStatistics instance.
         *
         * @return New GraphStatistics instance
         */
        public GraphStatistics build() {
            return new GraphStatistics(this);
        }
    }
}