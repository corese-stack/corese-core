package fr.inria.corese.core.extension.core;

import fr.inria.corese.core.compiler.eval.Interpreter;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.function.core.FunctionEvaluator;
import fr.inria.corese.core.compiler.parser.NodeImpl;
import fr.inria.corese.core.kgram.api.core.Loopable;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.producer.DataProducer;
import fr.inria.corese.core.query.PluginTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public PluginTransform getPluginTransform() {
        return (PluginTransform) ((Interpreter)eval).getComputerTransform();
    }

    String javaName(IDatatype dt) {
        return NSManager.nstrip(dt.getLabel());
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

    Loopable getLoop(final Producer p, final IDatatype subj, final IDatatype pred, final IDatatype obj) {
        return () -> new DataProducer(getGraph(p)).iterate(subj, pred, obj);
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
    
   
}
