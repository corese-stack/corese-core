package fr.inria.corese.core.sparql.triple.function.template;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.TransformProcessor;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class ApplyTemplates extends TemplateFunction {

    public ApplyTemplates(String name) {
        super(name);
    }

    /**
     * st:apply-templates(?x)
     */
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype[] param = evalArguments(eval, b, env, p, 0);
        if (param == null) {
            return null;
        }

        TransformProcessor trans = eval.getTransformer(b, env, p, this, null, null);

        switch (param.length) {
            case 0:
                return trans.process(null, isAll(), getModality(), this, env);
            case 1:
                return trans.process(null, isAll(), getModality(), this, env, param[0], null);
            default:
                return trans.process(null, isAll(), getModality(), this, env, param[0], param);
        }
    }
    

}
