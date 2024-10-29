package fr.inria.corese.core.elasticsearch;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.core.*;
import fr.inria.corese.core.sparql.api.IDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AbstractElasticsearchVisitor extends ProcessVisitorDefault {
    private static Logger logger = LoggerFactory.getLogger(AbstractElasticsearchVisitor.class);

    protected static String ELASTICSEARCH_API_KEY_STRING = "";

    public static String getElasticsearchApiKey() {
        return ELASTICSEARCH_API_KEY_STRING;
    }

    public static void setElasticsearchApiKey(String key) {
        ELASTICSEARCH_API_KEY_STRING = key;
    }

    @Override
    public IDatatype insert(IDatatype path, Edge edge) {
        logger.debug("insert {} {} {}", edge.getSubjectNode().getLabel(), edge.getPropertyNode().getLabel(), edge.getObjectNode().getLabel());
        return defaultValue();
    }

    @Override
    public IDatatype delete(Edge edge) {
        logger.debug("delete {} {} {}", edge.getSubjectNode().getLabel(), edge.getPropertyNode().getLabel(), edge.getObjectNode().getLabel());
        return defaultValue();
    }

    @Override
    public IDatatype update(Query q, List<Edge> delete, List<Edge> insert) {
        logger.debug("Update delete:{} triples, insert:{} triples" , delete.size(), insert.size());
        return defaultValue();
    }
}
