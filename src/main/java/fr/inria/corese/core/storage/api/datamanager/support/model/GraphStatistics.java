package fr.inria.corese.core.storage.api.datamanager.support.model;

import java.util.Objects;

/**
 * Statistics about a graph's content and structure.
 * This class is mutable to allow updates as the graph changes.
 */
public class GraphStatistics {

    private long edgeCount;
    private long nodeCount;
    private long predicateCount;
    private long contextCount;
    private long subjectCount;
    private long objectCount;
    private long literalCount;

    /**
     * Default constructor - initializes all counts to 0.
     */
    public GraphStatistics() {
        this.edgeCount = 0;
        this.nodeCount = 0;
        this.predicateCount = 0;
        this.contextCount = 0;
        this.subjectCount = 0;
        this.objectCount = 0;
        this.literalCount = 0;
    }

    /**
     * Constructor with initial values.
     *
     * @param edgeCount      Initial edge count
     * @param nodeCount      Initial node count
     * @param predicateCount Initial predicate count
     * @param contextCount   Initial context count
     * @param subjectCount   Initial subject count
     * @param objectCount    Initial object count
     * @param literalCount   Initial literal count
     */
    public GraphStatistics(long edgeCount, long nodeCount, long predicateCount,
                           long contextCount, long subjectCount, long objectCount,
                           long literalCount) {
        this.edgeCount = Math.max(0, edgeCount);
        this.nodeCount = Math.max(0, nodeCount);
        this.predicateCount = Math.max(0, predicateCount);
        this.contextCount = Math.max(0, contextCount);
        this.subjectCount = Math.max(0, subjectCount);
        this.objectCount = Math.max(0, objectCount);
        this.literalCount = Math.max(0, literalCount);
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
     * Returns the number of unique subjects.
     *
     * @return Subject count
     */
    public long getSubjectCount() {
        return subjectCount;
    }

    /**
     * Returns the number of unique objects.
     *
     * @return Object count
     */
    public long getObjectCount() {
        return objectCount;
    }

    /**
     * Returns the number of literals.
     *
     * @return Literal count
     */
    public long getLiteralCount() {
        return literalCount;
    }

    /**
     * Sets the edge count.
     *
     * @param count Edge count (negative values are set to 0)
     */
    public void setEdgeCount(long count) {
        this.edgeCount = Math.max(0, count);
    }

    /**
     * Sets the node count.
     *
     * @param count Node count (negative values are set to 0)
     */
    public void setNodeCount(long count) {
        this.nodeCount = Math.max(0, count);
    }

    /**
     * Sets the predicate count.
     *
     * @param count Predicate count (negative values are set to 0)
     */
    public void setPredicateCount(long count) {
        this.predicateCount = Math.max(0, count);
    }

    /**
     * Sets the context count.
     *
     * @param count Context count (negative values are set to 0)
     */
    public void setContextCount(long count) {
        this.contextCount = Math.max(0, count);
    }

    /**
     * Sets the subject count.
     *
     * @param count Subject count (negative values are set to 0)
     */
    public void setSubjectCount(long count) {
        this.subjectCount = Math.max(0, count);
    }

    /**
     * Sets the object count.
     *
     * @param count Object count (negative values are set to 0)
     */
    public void setObjectCount(long count) {
        this.objectCount = Math.max(0, count);
    }

    /**
     * Sets the literal count.
     *
     * @param count Literal count (negative values are set to 0)
     */
    public void setLiteralCount(long count) {
        this.literalCount = Math.max(0, count);
    }

    /**
     * Increments the edge count by 1.
     */
    public void incrementEdgeCount() {
        this.edgeCount++;
    }

    /**
     * Decrements the edge count by 1 (minimum 0).
     */
    public void decrementEdgeCount() {
        if (this.edgeCount > 0) {
            this.edgeCount--;
        }
    }

    /**
     * Increments the node count by 1.
     */
    public void incrementNodeCount() {
        this.nodeCount++;
    }

    /**
     * Decrements the node count by 1 (minimum 0).
     */
    public void decrementNodeCount() {
        if (this.nodeCount > 0) {
            this.nodeCount--;
        }
    }

    /**
     * Increments the predicate count by 1.
     */
    public void incrementPredicateCount() {
        this.predicateCount++;
    }

    /**
     * Decrements the predicate count by 1 (minimum 0).
     */
    public void decrementPredicateCount() {
        if (this.predicateCount > 0) {
            this.predicateCount--;
        }
    }

    /**
     * Increments the context count by 1.
     */
    public void incrementContextCount() {
        this.contextCount++;
    }

    /**
     * Decrements the context count by 1 (minimum 0).
     */
    public void decrementContextCount() {
        if (this.contextCount > 0) {
            this.contextCount--;
        }
    }

    /**
     * Increments the subject count by 1.
     */
    public void incrementSubjectCount() {
        this.subjectCount++;
    }

    /**
     * Decrements the subject count by 1 (minimum 0).
     */
    public void decrementSubjectCount() {
        if (this.subjectCount > 0) {
            this.subjectCount--;
        }
    }

    /**
     * Increments the object count by 1.
     */
    public void incrementObjectCount() {
        this.objectCount++;
    }

    /**
     * Decrements the object count by 1 (minimum 0).
     */
    public void decrementObjectCount() {
        if (this.objectCount > 0) {
            this.objectCount--;
        }
    }

    /**
     * Increments the literal count by 1.
     */
    public void incrementLiteralCount() {
        this.literalCount++;
    }

    /**
     * Decrements the literal count by 1 (minimum 0).
     */
    public void decrementLiteralCount() {
        if (this.literalCount > 0) {
            this.literalCount--;
        }
    }

    /**
     * Resets all statistics to 0.
     */
    public void reset() {
        this.edgeCount = 0;
        this.nodeCount = 0;
        this.predicateCount = 0;
        this.contextCount = 0;
        this.subjectCount = 0;
        this.objectCount = 0;
        this.literalCount = 0;
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
}