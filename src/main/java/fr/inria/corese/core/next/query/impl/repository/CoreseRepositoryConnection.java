package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.Query;
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
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

import java.util.Objects;

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
    private Transaction transaction;
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
        if (!open) {
            return;
        }
        try {
            if (transaction != null && transaction.isActive()) {
                transaction.close();
            }
        } catch (StorageException | IllegalStateException e) {
            throw new RepositoryException("Could not roll back the active transaction", e);
        } finally {
            transaction = null;
            open = false;
        }
    }

    private static final String PARAM_QUERY_STRING = "queryString";

    @Override
    public TupleQuery prepareTupleQuery(String queryString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        String source = requireSource(queryString, PARAM_QUERY_STRING);
        QueryAst ast = parse(source);
        if (!(ast instanceof SelectQueryAst)) {
            throw new QuerySyntaxException(
                    "Expected a SELECT query, got: " + ast.getClass().getSimpleName());
        }
        CoreseTupleQuery q = new CoreseTupleQuery(source, executor, this::checkOpen);
        applyConnectionDataset(q);
        return q;
    }

    @Override
    public GraphQuery prepareGraphQuery(String queryString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        String source = requireSource(queryString, PARAM_QUERY_STRING);
        QueryAst ast = parse(source);
        if (!(ast instanceof ConstructQueryAst) && !(ast instanceof DescribeQueryAst)) {
            throw new QuerySyntaxException(
                    "Expected a CONSTRUCT or DESCRIBE query, got: " + ast.getClass().getSimpleName());
        }
        CoreseGraphQuery q = new CoreseGraphQuery(source, executor, this::checkOpen);
        applyConnectionDataset(q);
        return q;
    }

    @Override
    public BooleanQuery prepareBooleanQuery(String queryString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        String source = requireSource(queryString, PARAM_QUERY_STRING);
        QueryAst ast = parse(source);
        if (!(ast instanceof AskQueryAst)) {
            throw new QuerySyntaxException(
                    "Expected an ASK query, got: " + ast.getClass().getSimpleName());
        }
        CoreseBooleanQuery q = new CoreseBooleanQuery(source, executor, this::checkOpen);
        applyConnectionDataset(q);
        return q;
    }

    @Override
    public Update prepareUpdate(String updateString)
            throws QuerySyntaxException, RepositoryException {
        checkOpen();
        String source = requireSource(updateString, "updateString");
        QueryAst ast = parse(source);
        if (!(ast instanceof UpdateRequestAst)) {
            throw new QuerySyntaxException(
                    "Expected a SPARQL UPDATE request, got: " + ast.getClass().getSimpleName());
        }
        return new CoreseUpdate(source, storage, parser, this::checkOpen);
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
    public boolean supportsTransactions() {
        checkOpen();
        return transactionManager().supportsTransactions();
    }

    @Override
    public boolean isActive() {
        checkOpen();
        return transaction != null && transaction.isActive();
    }

    @Override
    public void begin() throws RepositoryException {
        checkOpen();
        if (isActive()) {
            throw new RepositoryException("A transaction is already active on this connection.");
        }
        TransactionManager manager = transactionManager();
        if (!manager.supportsTransactions()) {
            throw new RepositoryException("Transactions are not supported by this repository.");
        }
        try {
            transaction = manager.beginTransaction();
        } catch (StorageException | UnsupportedOperationException e) {
            throw new RepositoryException("Could not begin transaction", e);
        }
    }

    @Override
    public void commit() throws RepositoryException {
        Transaction active = requireActiveTransaction();
        try {
            active.commit();
        } catch (StorageException | IllegalStateException e) {
            throw new RepositoryException("Could not commit transaction", e);
        } finally {
            if (!active.isActive()) {
                transaction = null;
            }
        }
    }

    @Override
    public void rollback() throws RepositoryException {
        Transaction active = requireActiveTransaction();
        try {
            active.rollback();
        } catch (StorageException | IllegalStateException e) {
            throw new RepositoryException("Could not roll back transaction", e);
        } finally {
            if (!active.isActive()) {
                transaction = null;
            }
        }
    }

    private Transaction requireActiveTransaction() {
        checkOpen();
        if (!isActive()) {
            throw new RepositoryException("No transaction is active on this connection.");
        }
        return transaction;
    }

    private TransactionManager transactionManager() {
        return storage.getTransactionManager();
    }

    private void checkOpen() {
        if (!isOpen()) {
            throw new RepositoryException("This connection is closed.");
        }
    }

    /**
     * Applies the connection-level dataset to an operation as its initial dataset,
     * only when no query-level dataset has been set yet.
     * The user can still override it by calling {@link Query#setDataset(Dataset)}
     * on the returned operation.
     */
    private void applyConnectionDataset(Query<?> operation) {
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

    private String requireSource(String source, String parameterName) {
        String checkedSource = Objects.requireNonNull(source, parameterName);
        if (checkedSource.isBlank()) {
            throw new QuerySyntaxException("SPARQL source must not be blank.");
        }
        return checkedSource;
    }
}
