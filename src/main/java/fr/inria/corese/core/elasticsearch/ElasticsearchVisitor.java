package fr.inria.corese.core.elasticsearch;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.core.*;
import fr.inria.corese.core.sparql.api.IDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ElasticsearchVisitor extends ProcessVisitorDefault {
    private static Logger logger = LoggerFactory.getLogger(ElasticsearchVisitor.class);

    protected static String ELASTICSEARCH_API_KEY_STRING = "";

    public static String getElasticsearchApiKey() {
        return ELASTICSEARCH_API_KEY_STRING;
    }

    public static void setElasticsearchApiKey(String key) {
        ELASTICSEARCH_API_KEY_STRING = key;
    }

    private List<EdgeChangeListener> listeners = new ArrayList<>();

    public void addEdgeChangeListener(EdgeChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public IDatatype insert(IDatatype path, Edge edge) {
        super.insert(path, edge);
        logger.debug("insert {} {} {}", edge.getSubjectNode().getLabel(), edge.getPropertyNode().getLabel(), edge.getObjectNode().getLabel());
        for (EdgeChangeListener listener : listeners) {
            listener.onEdgeInsert(edge);
        }
        return defaultValue();
    }

    @Override
    public IDatatype delete(Edge edge) {
        super.delete(edge);
        logger.debug("delete {} {} {}", edge.getSubjectNode().getLabel(), edge.getPropertyNode().getLabel(), edge.getObjectNode().getLabel());
        for (EdgeChangeListener listener : listeners) {
            listener.onEdgeDelete(edge);
        }
        return defaultValue();
    }

    @Override
    public IDatatype update(Query q, List<Edge> delete, List<Edge> insert) {
        super.update(q, delete, insert);
        if(delete.isEmpty() && insert.isEmpty()) {
            return defaultValue();
        }
        for (EdgeChangeListener listener : listeners) {
            listener.onBulkEdgeChange(delete, insert);
        }
        return defaultValue();
    }
}
