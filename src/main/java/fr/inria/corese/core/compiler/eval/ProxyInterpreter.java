package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Evaluator;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.sparql.api.ComputerProxy;
import fr.inria.corese.core.sparql.api.GraphProcessor;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluator of operators &amp; functions of filter language
 * implemented by fr.inria.corese.core.query.PluginImpl
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public abstract class ProxyInterpreter implements ExprType {

    public static final String RDFNS = NSManager.RDF;
    private static final Logger logger = LoggerFactory.getLogger(ProxyInterpreter.class);
    public static int count = 0;
    protected IDatatype EMPTY = DatatypeMap.newStringBuilder("");
    Interpreter eval;
    int number = 0;
    private Producer producer;
    // for LDScript java compiling only
    private Environment environment;

    protected ProxyInterpreter() {
    }

    public Interpreter getEvaluator() {
        return eval;
    }

    public void setEvaluator(Interpreter ev) {
        eval = ev;
    }

    public GraphProcessor getGraphProcessor() {
        return null;
    }

    public ComputerProxy getComputerTransform() {
        return null;
    }

    public abstract void setMode(Evaluator.Mode mode);

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
        return null;
    }

    public abstract void start(Producer p, Environment env);

    public abstract void finish(Producer p, Environment env);

    public Environment getEnvironment() {
        return environment;
    }

    // for Core & Extension
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Context getContext() {
        return getEval().getEvaluator().getContext(getEnvironment().getBind(), getEnvironment(), getProducer());
    }

    public Eval getEval() {
        return getEnvironment().getEval();
    }

}
