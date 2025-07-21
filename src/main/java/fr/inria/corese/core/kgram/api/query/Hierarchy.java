package fr.inria.corese.core.kgram.api.query;

import fr.inria.corese.core.sparql.api.IDatatype;
import java.util.List;

/**
 *
 * @author corby
 */
public interface Hierarchy {
        
    List<String> getSuperTypes(IDatatype object, IDatatype type);
    
    void defSuperType(IDatatype type, IDatatype sup);
        
}
