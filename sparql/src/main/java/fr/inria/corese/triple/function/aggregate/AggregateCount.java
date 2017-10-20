package fr.inria.corese.triple.function.aggregate;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.corese.triple.function.term.Binding;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import fr.inria.edelweiss.kgram.core.Mapping;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class AggregateCount extends Aggregate {

    int num;
    public AggregateCount(){
    }
    
    public AggregateCount(String name) {
        super(name);
        start();
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        if (arity() == 0){          
            return count(env);
        }
        return super.eval(eval, b, env, p);
    }

    
    IDatatype count(Environment env){
        init(env);
        int count = 0;
        for (Mapping map : env.getAggregate()) {
            if (accept(map)){
                count++;
            }
        }
        return value(count);
    }

    
    @Override
    public void aggregate(IDatatype dt) {
      if (accept(dt)) {           
            num++;
        }
    }
    
    @Override
    public void start(){
        num = 0;
    }
    
    @Override
    public IDatatype result() {
        return value(num);
    }
}
