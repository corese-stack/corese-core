package fr.inria.corese.core.sparql.triple.function.aggregate;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import java.util.ArrayList;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class AggregateList extends Aggregate {

    ArrayList<IDatatype> list;

    public AggregateList() {       
    }
    
    public AggregateList(String name) {
        super(name);
        start();
    }

    @Override
    public void aggregate(IDatatype dt) {
        if (accept(dt)) {
            list.add(dt);
        }
    }
    
    @Override
    public void start(){
        list = new ArrayList<>();
    }
    
    @Override
    public IDatatype result() {
        return DatatypeMap.createList(list);
    }
}
