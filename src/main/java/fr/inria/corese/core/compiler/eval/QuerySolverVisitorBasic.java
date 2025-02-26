package fr.inria.corese.core.compiler.eval;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.query.*;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.PointerObject;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.extension.ListSort;
import fr.inria.corese.core.sparql.triple.function.script.Funcall;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.function.script.LDScript;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Callback manager for LDScript functions with specific annotations Eval SPARQL
 * processor calls before() and after()
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 * @before function us:before(?q) {}
 * @after function us:after(?m) {}
 * @produce function us:produce(?q) {}
 * @candidate function us:candidate(?q, ?e) {}
 * @result function us:result(?m) {}
 * @solution function us:solution(?m) {}
 */
public class QuerySolverVisitorBasic extends PointerObject implements ProcessVisitor {

    public static final String RECURSION = "@recursion";
    public static final String VERBOSE = "@verbose";
    public static final String SHARE = "@share";
    public static final String TRACE = "@display";
    public static final String PREPARE = "@prepare";
    public static final String INIT = "@init";
    public static final String BEFORE = "@before";
    public static final String AFTER = "@after";
    public static final String CONSTRUCT = "@construct";
    public static final String BEFORE_UPDATE = "@beforeUpdate";
    public static final String AFTER_UPDATE = "@afterUpdate";
    public static final String BEFORE_LOAD = "@beforeLoad";
    public static final String AFTER_LOAD = "@afterLoad";
    public static final String BEFORE_ENTAIL = "@beforeEntailment";
    public static final String AFTER_ENTAIL = "@afterEntailment";
    public static final String ENTAILMENT = "@entailment";
    public static final String PREPARE_ENTAIL = "@prepareEntailment";
    public static final String LOOP_ENTAIL = "@loopEntailment";
    public static final String BEFORE_RULE = "@beforeRule";
    public static final String AFTER_RULE = "@afterRule";
    public static final String CONSTRAINT_RULE = "@constraintRule";
    public static final String START = "@start";
    public static final String FINISH = "@finish";
    public static final String OVERLOAD = "@overload";
    public static final String LIMIT = "@limit";
    public static final String TIMEOUT = "@timeout";
    public static final String SLICE = "@slice";
    public static final String ORDERBY = "@orderby";
    public static final String DISTINCT = "@distinct";
    public static final String PRODUCE = "@produce";
    public static final String RESULT = "@result";
    public static final String STATEMENT = "@statement";
    public static final String CANDIDATE = "@candidate";
    public static final String PATH = "@path";
    public static final String STEP = "@step";
    public static final String VALUES = "@values";
    public static final String BIND = "@bind";
    public static final String BGP = "@bgp";
    public static final String JOIN = "@join";
    public static final String OPTIONAL = "@optional";
    public static final String MINUS = "@minus";
    public static final String UNION = "@union";
    public static final String FILTER = "@filter";
    public static final String SELECT = "@select";
    public static final String SERVICE = "@service";
    public static final String QUERY = "@query";
    public static final String GRAPH = "@graph";
    public static final String AGGREGATE = "@aggregate";
    public static final String HAVING = "@having";
    public static final String FUNCTION = "@function";
    public static final String INSERT = "@insert";
    public static final String DELETE = "@delete";
    public static final String UPDATE = "@update";
    public static final int UPDATE_ARITY = 3;
    public static final String INIT_PARAM = "@initParam";
    public static final String INIT_SERVER = "@initServer";
    public static final String BEFORE_TRANSFORMER = "@beforeTransformer";
    public static final String AFTER_TRANSFORMER = "@afterTransformer";
    public static final String BEFORE_WORKFLOW = "@beforeWorkflow";
    public static final String AFTER_WORKFLOW = "@afterWorkflow";
    static final String[] EVENT_LIST = {
            BEFORE, AFTER, START, FINISH, PRODUCE, RESULT, STATEMENT, CANDIDATE, PATH, STEP, VALUES, BIND,
            BGP, JOIN, OPTIONAL, MINUS, UNION, FILTER, SELECT, SERVICE, QUERY, GRAPH,
            AGGREGATE, HAVING, FUNCTION, ORDERBY, DISTINCT
    };
    private static final Logger logger = LoggerFactory.getLogger(QuerySolverVisitorBasic.class);
    public static boolean REENTRANT_DEFAULT = false;
    private static boolean event = true;
    boolean select = false;
    Query query;
    ASTQuery ast;
    HashMap<Environment, IDatatype> distinct;
    QuerySolverOverload overload;
    private boolean active = false;
    private boolean reentrant = REENTRANT_DEFAULT;
    private boolean shareable = false;
    private boolean function = false;
    private Eval eval;

