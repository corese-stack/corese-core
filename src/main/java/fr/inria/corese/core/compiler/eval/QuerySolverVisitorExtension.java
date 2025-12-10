package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.List;

/**
 * Draf example of specific Visitor, e.g. implemented in Java instead of LDScript
 * Use case:
 * for one query: exec.query(q, new QuerySolverVisitorExtension())
 * for one query: exec.query(q, Mapping.create(vis));
 * <p>
 * for all query: QueryProcess.setSolverVisitorName("fr.inria.corese.core.compiler.eval.QuerySolverVisitorExtension");
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2020
 */
public class QuerySolverVisitorExtension extends QuerySolverVisitor {

    public QuerySolverVisitorExtension() {
    }

    @Override
    public IDatatype update(Query q, List<Edge> delete, List<Edge> insert) {
        return super.update(q, delete, insert);
    }

    @Override
    public IDatatype insert(IDatatype path, Edge triple) {
        return path;
    }
}
