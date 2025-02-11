package fr.inria.corese.core.extension;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent Java object accessible in LDScript with xt:agent() (see function/system.rq)
 * It has a singleton, hence each ag:fun() function call is performed on the same object
 * The singleton can be accessed in LDScript using xt:agent()
 * <p>
 * prefix ag: <function://fr.inria.corese.core.extension.Agent>
 * <p>
 * IDatatype ag:functionName(IDatatype arg)
 * <p>
 * IDatatype java:functionName(xt:agent(), IDatatype arg)
 * IDatatype java:functionName(xt:agent(), JavaType arg)
 * .
 */
public class Agent {

    static final String NS = NSManager.USER;
    static final String ENTAILMENT = NS + "entailment";
    static final String TEST = NS + "test";

    private static final Agent singleton;
    private static final IDatatype dt;

    static {
        singleton = new Agent("main");
        dt = DatatypeMap.createObject(singleton());
    }

    private String name;
    private Graph graph;
    private IDatatype value;
    private IDatatype uri;

    public Agent() {
        this("proxy");
    }

    public Agent(String n) {
        setName(n);
    }

    /**
     * Function singleton() enables ag:fun() SPARQL Extension Function (Extern)
     * to be called on the same singleton agent
     * otherwise an agent object would be created for each function call.
     */
    public static Agent singleton() {
        return singleton;
    }

    public static IDatatype getDatatypeValue() {
        return dt;
    }

    public IDatatype setURI(IDatatype dt) {
        uri = dt;
        return dt;
    }

    public IDatatype getURI() {
        return uri;
    }

    public IDatatype message(IDatatype name) {
        String label = name.getLabel();
        if (ENTAILMENT.equals(label)) {
            entailment();
        } else if (TEST.equals(label)) {
            test();
        }
        return name;
    }

    public IDatatype message(IDatatype name, IDatatype dt) {
        if (ENTAILMENT.equals(name.getLabel())) {
            entailment();
        }
        return name;
    }

    public IDatatype message(IDatatype name, IDatatype... args) {
        if (ENTAILMENT.equals(name.getLabel())) {
            entailment();
        }
        return name;
    }

    IDatatype test() {
        return DatatypeMap.TRUE;
    }

    void entailment() {
        if (getGraph() != null) {
            RuleEngine re = RuleEngine.create(graph);
            re.setProfile(RuleEngine.OWL_RL);
            try {
                re.process();
            } catch (EngineException ex) {
                Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public IDatatype getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public IDatatype setValue(IDatatype value) {
        this.value = value;
        return value;
    }

    /**
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph g) {
        graph = g;
    }


}
