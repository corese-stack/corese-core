package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Query Type QueryDefinitionChecker
 * <p>
 * Check occurrence of properties in Producer
 * Check if class/property are defined in Producer
 * - p rdf:type rdf:Property
 * - c rdf:type rdfs:Class
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 */
final class QueryDefinitionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDefinitionChecker.class);

    private final Eval eval;
    private final Producer producer;
    private final Matcher matcher;
    private Query query;

    QueryDefinitionChecker(Eval e) {
        eval = e;
        producer = e.getProducer();
        matcher = e.getMatcher();
    }

    static QueryDefinitionChecker create(Eval e) {
        return new QueryDefinitionChecker(e);
    }

    void check(Query q) {
        query = q;
        check(null, q.getBody(), eval.getMemory());
    }


    private void check(Node gNode, Exp exp, Environment env) {

        switch (exp.type()) {

            case EDGE:
                edge(gNode, exp, env);
                break;


            case QUERY:
                check(gNode, exp.getQuery().getBody(), env);
                break;

            default:
                for (Exp ee : exp.getExpList()) {
                    check(gNode, ee, env);
                }
        }


    }

    /**
     * Check occurrence of edge
     * If edge has an associated query, check class/property definition in ontology
     */
    private void edge(Node gNode, Exp exp, Environment env) {
        Edge edge = exp.getEdge();
        boolean exist = false;
        boolean match = false;
        boolean define ;

        for (Edge ent : producer.getEdges(gNode, query.getFrom(gNode), edge, env)) {

            if (ent != null) {
                exist = true;
                if (matcher.match(edge, ent, env)) {
                    match = true;
                    break;
                }
            }
        }

        Query q = query.get(edge);
        if (q != null) {
            Eval ee = Eval.create(producer, eval.getEvaluator(), matcher);
            Mappings map = ee.query(q);
            define = !map.isEmpty();
            report(edge, exist, match, define);

        } else {
            report(edge, exist, match);
        }
    }


    private void report(Edge edge, boolean exist, boolean match, boolean define) {
        query.addInfo(edge.toString(),
                " defined:" + define + " exist: " + exist + " match: " + match);
        LOGGER.info("Edge: {}: {} {} {}", edge, exist, match, define);
    }

    private void report(Edge edge, boolean exist, boolean match) {
        query.addInfo(edge.toString(), " exist: " + exist + " match: " + match);
        LOGGER.info("Edge: {}: {} {}", edge, exist, match);
    }


}
