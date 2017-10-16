package fr.inria.corese.triple.function.script;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.acacia.corese.triple.parser.Function;
import fr.inria.corese.triple.function.term.Binding;
import fr.inria.corese.triple.function.term.TermEval;
import fr.inria.edelweiss.kgram.api.core.Expr;
import static fr.inria.edelweiss.kgram.api.core.ExprType.AND;
import static fr.inria.edelweiss.kgram.api.core.ExprType.CONCAT;
import static fr.inria.edelweiss.kgram.api.core.ExprType.DIV;
import static fr.inria.edelweiss.kgram.api.core.ExprType.MINUS;
import static fr.inria.edelweiss.kgram.api.core.ExprType.MULT;
import static fr.inria.edelweiss.kgram.api.core.ExprType.OR;
import static fr.inria.edelweiss.kgram.api.core.ExprType.PLUS;
import static fr.inria.edelweiss.kgram.api.core.ExprType.XT_APPEND;
import static fr.inria.edelweiss.kgram.api.core.ExprType.XT_MERGE;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import java.util.List;

/**
 * reduce(rq:plus, list)
 * get a binary function, apply it on elements two by two
 * a kind of aggregate
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class Reduce extends Funcall {  
    
    public Reduce(String name){
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype name    = getArg(0).eval(eval, b, env, p);
        IDatatype[] param = evalArguments(eval, b, env, p, 1);
        if (name == null || param == null || param.length == 0) {
            return null;
        }
                
        Function function = (Function) eval.getDefineGenerate(this, env, name.stringValue(), 2);
        boolean isSystem = function.isSystem();
        IDatatype dt = param[0];
        if (! dt.isList()) {
            return null;
        }
        List<IDatatype> list = dt.getValues();
        if (list.isEmpty()){
            return neutral(function, dt);
        }
        IDatatype[] value = new IDatatype[2];
        IDatatype res = list.get(0);
        value[0] = res;
        // Iterate the value list in order to perform binary function call
        // reduce (rq:plus, list) -> for all (x, y) in list : rq:plus(x, y)
        for (int i = 1; i < list.size(); i++) {            
            value[1] = list.get(i);            
            // binary function call
            res = call(eval, b, env, p, function, value);            
            if (res == null) {
               return null;
            }
            value[0] = res;
        }
        return res;
    }
    
    IDatatype neutral(Expr exp, IDatatype dt){
        switch (exp.oper()){
            case OR:
                return FALSE;
                
            case AND:
                return TRUE;
                
            case CONCAT:
                return DatatypeMap.EMPTY_STRING;
                
            case PLUS:
            case MINUS:
                return DatatypeMap.ZERO;
                
            case MULT:
            case DIV:
                return DatatypeMap.ZERO; 
                
            case XT_APPEND:
            case XT_MERGE:
                return DatatypeMap.EMPTY_LIST;
                
            default: return dt;
        }
    }
    
    
}
