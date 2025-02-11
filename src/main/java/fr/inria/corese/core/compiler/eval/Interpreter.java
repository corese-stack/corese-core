package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Evaluator;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.event.ResultListener;
import fr.inria.corese.core.sparql.api.*;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Filter exists Evaluator
 *
 * @author Olivier Corby INRIA
 */
public class Interpreter implements Computer, Evaluator, ExprType {

    static final IDatatype[] EMPTY = new IDatatype[0];
    private static final Logger logger = LoggerFactory.getLogger(Interpreter.class);
    public static boolean testNewEval = false;
    public static Mode DEFAULT_MODE = Mode.KGRAM_MODE;
    public static int count = 0;
    Producer producer;
    Eval kgram;
    ResultListener listener;
    int mode = DEFAULT_MODE;
    boolean hasListener = false;
    boolean isDebug = false;
    // fr.inria.corese.core.query.PluginImpl
    private ProxyInterpreter plugin;


    public Interpreter() {
    }

    public static ASTExtension createExtension() {
        return new ASTExtension();
    }

    public static ASTExtension getCreateExtension(Query q) {
        ASTExtension ext = q.getExtension();
        if (ext == null) {
            ext = createExtension();
            q.setExtension(ext);
        }
        return ext;
    }

    public Producer getProducer() {
        return producer;
    }

    @Override
    public void setProducer(Producer p) {
        producer = p;
    }

    @Override
    public void setKGRAM(Eval o) {
    }

    @Override
    public void setDebug(boolean b) {
        isDebug = b;
    }

    @Override
    public void addResultListener(ResultListener rl) {
        listener = rl;
        hasListener = rl != null;
    }

    @Override
    public Evaluator getEvaluator() {
        return this;
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setMode(int m) {
        mode = m;
        getPlugin().setMode(m);
    }

    @Override
    public boolean isCompliant() {
        return mode == SPARQL_MODE;
    }

    @Override
    public void start(Environment env) {
    }

    @Override
    public void init(Environment env) {
        if (env.getBind() == null) {
            env.setBind(getBinder());
        }
    }

    @Override
    public Binding getBinder() {
        return Binding.create();
    }

    @Override
    public void finish(Environment env) {
        getPlugin().finish(producer, env);
    }

    public ProxyInterpreter getComputerPlugin() {
        return getPlugin();
    }

    @Override
    public GraphProcessor getGraphProcessor() {
        return getComputerPlugin().getGraphProcessor();
    }

    public ComputerProxy getComputerTransform() {
        return getComputerPlugin().getComputerTransform();
    }

    @Override
    public TransformProcessor getTransformer(Binding b, Environment env, Producer p) throws EngineException {
        return getComputerTransform().getTransformer(b, env, p);
    }

    @Override
    public TransformProcessor getTransformer(Binding b, Environment env, Producer p, Expr exp, IDatatype uri, IDatatype gname) throws EngineException {
        return getComputerTransform().getTransformer(b, env, p, exp, uri, gname);
    }

    @Override
    public TransformVisitor getVisitor(Binding b, Environment env, Producer p) {
        return getComputerTransform().getVisitor(b, env, p);
    }

    @Override
    public Context getContext(Binding b, Environment env, Producer p) {
        return getComputerTransform().getContext(b, env, p);
    }

    @Override
    public NSManager getNSM(Binding b, Environment env, Producer p) {
        return getComputerTransform().getNSM(b, env, p);
    }

    public ProxyInterpreter getPlugin() {
        return plugin;
    }

    // for PluginImpl
    public void setPlugin(ProxyInterpreter plugin) {
        this.plugin = plugin;
        if (plugin.getEvaluator() == null) {
            plugin.setEvaluator(this);
        }
    }

}
