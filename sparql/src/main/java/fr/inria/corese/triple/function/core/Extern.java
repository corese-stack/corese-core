package fr.inria.corese.triple.function.core;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.triple.parser.Processor;
import fr.inria.corese.triple.function.term.Binding;
import fr.inria.corese.triple.function.term.TermEval;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class Extern extends TermEval {

    public Extern(String name) {
        super(name);
    }
    
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype[] param = evalArguments(eval, b, env, p, 0);
        if (param == null || ! getProcessor().isCorrect()){
            return null;
        } 
        Processor proc = getProcessor();
        proc.compile();
        if (proc instanceof FunctionEvaluator){
            FunctionEvaluator fe = (FunctionEvaluator) proc;
            fe.setProducer(p);
            fe.setEnvironment(env);
        }
        String name = proc.getMethod().getName();
        try {
            return (IDatatype) proc.getMethod().invoke(proc.getProcessor(), param);
        } catch (IllegalArgumentException e) {
           trace(e, "eval", name, param);
        } catch (IllegalAccessException e) {
            trace(e, "eval", name, param);
        } catch (InvocationTargetException e) {
           trace(e, "eval", name, param);
        } catch (NullPointerException e) {
           trace(e, "eval", name, param); 
        }
        return null;
    }
    
    void trace(Exception e, String title, String name, IDatatype[] ldt){
        String str = "";
        for (IDatatype dt : ldt) {
            str += dt + " ";
        }
        TermEval.logger.error(e);
        TermEval.logger.error(title + " "+ name + " " + str);  
    }
    
}
