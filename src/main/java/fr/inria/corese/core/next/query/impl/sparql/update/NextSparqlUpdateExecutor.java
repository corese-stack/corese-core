package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.AddRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ClearRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.CopyRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.CreateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteWhereRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DropRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GraphAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.InsertDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ModifyRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MoveRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.NamedGraphQuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAsts;
import fr.inria.corese.core.next.query.impl.sparql.ast.QuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.SparqlTermResolver;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.ArrayList;
import java.util.List;

/** Executes update requests through the native query and storage pipelines. */
public final class NextSparqlUpdateExecutor {
    private final StorageManager storage;
    private final QueryPrologueAst prologue;
    private final SparqlTermResolver resolver;
    private final UpdateGraphStore graphs;

    public NextSparqlUpdateExecutor(StorageManager storage, QueryPrologueAst prologue) {
        this.storage = storage;
        this.prologue = prologue;
        this.resolver = new SparqlTermResolver(prologue);
        this.graphs = new UpdateGraphStore(storage, resolver);
    }

    public void execute(UpdateRequestAst request) {
        for (int index = 0; index < request.operations().size(); index++) {
            new NextSparqlUpdateExecutor(storage, request.operationPrologues().get(index))
                    .execute(request.operations().get(index));
        }
    }

    private void execute(UpdateRequestUnitAst operation) {
        try {
            if (silent(operation)) {
                UpdateTransaction.executeWithSavepoint(storage, () -> dispatch(operation));
            } else {
                dispatch(operation);
            }
        } catch (RuntimeException e) {
            if (!silent(operation) || e.getSuppressed().length > 0) {
                throw new QueryEvaluationException("SPARQL Update failed: " + operation.getClass().getSimpleName(), e);
            }
        }
    }

    private void dispatch(UpdateRequestUnitAst operation) {
        switch (operation) {
            case InsertDataRequestAst(QuadsAst data) ->
                    storage.mutations().addAll(new UpdateTemplate(resolver, null).instantiate(data, null));
            case DeleteDataRequestAst(QuadsAst data) ->
                    storage.mutations().removeAll(new UpdateTemplate(resolver, null).instantiate(data, null));
            case DeleteWhereRequestAst(QuadsAst pattern) -> deleteWhere(pattern);
            case ModifyRequestAst modify -> modify(modify);
            case CreateRequestAst create -> graphs.create(create.graphRef());
            case ClearRequestAst clear -> graphs.clear(clear.graphRef(), false);
            case DropRequestAst drop -> graphs.clear(drop.graphRef(), true);
            case AddRequestAst add -> graphs.transfer(add.source(), add.destination(), UpdateGraphStore.Transfer.ADD);
            case CopyRequestAst copy -> graphs.transfer(copy.source(), copy.destination(), UpdateGraphStore.Transfer.COPY);
            case MoveRequestAst move -> graphs.transfer(move.source(), move.destination(), UpdateGraphStore.Transfer.MOVE);
            case LoadRequestAst load -> UpdateLoader.load(storage, graphs.graph(load.fromClause()), graphs.graph(load.toClause()));
        }
    }

    private static boolean silent(UpdateRequestUnitAst operation) {
        return switch (operation) {
            case CreateRequestAst create -> create.silent();
            case ClearRequestAst clear -> clear.silent();
            case DropRequestAst drop -> drop.silent();
            case AddRequestAst add -> add.silent();
            case CopyRequestAst copy -> copy.silent();
            case MoveRequestAst move -> move.silent();
            case LoadRequestAst load -> load.silent();
            default -> false;
        };
    }

    private void deleteWhere(QuadsAst pattern) {
        List<PatternAst> patterns = new ArrayList<>();
        patterns.add(new BgpAst(pattern.defaultTriples()));
        for (NamedGraphQuadsAst block : pattern.namedGraphBlocks()) {
            patterns.add(new GraphAst(block.graph(), new GroupGraphPatternAst(List.of(new BgpAst(block.triples())))));
        }
        SelectQueryAst query = new SelectQueryAst(ProjectionAsts.selectAll(), null,
                new GroupGraphPatternAst(patterns), null, prologue, null);
        apply(query, pattern, new QuadsAst(null, null), null);
    }

    private void modify(ModifyRequestAst operation) {
        SelectQueryAst source = operation.query();
        SelectQueryAst query = new SelectQueryAst(source.projection(), operation.using(), source.whereClause(),
                source.solutionModifier(), prologue, source.valuesClause());
        Resource target = operation.withGraph() == null ? null
                : (Resource) resolver.toNode(operation.withGraph()).getDatatypeValue();
        apply(query, operation.deleteTemplate(), operation.insertTemplate(), target);
    }

    private void apply(SelectQueryAst query, QuadsAst delete, QuadsAst insert, Resource target) {
        List<Statement> deletions = new ArrayList<>();
        List<Statement> insertions = new ArrayList<>();
        NextSparqlPipelineExecutor executor = new NextSparqlPipelineExecutor(new UpdateDatasetView(storage, target));
        try (TupleQueryResult solutions = executor.evaluateTuple(query)) {
            while (solutions.hasNext()) {
                UpdateTemplate template = new UpdateTemplate(resolver, solutions.next());
                deletions.addAll(template.instantiate(delete, target));
                insertions.addAll(template.instantiate(insert, target));
            }
        }
        // All solutions and both templates must be materialized before any mutation.
        storage.mutations().removeAll(deletions);
        storage.mutations().addAll(insertions);
    }
}
