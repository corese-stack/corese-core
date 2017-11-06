package fr.inria.corese.triple.function.script;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.acacia.corese.triple.parser.Term;
import fr.inria.corese.triple.function.term.Binding;
import fr.inria.corese.triple.function.term.TermEval;
import fr.inria.edelweiss.kgram.api.core.Expr;
import fr.inria.edelweiss.kgram.api.core.ExprType;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LDScript function call
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class Extension extends TermEval {

    private static Logger logger = LogManager.getLogger(Extension.class);
    Function function;
    boolean isUnary = false, isBinary = false, isSystem = false;
    private boolean tailRecursion = false;
    Expression var1, var2, exp1, exp2, body;
    Term signature;
    List<Expr> arguments;
    Computer cc;

    public Extension(String name) {
        super(name);
    }

    void init() {
        body = function.getBody();
        isSystem = function.isSystem();
        if (!isSystem) {
            isUnary = arity() == 1;
            isBinary = arity() == 2;
        }
        if (isUnary) {
            var1 = function.getSignature().getArg(0);
            exp1 = getArg(0);
        } else if (isBinary) {
            var1 = function.getSignature().getArg(0);
            var2 = function.getSignature().getArg(1);
            exp1 = getArg(0);
            exp2 = getArg(1);

        } else {
            arguments = function.getSignature().getExpList();
        }
        // tr();
    }


    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        if (function == null) {
            function = (Function) eval.getDefine(this, env);
            if (function == null) {
                logger.error("Undefined function: " + this);
                return null;
            } else {
                init();
            }
        }
        
        if (isUnary) {
            IDatatype value1 = exp1.eval(eval, b, env, p);
            if (value1 == null) {
                return null;
            }
            IDatatype dt;
            if (tailRecursion) {
                b.setTailRec(function, var1, value1);
                dt = body.eval(eval, b, env, p);
            } else {
                b.set(function, var1, value1);
                dt = body.eval(eval, b, env, p);
                b.unset(function);
            }
            if (dt == null) {
                return null;
            }
            return b.resultValue(dt);
        } else if (isBinary) {
            IDatatype value1 = exp1.eval(eval, b, env, p);
            IDatatype value2 = exp2.eval(eval, b, env, p);
            if (value1 == null || value2 == null) {
                return null;
            }
            b.set(function, var1, value1, var2, value2);
            IDatatype dt = body.eval(eval, b, env, p);
            b.unset(function);
            if (dt == null) {
                return null;
            }
            return b.resultValue(dt);
        } else {
            IDatatype[] param = evalArguments(eval, b, env, p, 0);
            if (param == null) {
                return null;
            }
            b.set(function, arguments, param);
            IDatatype dt = null;
            if (isSystem) {
                Computer cc = eval.getComputer(env, p, function);
                // PRAGMA: b = cc.getEnvironment().getBind()
                dt = body.eval(cc, b, cc.getEnvironment(), p);
            } else {
                dt = body.eval(eval, b, env, p);
            }
            b.unset(function, arguments);
            if (dt == null) {
                return null;
            }
            return b.resultValue(dt);

        }

    }

    /**
     * Eval with param already computed Use case: xt:main(), xt:produce(?q)
     *
     * @param eval
     * @param b
     * @param env
     * @param p
     * @param param
     * @return
     */
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p, IDatatype[] param) {
        if (function == null) {
            function = (Function) eval.getDefine(this, env);
            if (function == null) {
                logger.error("Undefined function: " + this);
                return null;
            }
        }

        Expression fun = function.getSignature();
        IDatatype dt;
        b.set(function, fun.getExpList(), param);
        if (function.isSystem()) {
            Computer cc = eval.getComputer(env, p, function);
            // PRAGMA: b = cc.getEnvironment().getBind()
            dt = function.getBody().eval(cc, b, cc.getEnvironment(), p);
        } else {
            dt = function.getBody().eval(eval, b, env, p);
        }
        b.unset(function, fun.getExpList());

        if (dt == null) {
            return null;
        }
        //return DatatypeMap.getResultValue(dt);
        return b.resultValue(dt);

    }

    /**
     * @return the tailRecursion
     */
    public boolean isTailRecursion() {
        return tailRecursion;
    }

    /**
     * @param tailRecursion the tailRecursion to set
     */
    public void setTailRecursion(boolean tailRecursion) {
        this.tailRecursion = tailRecursion;
    }
    
    @Override
    public void tailRecursion(Function fun){
        if (getName().equals(fun.getSignature().getName())
                && getArity() == fun.getSignature().getArity()){
            setTailRecursion(true);
        }
    }
}
