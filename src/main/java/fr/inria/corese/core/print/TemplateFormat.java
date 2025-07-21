package fr.inria.corese.core.print;

import fr.inria.corese.core.transform.Transformer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import fr.inria.corese.core.sparql.api.IDatatype;
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

    public static TemplateFormat create(Mappings m, String pp) {
        return new TemplateFormat(m, pp);
    }

    public static TemplateFormat create(Graph g) {
        return new TemplateFormat(g);
    }

    public static TemplateFormat create(Graph g, String pp) {
        return new TemplateFormat(g, pp);
    }

    public void setNSM(NSManager n) {
        nsm = n;
    }

    public void setPPrinter(String pp) {
        printer = pp;
    }

    public void setTurtle(boolean b) {
        isTurtle = b;
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

    public StringBuilder toStringBuilder() {
        if (graph == null) {
            return new StringBuilder();
        }
        Transformer p = createPP();
        return p.toStringBuilder();
    }

    public Transformer getPPrinter() {
        return pp;
    }

    public void write(String name) throws IOException {
        try (final FileWriter fw = new FileWriter(name);
             final BufferedWriter bufferedWriter = new BufferedWriter(fw)) {
            String str = toString();
            bufferedWriter.write(str);
            bufferedWriter.flush();
        }
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean isCheck) {
        this.isCheck = isCheck;
    }

    public void setStart(String str) {
        start = str;
    }

}
