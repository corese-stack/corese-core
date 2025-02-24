package fr.inria.corese.core.visitor.solver;

import fr.inria.corese.core.transform.Transformer;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 *
 * @author corby
 */
public class QuerySolverVisitorTransformerUser extends QuerySolverVisitorTransformer {
    
    public QuerySolverVisitorTransformerUser() {}
    
    public QuerySolverVisitorTransformerUser(Transformer t, Eval e) { super(t, e); }
    
    public QuerySolverVisitorTransformerUser(Eval e) { super(e); }

    
    @Override
    public IDatatype afterTransformer(String uri, String res) {
        return DatatypeMap.TRUE;
    }


}
    
