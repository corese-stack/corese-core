/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.corese.core.workflow;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class AssertProcess extends SemanticProcess {
    
    WorkflowProcess test;
    IDatatype value;
    
    AssertProcess(WorkflowProcess w, IDatatype dt){
        insert(w);
        test = w;
        value = dt;
    }
    
    @Override
    public Data run(Data data) throws EngineException{
        Data val = test.compute(data);
        IDatatype dt = val.getDatatypeValue();
        Data res = new Data(this, data.getMappings(), data.getGraph());
        res.setDatatypeValue(dt);
        if (val.getMappings() != null){
            res.setSuccess(val.getMappings().size() > 0);
            res.addData(val);
        }
        else if (dt == null || ! dt.equals(value)){
            res.setSuccess(false);
        }
        return res;
    }
    
    @Override
    void start(Data data){
          
     }
    
    @Override
    void finish(Data data){
    }
    
    @Override
    public String  stringValue(Data data){
        return Boolean.valueOf(data.isSuccess()).toString();        
    }

}
