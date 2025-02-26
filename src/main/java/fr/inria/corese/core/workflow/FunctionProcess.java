/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.corese.core.workflow;

import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.Dataset;

/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 */
public class FunctionProcess extends WorkflowProcess {

    private String query;

    FunctionProcess(String q, String p) {
        query = q;
        path = p;
    }

    @Override
    void start(Data data) {
    }

    @Override
    void finish(Data data) {

    }


    @Override
    public Data run(Data data) throws EngineException {
        IDatatype dt = eval(data, getContext(), getDataset());
        Data res = new Data(data.getGraph(), dt);
        res.setProcess(this);
        //res.setVisitor(data.getVisitor());
        res.setBinding(data.getBinding());
        return res;
    }

    IDatatype eval(Data data, Context c, Dataset ds) {
        QueryProcess exec = QueryProcess.create(data.getGraph(), data.getDataManager());
        if (path != null) {
            exec.setDefaultBase(path);
        }
        return DatatypeMap.TRUE;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public String stringValue(Data data) {
        if (data.getDatatypeValue() != null) {
            return data.getDatatypeValue().stringValue();
        }
        return null;
    }

}
