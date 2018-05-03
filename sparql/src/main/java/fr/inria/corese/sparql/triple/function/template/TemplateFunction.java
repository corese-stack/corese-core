package fr.inria.corese.sparql.triple.function.template;

import fr.inria.corese.kgram.api.core.ExprType;
import fr.inria.corese.sparql.api.Computer;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.triple.function.term.Binding;
import fr.inria.corese.sparql.triple.function.term.TermEval;
import fr.inria.corese.kgram.api.query.Environment;
import fr.inria.corese.kgram.api.query.Producer;
import fr.inria.corese.sparql.datatype.DatatypeMap;
import fr.inria.corese.sparql.triple.parser.ASTQuery;
import fr.inria.corese.sparql.triple.parser.Constant;
import fr.inria.corese.sparql.triple.parser.Expression;
import fr.inria.corese.sparql.triple.parser.Term;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class TemplateFunction extends TermEval {  
    
    private String proxy;
    
    public TemplateFunction(String name){
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype[] param = evalArguments(eval, b, env, p, 0);
        if (param == null){
            return null;
        }

        switch (param.length){
            case 0:  return eval.getComputerTransform().function(this, env, p); 
            case 1:  return eval.getComputerTransform().function(this, env, p, param[0]); 
            case 2:  return eval.getComputerTransform().function(this, env, p, param[0], param[1]); 
            default: return eval.getComputerTransform().eval(this, env, p, param); 
        }
    }
    
  /**
     * st:format (e1, st:number(), e2)
     * Evaluate r1=eval(e1), r2=eval(e2) and freeze st:number(), until order by occurs
     * Generate a future datatype that will be evaluated by Template final Aggregate
     * after possible order by:
     * future(format(r1, st:number(), r2)
     */
    IDatatype future(Computer eval, Binding b, Environment env, Producer p) { 
        ArrayList<Expression> list = new ArrayList<>();
        for (Expression exp : getArgs()) {
            if (isFuture(exp) || exp.isConstant()) {
                // constant need not to be evaluated
                list.add(clean(exp));
            }
            else {
                IDatatype dt = exp.eval(eval, b, env, p);
                if (dt == null) {
                    return null;
                }
                else if (dt.isFuture()) {
                    list.add((Expression) dt.getObject());
                }
                else {
                    list.add(constant(dt));
                }
            }
        }
        ASTQuery ast = (ASTQuery) env.getQuery().getAST();
        Term t = ast.createFunction(getProxy(), list);
        return DatatypeMap.createFuture(t);
    }
    
    /**
     * Generate a fake Constant to store the future IDatatype
     */
    Constant constant(IDatatype dt) {
        Constant cst = Constant.create("Future", null, null);
        cst.setDatatypeValue(dt);
        return cst;
    }
    
    boolean isFuture() {
        for (Expression exp : getArgs()) {
            if (isFuture(exp)) {
                return true;
            }
        }
        return false;
    }
    
    boolean isFuture(Expression exp) {
        return exp.oper() == ExprType.STL_NUMBER || exp.oper() == ExprType.STL_FUTURE;
    }
    
    Expression clean(Expression exp) {
        if (exp.oper() == ExprType.STL_FUTURE && exp.arity() > 0) {
            return exp.getArg(0);
        }
        return exp;
    }
    
     /**
     * @return the proxy
     */
    public String getProxy() {
        return proxy;
    }

    /**
     * @param proxy the proxy to set
     */
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
    
    IDatatype[] getParam(IDatatype[] obj, int n){
        return Arrays.copyOfRange(obj, n, obj.length);
    }
        
   
}

