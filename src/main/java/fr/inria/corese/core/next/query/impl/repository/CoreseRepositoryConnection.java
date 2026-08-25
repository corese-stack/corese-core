package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.query.CoreseBooleanQuery;
import fr.inria.corese.core.next.query.impl.query.CoreseGraphQuery;
import fr.inria.corese.core.next.query.impl.query.CoreseTupleQuery;
import fr.inria.corese.core.next.query.impl.query.CoreseUpdate;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;
import fr.inria.corese.core.next.storage.api.StorageManager;

/**
 * Connection to a Corese repository.
 *
 * <p>Validates query syntax at preparation time and hides the internal pipeline
 * (parser, AST, bridge, KGRAM) behind the public {@link RepositoryConnection} API.
 * Users only interact with {@link TupleQuery}, {@link BooleanQuery},
 * {@link GraphQuery}, and {@link Update} interfaces.</p>
 */
public final class CoreseRepositoryConnection implements RepositoryConnection {

    private final Repository repository;
    private final StorageManager storage;
    private final NextSparqlPipelineExecutor executor;
    private final SparqlParser parser;
    private Dataset connectionDataset;
    private boolean open = true;

    CoreseRepositoryConnection(Repository repository, StorageManager storage) {
        this.repository = repository;
        this.storage = storage;
        this.executor = new NextSparqlPipelineExecutor(storage);
        this.parser = new SparqlParser();
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public ValueFactory getValueFactory() {
        return repository.getValueFactory();
    }

    @Override
    public boolean isOpen() {
        return open && repository.isOpen();
    }

    @Override
    public void close() throws RepositoryException {
        open = false;
    }

    @Override
    public TupleQuery prepareTupleQuery(QueryLanguage queryLanguage, String queryString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        checkLanguage(queryLanguage);
        QueryAst ast = parse(queryString);
        if (!(ast instanceof SelectQueryAst)) {
            throw new QuerySyntaxException(
                    "Expected a SELECT query, got: " + ast.getClass().getSimpleName());
        }
        CoreseTupleQuery q = new CoreseTupleQuery(queryString, queryLanguage, executor);
        applyConnectionDataset(q);
        return q;
    }

    @Override
    public GraphQuery prepareGraphQuery(QueryLanguage queryLanguage, String queryString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        checkLanguage(queryLanguage);
        QueryAst ast = parse(queryString);
        if (!(ast instanceof ConstructQueryAst) && !(ast instanceof DescribeQueryAst)) {
            throw new QuerySyntaxException(
                    "Expected a CONSTRUCT or DESCRIBE query, got: " + ast.getClass().getSimpleName());
        }
        CoreseGraphQuery q = new CoreseGraphQuery(queryString, queryLanguage, executor);
        applyConnectionDataset(q);
        return q;
    }

    @Override
    public BooleanQuery prepareBooleanQuery(QueryLanguage queryLanguage, String queryString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        checkLanguage(queryLanguage);
        QueryAst ast = parse(queryString);
        if (!(ast instanceof AskQueryAst)) {
            throw new QuerySyntaxException(
                    "Expected an ASK query, got: " + ast.getClass().getSimpleName());
        }
        CoreseBooleanQuery q = new CoreseBooleanQuery(queryString, queryLanguage, executor);
        applyConnectionDataset(q);
        return q;
    }

    @Override
    public Update prepareUpdate(QueryLanguage queryLanguage, String updateString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        checkLanguage(queryLanguage);
        QueryAst ast = parse(updateString);
        if (!(ast instanceof UpdateRequestAst)) {
            throw new QuerySyntaxException(
                    "Expected a SPARQL UPDATE request, got: " + ast.getClass().getSimpleName());
        }
        CoreseUpdate u = new CoreseUpdate(updateString, queryLanguage, storage, parser);
        applyConnectionDataset(u);
        return u;
    }

    @Override
    public void setDataset(Dataset dataset) {
        checkOpen();
        this.connectionDataset = dataset;
    }

    @Override
    public Dataset getDataset() {
        checkOpen();
        return connectionDataset;
    }

    @Override
    public void begin() throws RepositoryException {
        checkTransactionsSupported();
    }

    @Override
    public void commit() throws RepositoryException {
        checkTransactionsSupported();
    }

    @Override
    public void rollback() throws RepositoryException {
        checkTransactionsSupported();
    }

    private void checkTransactionsSupported() throws RepositoryException {
        checkOpen();
        throw new RepositoryException("Transactions are not yet supported.");
    }

    private void checkOpen() {
        if (!isOpen()) {
            throw new RepositoryException("This connection is closed.");
        }
    }

    private void checkLanguage(QueryLanguage queryLanguage) {
        if (queryLanguage != QueryLanguage.SPARQL) {
            throw new RepositoryException("Only SPARQL is supported by the next query pipeline.");
        }
    }

    /**
     * Applies the connection-level dataset to an operation as its initial dataset,
     * only when no query-level dataset has been set yet.
     * The user can still override it by calling {@link fr.inria.corese.core.next.query.api.Operation#setDataset(Dataset)}
     * on the returned operation.
     */
    private void applyConnectionDataset(fr.inria.corese.core.next.query.api.Operation operation) {
        if (connectionDataset != null && operation.getDataset() == null) {
            operation.setDataset(connectionDataset);
        }
    }

    /**
     * Parses the query string, throwing {@link QuerySyntaxException} on any syntax error.
     */
    private QueryAst parse(String queryString) throws QuerySyntaxException {
        return parser.parse(queryString);
    }
}
