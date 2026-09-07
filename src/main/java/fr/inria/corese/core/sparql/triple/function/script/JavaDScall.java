package fr.inria.corese.core.sparql.triple.function.script;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.SafetyException;
import fr.inria.corese.core.sparql.triple.parser.Access.Feature;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Call Corese Java function with parameters as IDatatype values
 * and this object possibly as getObject()
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
@SuppressWarnings("java:S110") // Corese extension function hierarchy exceeds 5 parents by design
public final class JavaDScall extends JavaFunction {

    private static final Logger logger = LoggerFactory.getLogger(JavaDScall.class);
    
    private String javaName;

    public JavaDScall() {}
    
    public JavaDScall(String name) {
        super(name); 
        javaName = name.substring(name.indexOf(":")+1);
        setArity(1);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        check(Feature.JAVA_FUNCTION, b, JAVA_FUNCTION_MESS);
        IDatatype dt   = getBasicArg(0).eval(eval, b, env, p);
        IDatatype[] param = evalArguments(eval, b, env, p, 1);  
        if (dt == null || param == null) {
            return null;
        }
        Object object = dt;
        if (dt.getNodeObject() != null) {
            object = dt.getNodeObject();
        }
        
        Class<?>[] types = new Class<?>[param.length];
        Arrays.fill(types, IDatatype.class);
        
        try {
            Method meth = object.getClass().getMethod(javaName, types);
            return DatatypeMap.getValue(meth.invoke(object, (Object[]) param));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("An unexpected error has occurred", ex);
        }
        return null;

    }
         
}
