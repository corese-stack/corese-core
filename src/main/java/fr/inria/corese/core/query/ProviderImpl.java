package fr.inria.corese.core.query;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Provider;
import fr.inria.corese.core.kgram.core.*;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.exceptions.SafetyException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Access;
import fr.inria.corese.core.sparql.triple.parser.Access.Feature;
import fr.inria.corese.core.sparql.triple.parser.URLParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Implements service expression There may be local QueryProcess for some URI
 * (use case: W3C test case) Send query to sparql endpoint using HTTP POST query
 * There may be a default QueryProcess
 * <p>
 * TODO: check use same ProducerImpl to generate Nodes ?
 *
 * @author Olivier Corby, Edelweiss INRIA 2011
 */
public class ProviderImpl implements Provider, URLParam {

    static Logger logger = LoggerFactory.getLogger(ProviderImpl.class);
    HashMap<String, QueryProcess> table;
    private QueryProcess defaut;

    private ProviderImpl() {
        table = new HashMap<>();
    }

    public static ProviderImpl create() {
        return new ProviderImpl();
    }

    public static ProviderImpl create(QueryProcess exec) {
        ProviderImpl pi = ProviderImpl.create();
        pi.setDefault(exec);
        return pi;
    }

    /**
     * Define a QueryProcess for this URI
     */
    public void add(String uri, Graph g) {
        QueryProcess exec = QueryProcess.create(g);
        exec.set(this);
        table.put(uri, exec);
    }

    /**
     * Define a default QueryProcess
     */
    public void add(Graph g) {
        QueryProcess exec = QueryProcess.create(g);
        exec.set(this);
        setDefault(exec);
    }

    /**
     * If there is a QueryProcess for this URI, use it Otherwise send query to
     * spaql endpoint If endpoint fails, use default QueryProcess if it exists
     * When service URL is a constant or a bound variable, serv = URL
     * otherwise serv = NULL
     */
    @Override
    public Mappings service(Node serv, Exp exp, Mappings lmap, Eval eval)
            throws EngineException {
        Binding b = getBinding(eval.getEnvironment());
        if (Access.reject(Feature.SPARQL_SERVICE, b.getAccessLevel())) {
            throw new SafetyException(TermEval.SERVICE_MESS);
        }
        return serviceBasic(serv, exp, lmap, eval);
    }

    Binding getBinding(Environment env) {
        return env.getBind();
    }

    /**
     * exp: service statement
     */
    public Mappings serviceBasic(Node serv, Exp exp, Mappings lmap, Eval eval)
            throws EngineException {
        Exp body = exp.rest();
        // select query inside service statement
        Query q = body.getQuery();

        QueryProcess exec = null;

        if (serv != null) {
            exec = table.get(serv.getLabel());
        }

        if (exec == null) {

            ProviderService ps = new ProviderService(this, q, lmap, eval);
            ps.setDefault(getDefault());
            Mappings map = ps.send(serv, exp);

            if (map == null) {
                map = Mappings.create(q);
                if (q.isSilent()) {
                    map.add(Mapping.create());
                }
            }

            return map;
        }

        ASTQuery ast = exec.getAST(q);
        Mappings map;
        try {
            map = exec.query(ast);
            return map;
        } catch (EngineException ex) {
            logger.error(ex.getMessage());
        }
        return new Mappings();
    }

    public QueryProcess getDefault() {
        return defaut;
    }

    public void setDefault(QueryProcess defaut) {
        this.defaut = defaut;
    }
}
