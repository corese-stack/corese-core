package fr.inria.corese.core.visitor.solver;

import fr.inria.corese.core.compiler.eval.QuerySolverVisitor;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 *
 * @author corby
 */
public class QuerySolverVisitorUser extends QuerySolverVisitor {
    
    public QuerySolverVisitorUser() {}
    
    public QuerySolverVisitorUser(Eval e) { super(e); }
    
    @Override
    public IDatatype init(Query q) {
        System.out.println("User defined init:");
        System.out.println(q.getAST());
        return DatatypeMap.TRUE;
    }


}
    
