package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.result.CoreseGraphQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlException;
import fr.inria.corese.core.next.query.impl.engine.eval.RdfTermMatcher;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlKgramEvaluator;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Internal orchestrator for the Corese-next SPARQL query path.
 *
 * <p>It connects the current pipeline stages:</p>
 *
 * <pre>
 * SPARQL string -> next parser -> next AST -> next KGRAM -> StorageManagerProducer -> StorageManager
 * </pre>
 *
 * <p>The executor handles SELECT, ASK, CONSTRUCT, and DESCRIBE through the next
 * storage path. Individual SPARQL 1.1 algebra features are enabled explicitly
 * as their runtime implementations become available.</p>
 */
public final class NextSparqlPipelineExecutor {

    private final StorageManager storage;
    private final SparqlParser parser;
    private final CoreseAstQueryBuilder queryBuilder;

    /**
     * Creates an executor backed by the given next storage manager.
     *
     * @param storage storage manager used by {@link StorageManagerProducer} to
     *                read RDF statements during KGRAM evaluation
     */
    public NextSparqlPipelineExecutor(StorageManager storage) {
        this(storage, new SparqlParser(), new CoreseAstQueryBuilder());
    }

    /**
     * Creates an executor with explicit collaborators.
     *
     * <p>This constructor is package-private so tests can inject parser or bridge
     * variants without exposing wiring details as public API.</p>
     */
    NextSparqlPipelineExecutor(
            StorageManager storage,
            SparqlParser parser,
            CoreseAstQueryBuilder queryBuilder) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.queryBuilder = Objects.requireNonNull(queryBuilder, "queryBuilder");
    }

    // -------------------------------------------------------------------------
    // Public API — called by CoreseTupleQuery / CoreseBooleanQuery
    // -------------------------------------------------------------------------

    /**
     * Evaluates a SELECT query through the next pipeline with no initial bindings,
     * no dataset override, and no timeout.
     *
     * @param sparql SPARQL query string to parse and evaluate
     * @return tuple result backed by the KGRAM mappings produced from next storage
     * @throws IllegalArgumentException  when the query is not a SELECT query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     */
    public TupleQueryResult evaluateTuple(String sparql) {
        return evaluateTuple(sparql, null, null, 0L);
    }

    /**
     * Evaluates a SELECT query through the next pipeline.
     *
     * @param sparql          SPARQL query string to parse and evaluate
     * @param bindings        initial variable bindings to inject, or {@code null}
     * @param dataset         dataset override (FROM / FROM NAMED), or {@code null}
     * @param timeoutMillis   maximum evaluation time in milliseconds; 0 means no limit
     * @return tuple result backed by the KGRAM mappings produced from next storage
     * @throws IllegalArgumentException  when the query is not a SELECT query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     * @throws QueryTimeoutException    when the evaluation exceeds {@code timeoutMillis}
     */
    public TupleQueryResult evaluateTuple(String sparql, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof SelectQueryAst select)) {
            throw new IllegalArgumentException("Tuple evaluation requires a SELECT query, got: "
                    + ast.getClass().getSimpleName());
        }
        return new CoreseTupleQueryResult(
                evaluate(queryBuilder.toNextQuery(select), bindings, dataset, timeoutMillis));
    }

    /**
     * Evaluates an ASK query through the next pipeline with no initial bindings,
     * no dataset override, and no timeout.
     *
     * @param sparql SPARQL query string to parse and evaluate
     * @return {@code true} when at least one mapping matches the ASK pattern
     * @throws IllegalArgumentException  when the query is not an ASK query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     */
    public boolean evaluateBoolean(String sparql) {
        return evaluateBoolean(sparql, null, null, 0L);
    }

    /**
     * Evaluates an ASK query through the next pipeline.
     *
     * @param sparql          SPARQL query string to parse and evaluate
     * @param bindings        initial variable bindings to inject, or {@code null}
     * @param dataset         dataset override (FROM / FROM NAMED), or {@code null}
     * @param timeoutMillis   maximum evaluation time in milliseconds; 0 means no limit
     * @return {@code true} when at least one mapping matches the ASK pattern
     * @throws IllegalArgumentException  when the query is not an ASK query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     * @throws QueryTimeoutException    when the evaluation exceeds {@code timeoutMillis}
     */
    public boolean evaluateBoolean(String sparql, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof AskQueryAst ask)) {
            throw new IllegalArgumentException("Boolean evaluation requires an ASK query, got: "
                    + ast.getClass().getSimpleName());
        }
        return evaluate(queryBuilder.toNextQuery(ask), bindings, dataset, timeoutMillis).size() > 0;
    }

    /**
     * Evaluates a CONSTRUCT or DESCRIBE query through the next pipeline with no initial
     * bindings, no dataset override, and no timeout.
     *
     * @param sparql SPARQL query string to parse and evaluate
     * @return graph result containing the constructed statements
     * @throws IllegalArgumentException  when the query is not a CONSTRUCT or DESCRIBE query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     */
    public GraphQueryResult evaluateGraph(String sparql) {
        return evaluateGraph(sparql, null, null, 0L);
    }

    /**
     * Evaluates a CONSTRUCT or DESCRIBE query through the next pipeline.
     *
     * <p>The WHERE clause is evaluated by KGRAM to produce variable bindings. Each
     * binding is then applied to the CONSTRUCT template to materialise the output
     * triples. DESCRIBE queries are lowered to a construct-like shape by
     * {@link CoreseAstQueryBuilder} before evaluation.</p>
     *
     * @param sparql          SPARQL query string to parse and evaluate
     * @param bindings        initial variable bindings to inject, or {@code null}
     * @param dataset         dataset override (FROM / FROM NAMED), or {@code null}
     * @param timeoutMillis   maximum evaluation time in milliseconds; 0 means no limit
     * @return graph result containing the constructed statements
     * @throws IllegalArgumentException  when the query is not a CONSTRUCT or DESCRIBE query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     * @throws QueryTimeoutException    when the evaluation exceeds {@code timeoutMillis}
     */
    public GraphQueryResult evaluateGraph(String sparql, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        QueryAst ast = parser.parse(sparql);
        Query kgramQuery = switch (ast) {
            case ConstructQueryAst construct -> queryBuilder.toNextQuery(construct);
            case DescribeQueryAst describe -> queryBuilder.toNextQuery(describe);
            default -> throw new IllegalArgumentException(
                    "Graph evaluation requires a CONSTRUCT or DESCRIBE query, got: "
                            + ast.getClass().getSimpleName());
        };
        Mappings mappings = evaluate(kgramQuery, bindings, dataset, timeoutMillis);
        List<Statement> statements = buildConstructStatements(kgramQuery, mappings);
        return new CoreseGraphQueryResult(statements.iterator());
    }

    // -------------------------------------------------------------------------
    // Internal evaluation pipeline
    // -------------------------------------------------------------------------

    private Mappings evaluate(Query kgramQuery, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        Eval eval = Eval.create(
                new StorageManagerProducer(storage),
                new SparqlKgramEvaluator(),
                new RdfTermMatcher());

        if (dataset != null) {
            applyDataset(kgramQuery, dataset);
        }

        Mapping initialMapping = buildInitialMapping(bindings);

        if (timeoutMillis > 0) {
            return evaluateWithTimeout(eval, kgramQuery, initialMapping, timeoutMillis);
        }
        return evaluateCore(eval, kgramQuery, initialMapping);
    }

    private Mappings evaluateCore(Eval eval, Query kgramQuery, Mapping initialMapping) {
        try {
            return eval.query(null, kgramQuery, initialMapping);
        } catch (SparqlException e) {
            throw new QueryEvaluationException(
                    "Failed to evaluate query with the next pipeline: " + e.getMessage(), e);
        }
    }

    /**
     * Runs {@code eval.query()} with a cooperative timeout.
     *
     * <p>The evaluation runs in a request-scoped virtual thread. At the deadline,
     * {@link Eval#finish()} cooperatively stops KGRAM and the task is interrupted.
     * No process-wide scheduler or mutable global timeout state is retained.</p>
     */
    private Mappings evaluateWithTimeout(Eval eval, Query kgramQuery, Mapping initialMapping, long timeoutMillis) {
        FutureTask<Mappings> evaluation = new FutureTask<>(
                () -> evaluateCore(eval, kgramQuery, initialMapping));
        Thread thread = Thread.ofVirtual().name("sparql-query").start(evaluation);
        try {
            return evaluation.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            eval.finish();
            evaluation.cancel(true);
            throw new QueryTimeoutException(timeoutMillis);
        } catch (InterruptedException exception) {
            eval.finish();
            evaluation.cancel(true);
            Thread.currentThread().interrupt();
            throw new QueryEvaluationException("Query evaluation was interrupted", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof QueryEvaluationException queryFailure) {
                throw queryFailure;
            }
            throw new QueryEvaluationException("Query evaluation failed", cause);
        } finally {
            if (thread.isAlive() && evaluation.isCancelled()) {
                thread.interrupt();
            }
        }
    }

    // -------------------------------------------------------------------------
    // CONSTRUCT template instantiation
    // -------------------------------------------------------------------------

    /**
     * Applies the CONSTRUCT template of a KGRAM query to every result mapping and
     * returns the materialised statements.
     *
     * <p>For each mapping produced by the WHERE clause, each template edge is
     * instantiated by substituting variable nodes with their bound target nodes.
     * Triples where any component is unbound or cannot be converted to a valid
     * RDF term are silently skipped, matching standard SPARQL CONSTRUCT semantics.</p>
     */
    private List<Statement> buildConstructStatements(Query kgramQuery, Mappings mappings) {
        List<Edge> templateEdges = new ArrayList<>();
        Exp constructTemplate = kgramQuery.getConstruct();
        if (constructTemplate == null) {
            return List.of();
        }
        constructTemplate.getEdgeList(templateEdges);

        ValueFactory factory = Values.factory();
        List<Statement> statements = new ArrayList<>();

        for (Mapping mapping : mappings) {
            for (Edge templateEdge : templateEdges) {
                Node subjectNode   = resolveTemplateNode(templateEdge.getNode(0), mapping);
                Node predicateNode = resolveTemplateNode(templateEdge.getProperty(), mapping);
                Node objectNode    = resolveTemplateNode(templateEdge.getNode(1), mapping);

                if (subjectNode == null || predicateNode == null || objectNode == null) {
                    continue;
                }

                Value subject   = kgramNodeToApiValue(subjectNode);
                Value predicate = kgramNodeToApiValue(predicateNode);
                Value object    = kgramNodeToApiValue(objectNode);

                if (subject instanceof Resource s && predicate instanceof IRI p && object != null) {
                    statements.add(factory.createStatement(s, p, object));
                }
            }
        }
        return statements;
    }

    /**
     * Resolves a CONSTRUCT template node: if the node is a variable, look it up in
     * the current mapping; if it is already a constant, return it directly.
     *
     * @return the bound or constant node, or {@code null} when a variable is unbound
     */
    private Node resolveTemplateNode(Node templateNode, Mapping mapping) {
        if (templateNode == null) {
            return null;
        }
        if (templateNode.isVariable()) {
            return mapping.getNode(templateNode);
        }
        return templateNode;
    }

    /**
     * Converts a KGRAM constant {@link Node} to the corresponding API {@link Value}.
     *
     * @return the API value, or {@code null} when the node kind is not supported
     */
    private Value kgramNodeToApiValue(Node node) {
        if (node.getDatatypeValue() instanceof Value value) {
            return value;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Dataset wiring
    // -------------------------------------------------------------------------

    /**
     * Overrides the FROM / FROM NAMED clauses of a KGRAM query with the API dataset.
     *
     * <p>Both graph sets replace the corresponding query clauses. Empty graph
     * sets remain significant: for example, a named-only dataset has an empty
     * default graph.</p>
     */
    private void applyDataset(Query kgramQuery, Dataset dataset) {
        kgramQuery.setFrom(irisToKgramNodes(dataset.getDefaultGraphs()));
        kgramQuery.setNamed(irisToKgramNodes(dataset.getNamedGraphs()));
        kgramQuery.setDatasetSpecified(true);
    }

    private List<Node> irisToKgramNodes(Iterable<IRI> iris) {
        List<Node> nodes = new ArrayList<>();
        for (IRI iri : iris) {
            nodes.add(NodeImpl.forIRI(iri.stringValue()));
        }
        return nodes;
    }

    // -------------------------------------------------------------------------
    // Initial bindings wiring
    // -------------------------------------------------------------------------

    /**
     * Converts an API {@link BindingSet} into a KGRAM {@link Mapping} that can be
     * passed to {@link Eval#query(Node, Query, Mapping)} as initial variable bindings.
     *
     * <p>Each binding entry becomes a (variable-node, value-node) pair. The variable
     * node carries the variable name as its label so that
     * {@link Query#getExtNode(String)} can look it up by name during evaluation.
     * Bindings whose value cannot be converted to a KGRAM node are silently skipped.</p>
     *
     * @return a {@link Mapping} with the converted bindings, or {@code null} when the
     *         binding set is empty or all values failed to convert
     */
    private Mapping buildInitialMapping(BindingSet bindings) {
        if (bindings == null || bindings.getBindingNames().isEmpty()) {
            return null;
        }
        List<Node> queryNodes = new ArrayList<>();
        List<Node> targetNodes = new ArrayList<>();
        for (Binding b : bindings) {
            Node targetNode = valueToKgramNode(b.value());
            if (targetNode != null) {
                queryNodes.add(NodeImpl.forVariable(b.name()));
                targetNodes.add(targetNode);
            }
        }
        return queryNodes.isEmpty() ? null : Mapping.create(queryNodes, targetNodes);
    }

    /**
     * Converts an API {@link Value} into a KGRAM constant {@link Node}.
     *
     * @return a constant node, or {@code null} when the value type is not supported
     */
    private Node valueToKgramNode(Value value) {
        return value == null ? null : NodeImpl.forValue(value);
    }
}
