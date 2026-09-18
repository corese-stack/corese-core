package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Savepoint for SILENT operations and backends without native transactions. */
record UpdateSnapshot(List<Statement> statements, Set<Resource> graphs) {
    static UpdateSnapshot capture(StorageManager storage) {
        try (Stream<Statement> stream = storage.queries().find(StatementPattern.matchAll())) {
            return new UpdateSnapshot(stream.toList(), Set.copyOf(storage.metadata().getContexts()));
        }
    }

    void restore(StorageManager storage) {
        Set<Statement> original = new HashSet<>(statements);
        try (Stream<Statement> current = storage.queries().find(StatementPattern.matchAll())) {
            storage.mutations().removeAll(current.filter(statement -> !original.contains(statement)).toList());
        }
        for (Resource graph : storage.metadata().getContexts()) {
            if (!graphs.contains(graph)) {
                storage.mutations().dropGraph(graph);
            }
        }
        for (Resource graph : graphs) {
            storage.mutations().createGraph(graph);
        }
        storage.mutations().addAll(statements);
    }
}
