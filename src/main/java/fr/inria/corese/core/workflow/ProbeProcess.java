package fr.inria.corese.core.workflow;

import fr.inria.corese.core.sparql.exceptions.EngineException;

/**
 * Execute a sub Process but return the input Data
 * Used for e.g trace purpose
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 */
public class ProbeProcess extends SemanticProcess {

    ProbeProcess() {
    }

    ProbeProcess(WorkflowProcess wp) {
        insert(wp);
    }

    @Override
    void start(Data data) {
    }

    @Override
    void finish(Data data) {
    }

    @Override
    public Data run(Data data) throws EngineException {
        for (WorkflowProcess wp : getProcessList()) {
            wp.compute(data);
        }
        return data;
    }


}
