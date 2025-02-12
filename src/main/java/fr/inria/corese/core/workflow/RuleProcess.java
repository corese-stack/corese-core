package fr.inria.corese.core.workflow;

import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.rule.Rule;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class RuleProcess extends  WorkflowProcess {

    private RuleEngine engine;
    RuleEngine.Profile profile = RuleEngine.Profile.STDRL;
    private boolean onUpdate = false;
    
    public RuleProcess(String p){
        path = p;
        if (path.equals(NSManager.OWLRL)){
            profile = RuleEngine.Profile.OWLRL;
        }
        else if (path.equals(NSManager.RDFSRL)){
            profile = RuleEngine.Profile.OWLRL_LITE;
        }
    }
    
    public RuleProcess(RuleEngine.Profile p){
        profile = p;
    }
    
    @Override
    void start(Data data){
    }
    
    @Override
    void finish(Data data){
        collect(data);
    }
    
    @Override
    public Data run(Data data) throws EngineException {
        if (isOnUpdate()) {
            // previous query is update
            // run rule engine on workflow input graph
            Graph g = getWorkflow().getGraph();
            if (g != null && g.getEventManager().isUpdate()) {
                infer(data, g);
            }
            return data;
        }
        // run rule engine on current graph
        return infer(data, data.getGraph());
    }
    
    Data infer(Data data, Graph g) throws EngineException {
        try {
            RuleEngine re = create(g); 
            re.setContext(getContext());
            setEngine(re);
            re.process();
            Data res = new Data(data.getGraph());
            res.setProcess(this);
            return res;
        } catch (LoadException ex) {
            throw new EngineException(ex);
        }
    }
    
    @Override
    public String stringValue(Data data){
        return data.getGraph().toString();
    }
    
    
    RuleEngine create(Graph g) throws LoadException {
        RuleEngine re;
        if (profile == RuleEngine.Profile.STDRL) {
            re = create(g, getPath());
        } else {
            re = RuleEngine.create(g);
            re.setProfile(profile);
        }
        init(re);
        return re;
    }
    
    void init(RuleEngine re){
        if (getContext() != null){
            for (Rule r : re.getRules()){
                r.getQuery().setContext(getContext());
            }
        }
    }
    
    RuleEngine create(Graph g, String p) throws LoadException{
        Load ld = Load.create(g);
        ld.parse(p, Loader.format.RULE_FORMAT);
        return ld.getRuleEngine();
    }

    /**
     * @return the engine
     */
    public RuleEngine getEngine() {
        return engine;
    }

    /**
     * @param engine the engine to set
     */
    public void setEngine(RuleEngine engine) {
        this.engine = engine;
    }

    /**
     * @return the onUpdate
     */
    public boolean isOnUpdate() {
        return onUpdate;
    }

    /**
     * @param onUpdate the onUpdate to set
     */
    public void setOnUpdate(boolean onUpdate) {
        this.onUpdate = onUpdate;
    }

  
    
}
