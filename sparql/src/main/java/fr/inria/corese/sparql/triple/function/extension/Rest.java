/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.corese.sparql.triple.function.extension;

import fr.inria.corese.kgram.api.core.Pointerable;
import fr.inria.corese.kgram.api.query.Environment;
import fr.inria.corese.kgram.api.query.Producer;
import fr.inria.corese.sparql.api.Computer;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.datatype.DatatypeMap;
import fr.inria.corese.sparql.triple.function.term.Binding;
import fr.inria.corese.sparql.triple.function.term.TermEval;
import java.util.ArrayList;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class Rest extends TermEval {
      
    public Rest(){}
    
    public Rest(String name){
        super(name);
        setArity(2);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype dt    = getBasicArg(0).eval(eval, b, env, p);
        IDatatype index   = getBasicArg(1).eval(eval, b, env, p);
        
        if (dt == null || index == null) {
            return null;
        }
        
        if (dt.isList()) {
            return DatatypeMap.rest(dt, index);
        }
        else if (dt.isPointer() && dt.isLoop()) {
            return rest(dt, index);
        }
        return null;
    }
    
    IDatatype rest(IDatatype dt, IDatatype index) {       
        Pointerable res = dt.getPointerObject(); 
        ArrayList<IDatatype> list = new ArrayList<>();
        int i = 0;
        for (Object obj : res.getLoop()) {          
            if (i++ >= index.intValue() && obj != null) {
                list.add(DatatypeMap.getValue(obj));
            }
        }
        return DatatypeMap.newInstance(list);
    }

}