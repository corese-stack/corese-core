package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.result.CoreseTupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.kgram.core.Eval;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.next.query.kgram.core.SparqlException;
import fr.inria.corese.core.next.query.kgram.execution.RdfTermMatcher;
import fr.inria.corese.core.next.query.kgram.execution.SparqlKgramEvaluator;
import fr.inria.corese.core.next.query.kgram.tool.StorageManagerProducer;
import fr.inria.corese.core.next.storagemanager.api.StorageManager;

import java.util.Objects;

/**
 * Internal executor for the first autonomous Corese-next SPARQL query path.
 *
 * <p>This class is intentionally small and transitional. It validates the
 * autonomous execution path:</p>
 *
 * <pre>
 * SPARQL string -> next parser -> next AST -> next KGRAM -> StorageManagerProducer -> StorageManager
 * </pre>
 *
 * <p>Only simple SELECT and ASK execution are supported here. A stable public
 * query API, graph query execution, updates, and advanced SPARQL 1.1 runtime
 * features still need to be designed separately.</p>
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
     * variants without exposing these transitional wiring details as public API.</p>
     */
    NextSparqlPipelineExecutor(
            StorageManager storage,
            SparqlParser parser,
            CoreseAstQueryBuilder queryBuilder) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.queryBuilder = Objects.requireNonNull(queryBuilder, "queryBuilder");
    }

    /**
     * Evaluates a SELECT query through the autonomous next pipeline.
     *
     * @param sparql SPARQL query string to parse and evaluate
     * @return tuple result backed by the KGRAM mappings produced from next storage
     * @throws IllegalArgumentException when the query is not a SELECT query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     */
    public TupleQueryResult evaluateTuple(String sparql) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof SelectQueryAst select)) {
            throw new IllegalArgumentException("Tuple evaluation requires a SELECT query, got: "
                    + ast.getClass().getSimpleName());
        }
        return new CoreseTupleQueryResult(evaluate(queryBuilder.toNextQuery(select)));
    }

    /**
     * Evaluates an ASK query through the autonomous next pipeline.
     *
     * @param sparql SPARQL query string to parse and evaluate
     * @return {@code true} when at least one mapping matches the ASK pattern
     * @throws IllegalArgumentException when the query is not an ASK query
     * @throws QueryEvaluationException when KGRAM evaluation fails
     */
    public boolean evaluateBoolean(String sparql) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof AskQueryAst ask)) {
            throw new IllegalArgumentException("Boolean evaluation requires an ASK query, got: "
                    + ast.getClass().getSimpleName());
        }
        return evaluate(queryBuilder.toNextQuery(ask)).size() > 0;
    }

    private Mappings evaluate(Query query) {
        try {
            Eval eval = Eval.create(
                    new StorageManagerProducer(storage),
                    new SparqlKgramEvaluator(),
                    new RdfTermMatcher());
            return eval.query(query);
        } catch (SparqlException e) {
            throw new QueryEvaluationException("Failed to evaluate query with the next pipeline: " + e.getMessage(), e);
        }
    }
}
