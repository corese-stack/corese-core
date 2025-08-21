/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.corese.core.transform;

import fr.inria.corese.core.transform.Transformer;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import java.util.HashMap;

/**
 * Map pprinter name to pprinter resource path
 *
 * @author Olivier Corby, Wimmics Inria I3S, 2013
 *
 */
public class Table extends HashMap<String, String> {

    HashMap<String, Boolean> table;

    Table() {
        init();
    }

    void init() {
        // namespace to pprinter
        put(NSManager.OWL,  TransformerUtils.OWL);
        put(NSManager.SPIN, TransformerUtils.SPIN);
        put(NSManager.SQL,  TransformerUtils.SQL);

        table = new HashMap<String, Boolean>();
        // pprinter is optimized ?
        table.put(TransformerUtils.OWL, false);
        table.put(TransformerUtils.SPIN, true);
        table.put(TransformerUtils.SQL, true);

    }
    
    boolean isOptimize(String name){
        Boolean b = table.get(name);
        if (b != null){
            return b;
        }
        return false;
    }
    
    void setOptimize(String name, boolean b){
        table.put(name, b);
    }
    
}
