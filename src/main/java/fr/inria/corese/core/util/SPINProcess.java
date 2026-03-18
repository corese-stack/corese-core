package fr.inria.corese.core.util;

import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.printer.SPIN;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

/**
 *
 * Compile SPARQL Query in SPIN format
 *
 * @author Olivier Corby, Wimmics Inria I3S, 2013
 *
 * @deprecated
 * This class should be removed as it requires SPIN which should also be removed.
 * The remaining uses of this class should be removed when re-structure of SPARQL is done
 */
@Deprecated
public class SPINProcess {

    Graph graph;
    private NSManager nsm;
    QueryProcess exec;
    private boolean isSPARQLCompliant = false;
    // The environment may have a default base
    // use case: W3C test case query have the query location path as default base
    private String defaultBase;

    @Deprecated
   public static SPINProcess create() {
        return new SPINProcess();
    }

    @Deprecated
    SPINProcess() {
        exec = QueryProcess.create(Graph.create());
    }

    @Deprecated
    public String toSpin(String sparql) throws EngineException {
        return toSpin(sparql, true);
    }

    @Deprecated
    public String toSpin(String sparql, boolean nsm) throws EngineException {
        Query qq = exec.compile(sparql);
        ASTQuery ast = exec.getAST(qq);
        setNSM(ast.getNSM());
        SPIN sp = SPIN.create();
        sp.visit(ast);
        return sp.toString();
    }

    @Deprecated
    public Graph toSpinGraph(String sparql) throws EngineException {
        return toGraph(toSpin(sparql));
    }

    @Deprecated
    public String toSpin(ASTQuery ast, String src) throws EngineException {
        SPIN sp = SPIN.create();
        sp.visit(ast, src);
        return sp.toString();
    }

    @Deprecated
    public Graph toSpinGraph(ASTQuery ast) throws EngineException {
        return toGraph(toSpin(ast, null), Graph.create());
    }

    @Deprecated
    public Graph toGraph(String spin) {
        graph = Graph.create();
        return toGraph(spin, graph);
    }

    @Deprecated
    public Graph toGraph(String spin, Graph g) {
        Load ld = Load.create(g);
        try {
            ld.parse(new ByteArrayInputStream(spin.getBytes(StandardCharsets.UTF_8)), Loader.format.TURTLE_FORMAT);
        } catch (LoadException ex) {
            LoggerFactory.getLogger(SPINProcess.class.getName()).error("", ex);
        }
        return g;
    }

    /**
     * @param nsm the nsm to set
     */
    @Deprecated
    public void setNSM(NSManager nsm) {
        this.nsm = nsm;
    }
}
