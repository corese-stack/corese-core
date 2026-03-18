package fr.inria.corese.core.extension;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.NSManager;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent Java object accessible in LDScript with xt:agent() (see function/system.rq)
 * It has a singleton, hence each ag:fun() function call is performed on the same object
 * The singleton can be accessed in LDScript using xt:agent()
 * <p>
 * prefix ag: &lt;function://fr.inria.corese.core.extension.Agent>
 * <p>
 * IDatatype ag:functionName(IDatatype arg)
 * <p>
 * IDatatype java:functionName(xt:agent(), IDatatype arg)
 * IDatatype java:functionName(xt:agent(), JavaType arg)
 * .
 */
public class Agent {

    private static final Logger logger = LoggerFactory.getLogger(Agent.class);

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

    IDatatype test() {
        return DatatypeMap.TRUE;
    }

    void entailment() {
        if (getGraph() != null) {
            RuleEngine re = RuleEngine.create(graph);
            try {
                re.setProfile(RuleEngine.Profile.OWLRL);
                re.process();
            } catch (EngineException | LoadException ex) {
                logger.error("An unexpected error has occurred", ex);
            }
        }
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }


}
