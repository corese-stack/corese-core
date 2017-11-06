package fr.inria.corese.triple.function.term;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.corese.triple.function.core.BinaryFunction;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class MinusTerm extends BinaryFunction {
    long val;
    boolean isConstant = false; 
        
    public MinusTerm(String name){
        super(name);
    }
    
    public MinusTerm(String name, Expression e1, Expression e2) {
        super(name, e1, e2);
        if (e2.isConstant() && e2.getDatatypeValue().getCode() == IDatatype.INTEGER){
            val = e2.getDatatypeValue().longValue();
            isConstant = true;
        }
    }
    
    public static MinusTerm create(String name, Expression e1, Expression e2) {        
        return new MinusTerm(name, e1, e2);
    }
         
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype dt1 = getExp1().eval(eval, b, env, p);
        if (dt1 == null) {
            return null;
        }
        if (isConstant) {
            return dt1.minus(val);
        }
        else {
            IDatatype dt2 = getExp2().eval(eval, b, env, p);
            if (dt2 == null) {
                return null;
            }
            return dt1.minus(dt2);
        }
    }
      
}
