package fr.inria.corese.core.print;

import fr.inria.corese.core.transform.Transformer;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.compiler.parser.Pragma;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.Graph;

/*
 * pragma with template ...
 * Olivier Corby, Wimmics INRIA 2013
 */
public class TemplateFormat {

    String printer;
    Mappings map;
    Query query;
    Graph graph;
    private NSManager nsm;
    Transformer pp;
    boolean isTurtle = false;
    private boolean isCheck = false;
    private String start;

    TemplateFormat(Mappings m) {
        map = m;
        graph = (Graph) map.getGraph();
        query = map.getQuery();
        if (query != null) {
            if (query.hasPragma(Pragma.TEMPLATE)) {
                printer = (String) query.getPragma(Pragma.TEMPLATE);
            }
            ASTQuery ast =  query.getAST();
            setNSM(ast.getNSM());
        }
    }

    TemplateFormat(Mappings m, String p) {
        this(m);
        printer = p;
    }

    TemplateFormat(Graph g) {
        graph = g;
    }

    TemplateFormat(Graph g, String p) {
        this(g);
        printer = p;
    }

    public static TemplateFormat create(Mappings m) {
        return new TemplateFormat(m);
    }

    public void setNSM(NSManager n) {
        nsm = n;
    }

    @Override
    public String toString() {
        if (query != null && query.isTemplate()) {
            Node node = map.getTemplateResult();
            if (node != null) {
                return node.getLabel();
            }
            return "";
        }
        if (graph == null) {
            return "";
        }
        Transformer p = createPP();
        return p.toString();
    }

    Transformer createPP() {
        pp = Transformer.create(graph, printer);
        if (isCheck) {
            pp.check();
        }
        pp.setTurtle(isTurtle);

        if (nsm != null) {
            pp.setNSM(nsm);
        }
        if (start != null) {
            pp.setStart(start);
        }
        return pp;
    }

}
