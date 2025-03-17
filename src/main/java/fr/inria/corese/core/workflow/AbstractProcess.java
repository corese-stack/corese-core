package fr.inria.corese.core.workflow;

import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.Dataset;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public interface AbstractProcess {
    
    String PREF = NSManager.SWL;
    String GRAPH = PREF + "graph";
    String PROBE = PREF + "probe";
            
   
    void subscribe(SemanticWorkflow w);
    
    String stringValue(Data data);
        
    void setContext(Context c);
    
    void inherit(Context c);

    void setDataset(Dataset ds);

    void inherit(Dataset ds);
    
    boolean isTransformation();
    
    void setProbe(boolean b);
    
    void setResult(String r);
    
    void setURI(String uri);
    
    void setName(String name);
}
