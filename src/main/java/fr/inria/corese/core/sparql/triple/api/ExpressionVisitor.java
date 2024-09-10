package fr.inria.corese.core.sparql.triple.api;

import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Exp;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.parser.Term;
import fr.inria.corese.core.sparql.triple.parser.Variable;

/**
 *
 * @author corby
 */
public interface ExpressionVisitor {
    
    void start(Expression exp);
    
    void visit(Exp exp);
    
    void visit(Term t);
    
    void visit(Function f);

    void visit(Variable v);
    
    void visit(Constant c);
        
}
