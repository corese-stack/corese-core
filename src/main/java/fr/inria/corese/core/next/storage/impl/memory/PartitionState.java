package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.Resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered graph registry whose partition references are acquired and released together.
 */
final class PartitionState {

    /**
     * Ordered partition registry; empty named graphs are retained until DROP.
     * A null key represents the default partition.
     */
    final Map<Resource, GraphPartition> graphs = new LinkedHashMap<>();

    /**
     * Number of live entries or statements owned by this container.
     */
    long size;

    /**
     * Ordinal assigned to the next created graph.
     */
    long nextOrdinal;

    /**
     * Creates an empty ordered registry with exclusive ownership of its map.
     */
    PartitionState() {
    }

    /**
     * Copies owned mutable containers while sharing immutable keys and statements.
     *
     * @return an independently owned copy of the mutable containers
     */
    PartitionState copy() {
        final PartitionState copy = new PartitionState();
        graphs.forEach((graph, partition) -> {
            partition.references++;
            copy.graphs.put(graph, partition);
        });
        copy.size = size;
        copy.nextOrdinal = nextOrdinal;
        return copy;
    }

    /**
     * Capture selected graphs only; writes to unrelated graphs need no cursor-induced COW.
     *
     * @param contexts selected graph identifiers; an empty array selects all graphs
     * @return an independently owned copy of the mutable containers
     */
    PartitionState copy(final Resource[] contexts) {
        if (contexts.length == 0) {
            return copy();
        }
        final PartitionState copy = new PartitionState();
        final List<GraphPartition> selected = new ArrayList<>();
        for (final Resource graph : contexts) {
            final GraphPartition partition = graphs.get(graph);
            if (partition != null) {
                selected.add(partition);
            }
        }
        selected.sort(Comparator.comparingLong(partition -> partition.ordinal));
        for (final GraphPartition partition : selected) {
            partition.references++;
            copy.graphs.put(partition.context, partition);
            copy.size += partition.spo.size();
        }
        copy.nextOrdinal = nextOrdinal;
        return copy;
    }

    /**
     * Detaches only a shared partition, preserving existing readers and graph order.
     *
     * @param graph named graph identifier or staging registry key, according to the operation
     * @return the exclusively owned partition, or null if the graph is absent
     */
    GraphPartition writable(final Resource graph) {
        final GraphPartition partition = graphs.get(graph);
        if (partition != null && partition.references > 1) {
            final GraphPartition replacement = partition.copy();
            partition.references--;
            graphs.put(graph, replacement);
            return replacement;
        }
        return partition;
    }

    /**
     * Replaces a partition with an empty version, preserving graph identity and registry position.
     *
     * @param partition partition retained or exclusively owned by the supplied state
     */
    void empty(final GraphPartition partition) {
        // Keep the graph's identity and registry position. Readers continue to own
        // the old partition; clearing never needs to clone its three indexes.
        final GraphPartition replacement = new GraphPartition(partition.context, partition.ordinal);
        graphs.put(partition.context, replacement);
        partition.references--;
    }

    /**
     * Releases this state's ownership; clearing the registry makes repeated release harmless.
     */
    void release() {
        graphs.values().forEach(partition -> partition.references--);
        graphs.clear();
    }
}
