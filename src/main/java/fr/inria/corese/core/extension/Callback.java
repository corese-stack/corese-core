package fr.inria.corese.core.extension;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 *
 * @author corby
 */
public class Callback extends Core {
    
    public IDatatype report(IDatatype name, IDatatype node) {
        System.out.println("report: " + name + " " + node);
        return DatatypeMap.TRUE;
    }
    
    
    
}
