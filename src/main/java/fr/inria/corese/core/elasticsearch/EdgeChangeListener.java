package fr.inria.corese.core.elasticsearch;


import fr.inria.corese.core.kgram.api.core.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class EdgeChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(EdgeChangeListener.class);

    public void onEdgeInsert(Edge edge) {
        onBulkEdgeChange(List.of(), List.of(edge));
    }

    public void onEdgeDelete(Edge edge) {
        onBulkEdgeChange(List.of(edge), List.of());
    }

    public abstract void onBulkEdgeChange(List<Edge> delete, List<Edge> insert);
}
