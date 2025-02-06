package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.sparql.api.ComputerProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.event.EvalListener;
import fr.inria.corese.core.sparql.api.GraphProcessor;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.parser.Context;

/**
 * Evaluator of operators & functions of filter language 
 * implemented by fr.inria.corese.core.query.PluginImpl
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 * 
 */
public abstract class ProxyInterpreter implements ExprType {

    private static final String URN_UUID = "urn:uuid:";
    private static Logger logger = LoggerFactory.getLogger(ProxyInterpreter.class);

    static final String UTF8 = "UTF-8";
    public static final String RDFNS = NSManager.RDF;
    public static final String RDFTYPE = RDFNS + "type";
    private static final String USER_DISPLAY = NSManager.USER + "display";
    public static int count = 0;
    //ProxyInterpreter plugin;
    Custom custom;
    SQLFun sql;
    Interpreter eval;
    private Producer producer;
    EvalListener el;
    // for LDScript java compiling only
    private Environment environment;
    int number = 0;
    // KGRAM is relax wrt to string vs literal vs uri input arg of functions
    // eg regex() concat() strdt()
    // setMode(SPARQL_MODE) 
    boolean SPARQLCompliant = false;
    protected IDatatype EMPTY = DatatypeMap.newStringBuilder("");

    protected ProxyInterpreter() {
    }

    public void setEvaluator(Interpreter ev) {
        eval =  ev;
    }

    public Interpreter getEvaluator() {
        return eval;
    }

    public GraphProcessor getGraphProcessor() {
        return null;
    }

    public ComputerProxy getComputerTransform() {
        return null;
    }

    public abstract void setMode(int mode);

    public void start() {
        number = 0;
    }

    
    public Producer getProducer() {
        return producer;
    }

    
    // for Core & Extension
    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public IDatatype getValue(boolean b) {
        if (b) {
            return DatatypeMap.TRUE;
        } else {
            return DatatypeMap.FALSE;
        }
    }

    public IDatatype getValue(int value) {
        return DatatypeMap.newInstance(value);
    }

    public IDatatype getValue(long value) {
        return DatatypeMap.newInstance(value);
    }

    public IDatatype getValue(float value) {
        return DatatypeMap.newInstance(value);
    }

    public IDatatype getValue(double value) {
        return DatatypeMap.newInstance(value);
    }

    public IDatatype getValue(double value, String datatype) {
        return DatatypeMap.newInstance(value, datatype);
    }

    // return xsd:string
    public IDatatype getValue(String value) {
        return DatatypeMap.newInstance(value);
    }

    public Function getDefine(Expr exp, Environment env, String name, int n) throws EngineException {
        //return plugin.getDefine(exp, env, name, n);
        return null;
    }

    public abstract void start(Producer p, Environment env);

    public abstract void finish(Producer p, Environment env);
   
    public Environment getEnvironment() {
        return environment;
    }

    public Context getContext() {
        return getEval().getEvaluator().getContext(getEnvironment().getBind(), getEnvironment(), getProducer());
    }

    public Eval getEval() {
        return getEnvironment().getEval();
    }
    
    // for Core & Extension
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
