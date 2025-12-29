package fr.inria.corese.core.storage;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.storage.api.dataManager.DataManagerBuilder;

public class CoreseGraphDataManagerBuilder implements DataManagerBuilder {


    private Graph graph;
    private boolean defGraph = false;


    /**
     * Create a CoreseGraphDataManagerBuilder.
     */
    public CoreseGraphDataManagerBuilder() {
    }

    /**
     * Build the dataManager from an existing Corese Graphn
     * 
     * @param graph Corese Graph.
     * @return this instance.
     */
    public CoreseGraphDataManagerBuilder graph(Graph graph) {
        this.graph = graph;
        this.defGraph = true;
        return this;
    }


    @Override
    public CoreseGraphDataManager build() {
        if (defGraph) {
            return new CoreseGraphDataManager(this.graph);
        }

        return new CoreseGraphDataManager();
    }
}
