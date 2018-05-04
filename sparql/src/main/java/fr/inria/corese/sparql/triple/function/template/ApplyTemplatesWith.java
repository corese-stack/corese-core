package fr.inria.corese.sparql.triple.function.template;

import fr.inria.corese.sparql.api.Computer;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.triple.function.term.Binding;
import fr.inria.corese.kgram.api.query.Environment;
import fr.inria.corese.kgram.api.query.Producer;
import fr.inria.corese.sparql.api.TransformProcessor;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class ApplyTemplatesWith extends TemplateFunction {  
        
    public ApplyTemplatesWith(String name){
        super(name);
        setArity(1);
    }
    
    /**
     * st:apply-templates-with(st:turtle, ?x)
     */
    @Override
     public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype[] param = evalArguments(eval, b, env, p, 0);
        if (param == null) {
            return null;
        }

        TransformProcessor trans = eval.getComputerTransform().getTransformer(env, p, this, param[0], null, null);

        switch (param.length) {

            case 1:
                return trans.process(null, isAll(), getModality());
            case 2:
                return trans.process(param[1], null, null, isAll(), getModality(), this, env);
            default:
                return trans.process(param[1], getParam(param, 1), null, isAll(), getModality(), this, env);
        }
    }
     
    //@Override
//    public IDatatype eval2(Computer eval, Binding b, Environment env, Producer p) {
//        IDatatype[] param = evalArguments(eval, b, env, p, 0);
//        if (param == null){
//            return null;
//        }
//        switch (param.length){
//            case 1:  return eval.getComputerTransform().transform(param[0], null, null, this, env, p); 
//            case 2:  return eval.getComputerTransform().transform(null, param[1], param[0], null, null, this, env, p); 
//            default: return eval.getComputerTransform().transform(getParam(param, 1), param[1], param[0], null, null, this, env, p); 
//        }
//    }
   
   
}

