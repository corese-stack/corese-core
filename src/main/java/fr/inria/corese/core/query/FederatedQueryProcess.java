package fr.inria.corese.core.query;

import fr.inria.corese.core.sparql.triple.parser.Metadata;


/**
 * Specific class for Federated Query Processing
 * Define a federation
 * Execute query on federation
 */
public class FederatedQueryProcess {

    private QueryProcess queryProcess;
    private Metadata metadata;


    public QueryProcess getQueryProcess() {
        return queryProcess;
    }

    public void setQueryProcess(QueryProcess queryProcess) {
        this.queryProcess = queryProcess;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}
