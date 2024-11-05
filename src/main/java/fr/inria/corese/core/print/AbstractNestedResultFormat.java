package fr.inria.corese.core.print;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mother class for the query result formats such as XML and JSON
 */
public abstract class AbstractNestedResultFormat extends QueryResultFormat {

    public interface Title {
    }
    public enum AbstractTitle implements Title{
        OHEADER, CHEADER, OHEAD, CHEAD, OVAR, CVAR, ORESULT, CRESULT, ORESULTS, CRESULTS
    }

    static final String VAR1 = "?";
    static final String VAR2 = "$";

    private long nbResult = Long.MAX_VALUE;
    private Mappings lMap;
    NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
    ASTQuery ast;
    Query query;
    PrintWriter pw;
    ArrayList<String> select;
    private boolean selectAll = false;

    AbstractNestedResultFormat(Mappings lm) {
        super();
        lMap = lm;
        nf.setMaximumFractionDigits(4);
    }

    public abstract void printAsk();
    abstract void display(String var, IDatatype dt);
    abstract void display(IDatatype dt);
    abstract void newResult();
    abstract void error();
    abstract String protect(String mes);

    /**
     * perform init() just before printing because we need to wait
     * a possible setSelectAll(true)
     * So we cannot do it at creation time
     */
    void init() {
        setSelect();
    }

    public Mappings getMappings() {
        return lMap;
    }

    public void setMappings(Mappings lMap) {
        this.lMap = lMap;
    }

    /**
     * @return the nbResult
     */
    public long getNbResult() {
        return nbResult;
    }

    /**
     * @param nbResult the nbResult to set
     */
    public void setNbResult(long nbResult) {
        this.nbResult = nbResult;
    }

    ArrayList<String> getSelect() {
        return select;
    }

    public abstract <T extends Title> String getTitle(T t);

    ASTQuery getAST() {
        return ast;
    }

    /**
     * @param selectAll the selectAll to set
     */
    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    void setQuery(Query q, Mappings map) {
        query = q;
    }

    /*
     * The main printer
     */
    /**
     * Print the vector of CG results, grouped and sorted and counted, as an
     * HTML table Print it in the print writer, prints the concepts using cg2rdf
     * pprinter The table is/can be processed by a stylesheet
     */
    public void print() {
        print(false, "");
    }

    public void print(boolean printInfoInFile, String fileName) {
        init();
        println(getTitle(XMLFormat.XMLTitle.XMLDEC));
        if (getMappings().size() > getNbResult()) {
            println(String.format("<!-- Display %s results out of %s -->", getNbResult(), getMappings().size()));
        }
        println(getTitle(AbstractTitle.OHEADER));
        error();
        detail();

        printHead();

        if (isQTAsk()) {
            printAsk();
        } else {
            println(getTitle(AbstractTitle.ORESULTS));
            if (getMappings() != null) {
                long n = 1;
                for (Mapping map : getMappings()) {
                    if (n > getNbResult()) {
                        break;
                    }
                    print(map, n++);
                }
            }
            println(getTitle(AbstractTitle.CRESULTS));
        }

        println(getTitle(AbstractTitle.CHEADER));
    }

    /**
     * Print one value using the SPARQL XML markup
     */
    void print(String var, Node c) {
        if (c == null) {
            // do nothing
            return;
        }
        display(var,  c.getDatatypeValue());
    }

    /**
     * Print a cg result as a line of the table each column is one of the select
     * variables of the query
     */
    void print(Mapping map, long n) {
        newResult();
        println(getTitle(AbstractTitle.ORESULT));
        for (String var : getSelect()) {
            // for each select variable, get its binding and print it
            if (map.getMappings() != null) {
                List<Node> list = map.getNodes(var, true);
                for (Node node : list) {
                    print(var, node);
                }
            } else {
                Node value = map.getNode(var);
                print(var, value);
            }
        }
        println(getTitle(AbstractTitle.CRESULT));
    }

    protected void print(String str) {
        pw.print(str);
    }

    protected void printf(String format, Object... str) {
        pw.print(String.format(format, str));
    }

    protected void println(String str) {
        pw.println(str);
    }

    protected void println() {
        pw.println();
    }

    protected void println(Object obj) {
        pw.println(obj.toString());
    }

    public void printHead() {
        println(getTitle(AbstractTitle.OHEAD));
        // print variable or functions selected in the header
        printVariables(getSelect());
        printLink(getMappings().getLinkList());
        println(getTitle(AbstractTitle.CHEAD));
    }

    void printVariables(ArrayList<String> select) {
        for (String var : select) {
            printVar(var);
        }
    }

    //	variable in header (<th>)
    private void printVar(String var) {
        println(getTitle(AbstractTitle.OVAR) + getName(var) + getTitle(AbstractTitle.CVAR));
    }

    void printLink(List<String> list) {
        for (String name : list) {
            print("<link href=\"" + name + "\"/>\n");
        }
    }

    protected String getName(String var) {
        if (var.indexOf(VAR1) == 0) {
            return var.substring(1);
        } else {
            return var;
        }
    }

    void detail() {
        if (getMappings().getInsert() != null) {
            for (Edge ent : getMappings().getInsert()) {
                println(ent);
            }
        }

        if (getMappings().getDelete() != null) {
            for (Edge ent : getMappings().getDelete()) {
                println(ent);
            }
        }
    }

    boolean isQTAsk() {
        return ast.isAsk();
    }

    void setSelect() {
        select = new ArrayList<>();

        for (Node node : query.getSelect()) {
            defSelect(node.getLabel());
        }

        if (isSelectAll()) {
            // additional select nodes such as ?_server_0 in federate mode
            for (Node node : getMappings().getQueryNodeList()) {
                defSelect(node.getLabel());
            }
        }
    }

    /**
     * @return the selectAll
     */
    public boolean isSelectAll() {
        return selectAll;
    }

    void defSelect(String var) {
        if (! select.contains(var) && accept(var)) {
            select.add(var);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = toStringBuffer();
        return sb.toString();
    }

    public StringBuffer toStringBuffer() {
        StringWriter sw = new StringWriter();
        pw = new PrintWriter(sw);
        print();
        return sw.getBuffer();
    }

}