    public QuerySolverVisitorBasic() {
        distinct = new HashMap<>();
        overload = new QuerySolverOverload(this);
    }

    public QuerySolverVisitorBasic(Eval e) {
        this();
        eval = e;
    }

    /**
     * @return the event
     */
    public static boolean isEvent() {
        return event;
    }

    /**
     * @param aEvent the event to set
     */
    public static void setEvent(boolean aEvent) {
        event = aEvent;
    }

    @Override
    public Eval getProcessor() {
        return getEval();
    }

    @Override
    public void setProcessor(Eval e) {
        setEval(e);
    }

    Hierarchy getHierarchy() {
        return getEnvironment().getExtension().getHierarchy();
    }

    public IDatatype datatype(IDatatype type, IDatatype sup) {
        getHierarchy().defSuperType(type, sup);
        return type;
    }

    List<String> getSuperTypes(IDatatype type) {
        return getHierarchy().getSuperTypes(null, type);
    }

    String getSuperType(IDatatype type) {
        List<String> list = getSuperTypes(type);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    void initialize() {
        // Visitor shared among different query processing:
        // query in function, transformer
        setShareable(ast.hasMetadata(SHARE));
        setFunction(define(FUNCTION, 2));
        if (ast.hasMetadata(RECURSION)) {
            // Visitor still active inside event function
            // by default it is not to prevent loop
            setReentrant(true);
        }
    }

    /**
     * If query has @event @before, process only @before event
     */
    void setSelect() {
        if (ast.getMetadata() != null) {
            for (String name : EVENT_LIST) {
                if (ast.getMetadata().hasMetadata(name)) {
                    select = true;
                    break;
                }
            }
        }
    }

    boolean accept(String name) {
        if (select) {
            return ast.getMetadata().hasMetadata(name);
        }
        return true;
    }

    public boolean isOverload() {
        return overload.isOverload();
    }

    public void setOverload(boolean b) {
        overload.setOverload(b);
    }

    public boolean define(String name, int arity) {
        Expr exp = getDefineMetadata(getEnvironment(), name, arity);
        return (exp != null);
    }

    /**
     * @before function us:before(?q) call function us:before set Visitor as
     * inactive during function call to prevent loop and also in case where
     * function execute a query (which would trigger Visitor recursively)
     */
    public IDatatype callback(String metadata, IDatatype[] param) {
        return callback(getEval(), metadata, param);
    }

    public IDatatype callback(Eval ev, String metadata, IDatatype[] param) {
        if (isRunning() || !isEvent() || !accept(metadata)) {
            return null;
        }
        Function function = getDefineMetadata(getEnvironment(), metadata, param.length);
        if (function != null) {
            // prevent infinite loop in case where there is a query in the function
            setActive(true);
            IDatatype dt = call(function, param, ev.getEvaluator(), ev.getEnvironment(), ev.getProducer());
            setActive(false);
            return dt;
        }
        return null;
    }

    /**
     * @eq function us:eq(?e, ?x, ?y)
     * @error function us:error(?e, ?x, ?y) Function call is performed even if
     * Visitor is inactive use case: @select function execute ?a = ?b on
     * extension datatype we want @eq function us:eq(?e, ?x, ?y) to handle ?a =
     * ?b
     */
    public IDatatype callbackBasic(Eval ev, String metadata, IDatatype[] param) {
        return callbackSimple(ev, metadata, param);
    }

    Function getDefineMetadata(Environment env, String metadata, int n) {
        return new LDScript().getDefineMetadata(env, metadata, n);
    }

    Function getDefineMethod(Environment env, String name, IDatatype type, IDatatype[] param) {
        return new LDScript().getDefineMethod(env, name, type, param);
    }

    Function getDefine(Environment env, String name, int n) {
        return new LDScript().getDefine(env, name, n);
    }

    IDatatype callbackSimple(Eval ev, String metadata, IDatatype[] param) {
        Function function = getDefineMetadata(getEnvironment(), metadata, param.length);
        if (function != null) {
            return call(function, param, ev.getEvaluator(), ev.getEnvironment(), ev.getProducer());
        }
        return null;
    }

    // param = Mappings map
    IDatatype sort(Eval ev, String metadata, IDatatype[] param) {
        if (isRunning() || !accept(metadata)) {
            return null;
        }
        // function us:compare(?m1, ?m2)
        Function function = getDefineMetadata(getEnvironment(), metadata, 2);
        if (function != null) {
            // prevent infinite loop in case where there is a query in the function
            setActive(true);
            IDatatype dt = new ListSort("sort").sort(ev.getEvaluator(), ev.getEnvironment().getBind(), ev.getEnvironment(),
                    ev.getProducer(), function, param[0]);
            setActive(false);
            return dt;
        }
        return null;
    }

    public IDatatype method(Eval ev, String name, IDatatype[] param) {
        return method(ev, name, null, param);
    }

    public IDatatype method(Eval ev, String name, IDatatype type, IDatatype[] param) {
        if (isRunning()) {
            return null;
        }
        Function exp = getDefineMethod(getEnvironment(), name, type, param);
        if (exp != null) {
            setActive(true);
            IDatatype dt = call(exp, param, ev.getEvaluator(), ev.getEnvironment(), ev.getProducer());
            setActive(false);
            return dt;
        }
        return null;
    }

    public IDatatype methodBasic(Eval ev, String name, IDatatype[] param) {
        return methodBasic(ev, name, null, param);
    }

    public IDatatype methodBasic(Eval ev, String name, IDatatype type, IDatatype[] param) {
        Function exp = getDefineMethod(getEnvironment(), name, type, param);
        if (exp != null) {
            return call(exp, param, ev.getEvaluator(), ev.getEnvironment(), ev.getProducer());
        }
        return null;
    }

    IDatatype call(Function fun, IDatatype[] param, Evaluator eval, Environment env, Producer p) {
        try {
            return new Funcall(fun.getFunction().getLabel()).callWE(eval, env.getBind(), env, p, fun, param);
        } catch (EngineException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    public IDatatype funcall(Eval ev, String name, IDatatype[] param) {
        return funcall(name, param, ev.getEvaluator(), ev.getEnvironment(), ev.getProducer());
    }

    IDatatype funcall(String name, IDatatype[] param, Evaluator eval, Environment env, Producer p) {
        Function fun = getDefine(env, name, param.length);
        if (fun != null) {
            return call(fun, param, eval, env, p);
        }
        return null;
    }

    public IDatatype toDatatype(List<Edge> list) {
        ArrayList<IDatatype> res = new ArrayList<>();
        for (Edge edge : list) {
            if (edge != null) {
                res.add(DatatypeMap.createObject(edge));
            }
        }
        return DatatypeMap.newList(res);
    }

    public IDatatype[] toArray(Object... lobj) {
        IDatatype[] param = new IDatatype[lobj.length];
        int i = 0;
        for (Object obj : lobj) {
            param[i++] = (obj == null) ? null : DatatypeMap.getValue(obj);
        }
        return param;
    }

    Environment getEnvironment() {
        return getEval().getEnvironment();
    }

    @Override
    public boolean isShareable() {
        return shareable;
    }

    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }

    boolean isRunning() {
        if (isReentrant()) {
            return false;
        }
        return isActive();
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    public void sleep(boolean b) {
        setActive(b);
    }

    /**
     * @return the reentrant
     */
    public boolean isReentrant() {
        return reentrant;
    }

    /**
     * @param reentrant the reentrant to set
     */
    public void setReentrant(boolean reentrant) {
        this.reentrant = reentrant;
    }

    /**
     * @return the function
     */
    public boolean isFunction() {
        return function;
    }

    /**
     * @param function the function to set
     */
    public void setFunction(boolean function) {
        this.function = function;
    }

    /**
     * @return the eval
     */
    public Eval getEval() {
        return eval;
    }

    /**
     * @param eval the eval to set
     */
    public void setEval(Eval eval) {
        this.eval = eval;
    }

}
