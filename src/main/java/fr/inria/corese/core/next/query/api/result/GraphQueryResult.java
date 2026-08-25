package fr.inria.corese.core.next.query.api.result;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.GraphQuery;

/**
 * Represents the result of evaluating a SPARQL CONSTRUCT or DESCRIBE query.
 *
 * @see Statement
 * @see GraphQuery
 * @see GraphQuery#evaluate()
 */
public interface GraphQueryResult extends StatementResult {
}
