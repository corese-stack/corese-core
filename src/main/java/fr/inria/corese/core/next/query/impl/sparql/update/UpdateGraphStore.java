package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.GraphRefAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.SparqlTermResolver;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Graph-management semantics, including the distinction between CLEAR and DROP. */
final class UpdateGraphStore {
    enum Transfer { COPY, MOVE, ADD }

    private final StorageManager storage;
    private final SparqlTermResolver resolver;

    UpdateGraphStore(StorageManager storage, SparqlTermResolver resolver) {
        this.storage = storage;
        this.resolver = resolver;
    }

    Resource graph(GraphRefAst reference) {
        return reference == null || reference.graph() == null ? null
                : (Resource) resolver.toNode(reference.graph()).getDatatypeValue();
    }

    void create(GraphRefAst reference) {
        Resource graph = graph(reference);
        if (!storage.mutations().createGraph(graph)) {
            throw new QueryEvaluationException("Graph already exists: " + graph);
        }
    }

    void clear(GraphRefAst reference, boolean drop) {
        if (reference.graph() != null) {
            requireGraph(graph(reference));
        }
        for (Resource graph : targets(reference)) {
            if (drop && graph != null) {
                storage.mutations().dropGraph(graph);
            } else {
                storage.mutations().clear(graph);
            }
        }
    }

    private List<Resource> targets(GraphRefAst reference) {
        List<Resource> targets = new ArrayList<>();
        if (reference.named() || reference.all()) {
            targets.addAll(storage.metadata().getContexts());
        }
        if (!reference.named()) {
            targets.add(graph(reference));
        }
        return targets;
    }

    void transfer(GraphRefAst source, GraphRefAst destination, Transfer transfer) {
        Resource from = graph(source);
        Resource to = graph(destination);
        if (Objects.equals(from, to)) {
            return;
        }
        requireGraph(from);
        List<Statement> statements;
        try (Stream<Statement> stream = storage.queries().find(StatementPattern.of(null, null, null, from))) {
            statements = stream.toList();
        }
        if (to != null) {
            storage.mutations().createGraph(to);
        }
        if (transfer != Transfer.ADD) {
            storage.mutations().clear(to);
        }
        for (Statement statement : statements) {
            storage.mutations().add(Values.factory().createStatement(statement.getSubject(),
                    statement.getPredicate(), statement.getObject(), to));
        }
        if (transfer == Transfer.MOVE) {
            clear(source, true);
        }
    }

    private void requireGraph(Resource graph) {
        if (graph != null && !storage.metadata().getContexts().contains(graph)) {
            throw new QueryEvaluationException("Graph does not exist: " + graph);
        }
    }
}
