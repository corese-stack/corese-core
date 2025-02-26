package fr.inria.corese.core.extension.core;

import fr.inria.corese.core.compiler.eval.Interpreter;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.function.core.FunctionEvaluator;
import fr.inria.corese.core.compiler.parser.NodeImpl;
import fr.inria.corese.core.kgram.api.core.Loopable;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.producer.DataProducer;
import fr.inria.corese.core.query.PluginTransform;
import fr.inria.corese.core.query.QueryProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Root of external java function evaluator for SPARQL extension function with
 * JavaCompiler : class Datashape extends Core environment and producer are set
 * by Extern function call
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class Core implements FunctionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(Core.class);

    private static final String QM = "?";

    private Environment environment;
    private Producer producer;
    private Computer eval;

    public Core() {
    }

    public IDatatype self(IDatatype x) {
        return x;
    }

    public IDatatype error() {
        return null;
    }

    public PluginTransform getPluginTransform() {
        return (PluginTransform) ((Interpreter)eval).getComputerTransform();
    }

    String javaName(IDatatype dt) {
        return NSManager.nstrip(dt.getLabel());
    }

    /**
     * LDScript Java compiler
     */
    public IDatatype funcall(IDatatype fun, IDatatype... ldt) {
        String name = javaName(fun);
        try {
            Class<IDatatype>[] aclasses = new Class[ldt.length];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            Method m = this.getClass().getMethod(name, aclasses);
            return (IDatatype) m.invoke(this, ldt);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            logger.error("", e);
        }
        return null;
    }

    /**
     * LDScript Java compiler ldt[0] is a list
     */
    public IDatatype map(IDatatype fun, IDatatype... ldt) {
        String name = javaName(fun);
        try {
            Class<IDatatype>[] aclasses = new Class[ldt.length];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            Method m = this.getClass().getMethod(name, aclasses);
            IDatatype list = ldt[0];
            for (IDatatype dt : list.getValueList()) {
                ldt[0] = dt;
                m.invoke(this, ldt);
            }
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            logger.error("", e);
        }
        return null;
    }

    public IDatatype maplist(IDatatype fun, IDatatype... ldt) {
        String name = javaName(fun);
        try {
            Class<IDatatype>[] aclasses = new Class[ldt.length];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            Method m = this.getClass().getMethod(name, aclasses);
            IDatatype list = ldt[0];
            ArrayList<IDatatype> res = new ArrayList<IDatatype>();
            for (IDatatype dt : list.getValues()) {
                ldt[0] = dt;
                IDatatype obj = (IDatatype) m.invoke(this, ldt);
                if (obj != null) {
                    res.add(obj);
                }
            }
            return DatatypeMap.newInstance(res);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            logger.error("", e);
        }
        return null;
    }

    /**
     * This PluginImpl was created for executing a Method such as java:report()
     * where java: = <function:// ...>
     * This PluginImpl contains Environment and Producer use case: JavaCompiler
     * external function
     */
    public IDatatype kgram(IDatatype query, IDatatype... ldt) {
        Graph g = getGraph(getProducer());
        Mapping m = null;
        if (ldt.length > 0) {
            m = createMapping(getProducer(), ldt, 0);
        }
        QueryProcess exec = QueryProcess.create(g, true);
        try {
            Query q = exec.compile(query.getLabel());
            q.complete(getEnvironment().getQuery(), getPluginTransform().getContext());
            Mappings map = exec.sparqlQuery(q, m, null);
            if (map.getGraph() == null) {
                return DatatypeMap.createObject(map);
            } else {
                return DatatypeMap.createObject(map.getGraph());
            }
        } catch (EngineException e) {
            return DatatypeMap.createObject(new Mappings());
        }
    }

    /**
     * First param is query other param are variable bindings (variable, value)
     */
    Mapping createMapping(Producer p, IDatatype[] param, int start) {
        ArrayList<Node> variables = new ArrayList<>();
        ArrayList<Node> val = new ArrayList<>();
        for (int i = start; i < param.length; i += 2) {
            variables.add(NodeImpl.createVariable(clean(param[i].getLabel())));
            val.add(p.getNode(param[i + 1]));
        }
        return Mapping.create(variables, val);
    }

    String clean(String name) {
        if (name.startsWith("$")) {
            return QM.concat(name.substring(1));
        }
        return name;
    }

    public IDatatype coalesce(IDatatype... ldt) {
        for (IDatatype dt : ldt) {
            if (dt != null) {
                return dt;
            }
        }
        return null;
    }

    public IDatatype bound(IDatatype dt) {
        return (dt != null) ? DatatypeMap.TRUE : DatatypeMap.FALSE;
    }

    public IDatatype in(IDatatype dt, IDatatype list) {
        for (IDatatype val : list.getValues()) {
            if (dt.equals(val)) {
                return DatatypeMap.TRUE;
            }
        }
        return DatatypeMap.FALSE;
    }

    public IDatatype edge(IDatatype subj, IDatatype pred) {
        return DatatypeMap.createObject(getLoop(getProducer(), subj, pred, null));
    }

    public IDatatype edge(IDatatype subj, IDatatype pred, IDatatype obj) {
        return DatatypeMap.createObject(getLoop(getProducer(), subj, pred, obj));
    }

    Loopable getLoop(final Producer p, final IDatatype subj, final IDatatype pred, final IDatatype obj) {
        return () -> new DataProducer(getGraph(p)).iterate(subj, pred, obj);
    }

    public IDatatype and(IDatatype... ldt) {
        for (IDatatype dt : ldt) {
            if (dt == null) {
                return null;
            }
            if (!dt.booleanValue()) {
                return DatatypeMap.FALSE;
            }
        }
        return DatatypeMap.TRUE;
    }

    public IDatatype or(IDatatype... ldt) {
        for (IDatatype dt : ldt) {
            if (dt == null) {
                return null;
            }
            if (dt.booleanValue()) {
                return DatatypeMap.TRUE;
            }
        }
        return DatatypeMap.FALSE;
    }

    public IDatatype not(IDatatype dt) {
        if (dt == null) {
            return null;
        }
        return dt.booleanValue() ? DatatypeMap.FALSE : DatatypeMap.TRUE;
    }

    public IDatatype concat(IDatatype... ldt) {
        StringBuilder sb = new StringBuilder();
        for (IDatatype dt : ldt) {
            sb.append(dt.getLabel());
        }
        return DatatypeMap.newInstance(sb.toString());
    }

    public IDatatype bnode() {
        return DatatypeMap.createBlank();
    }

    Environment getEnvironment() {
        return environment;
    }

    Producer getProducer() {
        return producer;
    }

    private Graph getGraph(Producer producer) {
        return (Graph) producer.getGraph();
    }

    /**
     * @param environment the environment to set
     */
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * @param producer the producer to set
     */
    @Override
    public void setProducer(Producer producer) {
        this.producer = producer;
    }
    
     /**
     * @return the eval
     */
    public Computer getComputer() {
        return eval;
    }

    /**
     * @param eval the eval to set
     */
    public void setComputer(Computer eval) {
        this.eval = eval;
    }

   
}
