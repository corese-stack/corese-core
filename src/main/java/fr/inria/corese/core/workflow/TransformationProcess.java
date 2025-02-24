package fr.inria.corese.core.workflow;

import fr.inria.corese.core.Event;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.transform.Transformer;

/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 */
public class TransformationProcess extends WorkflowProcess {

    static final String TEMPLATE_RESULT = "?templateResult";
    private boolean isDefault = false;
    private boolean template = false;
    private Transformer transfomer;

    public TransformationProcess(String p) {
        setPath(p);
    }

    public TransformationProcess(String p, boolean b) {
        this(p);
        isDefault = b;
    }

    @Override
    public boolean isTransformation() {
        return true;
    }

    @Override
    void start(Data data) {
        data.getEventManager().start(Event.WorkflowTransformation, getPath());
        // focus this event only
        data.getEventManager().show(Event.WorkflowTransformation);
    }

    @Override
    void finish(Data data) {
        collect(data);
        data.getEventManager().finish(Event.WorkflowTransformation, getPath());
        data.getEventManager().show(Event.WorkflowTransformation, false);
    }

    @Override
    public Data run(Data data) throws EngineException {
        if (data.getMappings() != null && data.getMappings().getQuery().isTemplate()) {
            if (isDefault) {
                // former SPARQLProcess is a template {} where {}
                // this Transformer is default transformer : return former template result
                return data;
            } else if (data.getMappings().getTemplateResult() != null) {
                setTemplate(true);
            }
        }
        Transformer t = Transformer.create(data.getGraph(), getPath());
        setTransfomer(t);
        init(t, data, getContext());
        Data res = new Data(data.getGraph());
        if (isTemplate()) {
            // set result of previous template query into ldscript global variable ?templateResult
            // use case: in Workflow, transformation st:web return ?templateResult as result 
            // when this variable is bound
            data.getBinding().setGlobalVariable(TEMPLATE_RESULT,
                    data.getMappings().getTemplateResult().getDatatypeValue());
        }
        IDatatype dt = t.process(data.getBinding());
        if (dt != null) {
            res.setTemplateResult(dt.getLabel());
            res.setDatatypeValue(dt);
        }
        res.setProcess(this);
        res.setBinding(t.getBinding());
        complete(t, data, res);
        return res;
    }

    @Override
    public String stringValue(Data data) {
        return data.getTemplateResult();
    }

    void init(Transformer t, Data data, Context c) {
        if (c != null) {
            t.setContext(c);
        }
        if (data.getMappings() != null) {
            t.getContext().set(Context.STL_MAPPINGS, DatatypeMap.createObject(data.getMappings()));
        }
    }

    void complete(Transformer t, Data data, Data res) {
    }

    /**
     * @return the transfomer
     */
    public Transformer getTransfomer() {
        return transfomer;
    }

    /**
     * @param transfomer the transfomer to set
     */
    public void setTransfomer(Transformer transfomer) {
        this.transfomer = transfomer;
    }

    @Override
    public String getTransformation() {
        return getPath();
    }

    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(boolean template) {
        this.template = template;
    }

}
