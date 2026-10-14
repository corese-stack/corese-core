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
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParserOptions;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final int QUERY_RUNNING = 0;
    private static final int QUERY_COMPLETED = 1;
    private static final int QUERY_TIMED_OUT = 2;

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

    /** Creates an executor with a query-scoped source base IRI. */
    public NextSparqlPipelineExecutor(StorageManager storage, String baseIRI) {
        this(storage, new SparqlParser(new SparqlParserOptions.Builder().baseIRI(baseIRI).build()),
                new CoreseAstQueryBuilder());
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
        return evaluateTuple(select, bindings, dataset, timeoutMillis);
    }

    /** Evaluates an already parsed SELECT, also used by SPARQL Update WHERE clauses. */
    public TupleQueryResult evaluateTuple(SelectQueryAst query) {
        return evaluateTuple(query, null, null, 0L);
    }

    /** Evaluates a parsed SELECT with current options and a fresh runtime query. */
    public TupleQueryResult evaluateTuple(SelectQueryAst query, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        return new CoreseTupleQueryResult(evaluate(queryBuilder.toNextQuery(query), bindings, dataset, timeoutMillis));
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
        return evaluateBoolean(ask, bindings, dataset, timeoutMillis);
    }

    /** Evaluates a parsed ASK with current options and a fresh runtime query. */
    public boolean evaluateBoolean(AskQueryAst query, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        return evaluate(queryBuilder.toNextQuery(query), bindings, dataset, timeoutMillis).size() > 0;
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
        return evaluateGraph(parser.parse(sparql), bindings, dataset, timeoutMillis);
    }

    /** Evaluates a parsed CONSTRUCT or DESCRIBE with a fresh runtime query and template. */
    public GraphQueryResult evaluateGraph(QueryAst ast, BindingSet bindings, Dataset dataset, long timeoutMillis) {
        Objects.requireNonNull(ast, "ast");
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
        StorageManagerProducer producer = new StorageManagerProducer(storage);
        Eval eval = Eval.create(
                producer,
                new SparqlKgramEvaluator(),
                new RdfTermMatcher());

        if (dataset != null) {
            applyDataset(kgramQuery, dataset);
        }

        Mapping initialMapping = buildInitialMapping(bindings);

        if (timeoutMillis > 0) {
            return evaluateWithTimeout(eval, producer, kgramQuery, initialMapping, timeoutMillis);
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
     * Runs {@code eval.query()} with a cooperative timeout on the caller thread.
     *
     * <p>Evaluation runs on the current thread so active storage transactions maintain
     * single-thread ownership. A request-scoped virtual thread watchdog cooperatively terminates
     * evaluation and interrupts the caller if {@code timeoutMillis} elapses. Completion
     * and expiration compete for one atomic terminal state; the timer cannot publish
     * an expiration separately from claiming it. Elapsed time is measured from entry,
     * including watchdog scheduling delay.</p>
     */
    private Mappings evaluateWithTimeout(Eval eval, StorageManagerProducer producer, Query kgramQuery, Mapping initialMapping, long timeoutMillis) {
        Thread caller = Thread.currentThread();
        boolean interruptedOnEntry = caller.isInterrupted();
        long started = System.nanoTime();
        long budget = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        AtomicInteger outcome = new AtomicInteger(QUERY_RUNNING);
        Thread watchdog = Thread.ofVirtual().name("sparql-timeout-watchdog").start(() -> {
            try {
                long remaining = budget - (System.nanoTime() - started);
                if (remaining > 0) {
                    TimeUnit.NANOSECONDS.sleep(remaining);
                }
                if (outcome.compareAndSet(QUERY_RUNNING, QUERY_TIMED_OUT)) {
                    caller.interrupt();
                    eval.finish();
                    producer.close();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            Mappings mappings = evaluateCore(eval, kgramQuery, initialMapping);
            if (!outcome.compareAndSet(QUERY_RUNNING, QUERY_COMPLETED)
                    || System.nanoTime() - started >= budget) {
                throw new QueryTimeoutException(timeoutMillis);
            }
            return mappings;
        } catch (RuntimeException exception) {
            if (!outcome.compareAndSet(QUERY_RUNNING, QUERY_COMPLETED)
                    && outcome.get() == QUERY_TIMED_OUT
                    && !(exception instanceof QueryTimeoutException)) {
                throw new QueryTimeoutException(timeoutMillis);
            }
            throw exception;
        } finally {
            outcome.compareAndSet(QUERY_RUNNING, QUERY_COMPLETED);
            finishWatchdog(watchdog, outcome.get() == QUERY_TIMED_OUT, interruptedOnEntry);
        }
    }

    /**
     * Cancels a pending timer, or waits for timeout interruption and resource cleanup to finish.
     * A timeout's interruption is consumed only after the watchdog terminates, so it cannot
     * interrupt a later query. An interruption already present on entry remains set.
     *
     * @param watchdog request-scoped timeout thread
     * @param timedOut whether expiration won the atomic outcome transition
     * @param interruptedOnEntry whether the caller entered evaluation interrupted
     */
    @SuppressWarnings("java:S2142") // Consume watchdog interruption after joining; restore the caller's entry status.
    private static void finishWatchdog(Thread watchdog, boolean timedOut, boolean interruptedOnEntry) {
        if (!timedOut) {
            watchdog.interrupt();
            return;
        }
        boolean joined = false;
        while (!joined) {
            try {
                watchdog.join();
                joined = true;
            } catch (InterruptedException interruption) {
                // The watchdog interrupts this caller; finish waiting before consuming that signal.
            }
        }
        Thread.interrupted();
        if (interruptedOnEntry) {
            Thread.currentThread().interrupt();
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
        Exp constructTemplate = kgramQuery.getConstruct();
        if (constructTemplate == null) {
            return List.of();
        }
        List<Edge> templateEdges = new ArrayList<>();
        constructTemplate.getEdgeList(templateEdges);

        ValueFactory factory = Values.factory();
        List<Statement> statements = new ArrayList<>();
        Set<String> usedBlankLabels = collectUsedBlankLabels(mappings);

        for (Mapping mapping : mappings) {
            instantiateTemplateMapping(templateEdges, mapping, usedBlankLabels, factory, statements);
        }
        return statements;
    }

    private Set<String> collectUsedBlankLabels(Mappings mappings) {
        Set<String> usedBlankLabels = new HashSet<>();
        for (Mapping mapping : mappings) {
            for (Node node : mapping.getNodes()) {
                if (node != null && node.isBlank()) {
                    usedBlankLabels.add(node.getLabel());
                }
            }
        }
        return usedBlankLabels;
    }

    private void instantiateTemplateMapping(
            List<Edge> templateEdges,
            Mapping mapping,
            Set<String> usedBlankLabels,
            ValueFactory factory,
            List<Statement> statements) {
        Map<String, Node> blankNodeMap = new HashMap<>();
        for (Edge templateEdge : templateEdges) {
            Node subjectNode = resolveTemplateNode(templateEdge.getNode(0), mapping, blankNodeMap, usedBlankLabels);
            Node predicateNode = resolveTemplateNode(templateEdge.getProperty(), mapping, blankNodeMap, usedBlankLabels);
            Node objectNode = resolveTemplateNode(templateEdge.getNode(1), mapping, blankNodeMap, usedBlankLabels);

            if (subjectNode == null || predicateNode == null || objectNode == null) {
                continue;
            }

            Value subject = kgramNodeToApiValue(subjectNode);
            Value predicate = kgramNodeToApiValue(predicateNode);
            Value object = kgramNodeToApiValue(objectNode);

            if (subject instanceof Resource s && predicate instanceof IRI p && object != null) {
                statements.add(factory.createStatement(s, p, object));
            }
        }
    }

    /**
     * Resolves a CONSTRUCT template node:
     * <ul>
     *   <li>Blank-variable nodes (existential blank nodes like {@code _:b0}) are
     *       resolved to a per-solution constant blank node, so that every occurrence
     *       of the same label within one solution maps to the same blank node.</li>
     *   <li>Regular variable nodes are looked up in the current mapping.</li>
     *   <li>Constant nodes are returned as-is.</li>
     * </ul>
     *
     * @return the resolved node, or {@code null} when a regular variable is unbound
     */
    private Node resolveTemplateNode(
            Node templateNode, Mapping mapping,
            Map<String, Node> blankNodeMap, Set<String> usedBlankLabels) {
        if (templateNode == null) {
            return null;
        }
        if (templateNode.isVariable()) {
            if (templateNode.isBlank()) {
                // Existential blank node: fresh blank per solution, same label → same node
                return blankNodeMap.computeIfAbsent(templateNode.getLabel(),
                        label -> {
                            Node blank;
                            do {
                                blank = NodeImpl.forValue(Values.factory().createBNode());
                            } while (!usedBlankLabels.add(blank.getLabel()));
                            return blank;
                        });
            }
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

    /** Translates dataset IRIs in encounter order into KGRAM's mutable dataset list. */
    @SuppressWarnings("java:S9391") // Keep the existing list allocation without a stream pipeline for this iterable.
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
