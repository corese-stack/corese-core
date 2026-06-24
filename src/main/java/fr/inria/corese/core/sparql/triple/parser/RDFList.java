package fr.inria.corese.core.sparql.triple.parser;

import java.util.List;

/**
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 *
 */
public class RDFList extends And {

    Atom first;
    List<Atom> list;

    
    RDFList(){       
    }
    
    RDFList(Atom f, List<Atom> l){
        first = f;
        list = l;
    }
    

    @Override
    public boolean isRDFList() {
        return true;
    }

    public Atom head() {
        return first;
    }


    public List<Atom> getList() {
        return list;
    }

   
}
