package fr.inria.corese.core.sparql.triple.function.template;

import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.function.term.Binding;



/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class TemplateFuture extends TemplateFunction {

    public TemplateFuture(String name) {
        super(name);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {     
        return DatatypeMap.createFuture(this.getArg(0));
    }   
       
}
