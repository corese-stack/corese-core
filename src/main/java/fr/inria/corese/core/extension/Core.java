package fr.inria.corese.core.extension;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.query.PluginImpl;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.core.FunctionEvaluator;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.transform.Transformer;

import java.util.Arrays;
import java.util.HashMap;

import fr.inria.corese.core.transform.TransformerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super class for Java extension function class
 * public class Myclass extends Core
 * Extension function called with prefix:
 * function://fr.inria.corese.core.extension.Myclass
 * See sparql.triple.function.core.Extern
 * <p>
 * Use case: JavaCompiler compiles SHACL Interpreter
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017-2019
 */
public class Core extends PluginImpl implements FunctionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(Core.class);

    private static final String MSH = "http://ns.inria.fr/shacl/";
    static Class<IDatatype>[][] signature;
    static HashMap<String, String> prefix;

    static {
        init();
    }

    HashMap<String, String> functionName;

    static void init() {
        defSignature();
        defNamespace();
    }

    static HashMap<String, String> getPrefix() {
        return prefix;
    }

    static void define(String pref, String ns) {
        prefix.put(pref, ns);
    }

    /**
     * Define prefix namespace for funcall(sh:fun)
     * because functions are defined as sh_fun()
     */
    static void defNamespace() {
        prefix = new HashMap<>();
        define("sh", NSManager.SHAPE);
        define("msh", MSH);
    }

    static void defSignature() {
        signature = new Class[20][];
        for (int i = 0; i < signature.length; i++) {
            Class[] sig = new Class[i];
            Arrays.fill(sig, IDatatype.class);
            signature[i] = sig;
        }
    }

    void defFunction() {
        functionName.put(NSManager.EXT + "member", "member");
    }

    @Override
    public void setProducer(Producer producer) {
        super.setProducer(producer);
    }

    @Override
    public void setEnvironment(Environment env) {
        super.setEnvironment(env);
    }


    Graph getGraph(IDatatype dt) {
        return (Graph) dt.getPointerObject();
    }

    IDatatype xt_turtle(IDatatype x) {
        if (x.isLiteral() && x.getDatatypeURI().equals(IDatatype.GRAPH_DATATYPE)) {
            try {
                Transformer t = Transformer.create(getGraph(x), TransformerUtils.TURTLE);
                return t.process();
            } catch (EngineException ex) {
                logger.error("An unexpected error has occurred", ex);
            }
        } else {
            Transformer t = Transformer.create(getGraph(), TransformerUtils.TURTLE);
            try {
                return t.process(x);
            } catch (EngineException ex) {
                logger.error("An unexpected error has occurred", ex);
            }
        }
        return x;
    }


}
