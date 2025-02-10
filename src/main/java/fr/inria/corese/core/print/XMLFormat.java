package fr.inria.corese.core.print;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

/**
 * SPARQL XML Result Format for KGRAM Mappings
 *
 * Olivier Corby, Edelweiss INRIA 2011
 *
 */
public class XMLFormat extends AbstractNestedResultFormat {

    /**
     * Use to keep the class version, to be consistent with the interface
     * Serializable.java
     */
    private static final long serialVersionUID = 1L;

    public static final String SPARQLRES = NSManager.XMLRESULT;
    private static final String XMLDEC = "<?xml version=\"1.0\" ?>";
    private static final String OHEADER = "<sparql xmlns='" + SPARQLRES + "'>";
    private static final String CHEADER = "</sparql>";
    private static final String OHEAD = "<head>";
    private static final String CHEAD = "</head>";
    private static final String OVAR = "<variable name='";
    private static final String CVAR = "'/>";
    private static final String ORESULTS = "<results>";
    private static final String CRESULTS = "</results>";
    private static final String ORESULT = "<result>";
    private static final String CRESULT = "</result>";
    private static final String OBOOLEAN = "<boolean>";
    private static final String CBOOLEAN = "</boolean>";
    private static final String[] XML = {"&", "<"};
    private static final String ODATA = "<![CDATA[";
    private static final String CDATA = "]]>";
    private static final String OCOM = "<!--";
    private static final String CCOM = "-->";

    boolean displaySort = false;

    XMLFormat(Mappings lm) {
        super(lm);
    }

    public static XMLFormat create(Mappings lm) {
        Query q = lm.getQuery();
        return XMLFormat.create(q,  q.getAST(), lm);
    }

    public static XMLFormat create(Query q, ASTQuery ast, Mappings lm) {
        XMLFormat res = new XMLFormat(lm);
        res.setQuery(q, lm);
        res.setAST(ast);
        return res;
    }

    public void setAST(ASTQuery q) {
        ast = q;
    }


    void setWriter(PrintWriter p) {
        pw = p;
    }

    enum XMLTitle implements Title {
        XMLDEC, OCOM, CCOM
    }

    public <T extends AbstractNestedResultFormat.Title> String getTitle(T t) {
        if (XMLTitle.XMLDEC.equals(t)) {
            return XMLDEC;
        } else if (AbstractTitle.OHEADER.equals(t)) {
            return OHEADER;
        } else if (AbstractTitle.CHEADER.equals(t)) {
            return CHEADER;
        } else if (AbstractTitle.OHEAD.equals(t)) {
            return OHEAD;
        } else if (AbstractTitle.CHEAD.equals(t)) {
            return CHEAD;
        } else if (AbstractTitle.OVAR.equals(t)) {
            return OVAR;
        } else if (AbstractTitle.CVAR.equals(t)) {
            return CVAR;
        } else if (AbstractTitle.ORESULT.equals(t)) {
            return ORESULT;
        } else if (AbstractTitle.CRESULT.equals(t)) {
            return CRESULT;
        } else if (AbstractTitle.ORESULTS.equals(t)) {
            return ORESULTS;
        } else if (AbstractTitle.CRESULTS.equals(t)) {
            return CRESULTS;
        } else if(XMLTitle.OCOM.equals(t)) {
            return OCOM;
        } else if(XMLTitle.CCOM.equals(t)) {
            return CCOM;
        }
        return "";
    }

    boolean isMore() {
        return ast.isMore();
    }

    void error() {
        boolean b1 = ast != null && ast.getErrors() != null;
        boolean b2 = query != null && query.getErrors() != null;
        boolean b3 = query != null && query.getInfo() != null;

        if (b1 || b2 || b3) {

            println(OCOM);
            if (ast.getText() != null) {
                println(protect(ast.getText()));
            }
            println("");

            if (b1) {
                for (String mes : ast.getErrors()) {
                    println(protect(mes));
                }
            }
            if (b2) {
                for (String mes : query.getErrors()) {
                    println(protect(mes));
                }
            }
            if (b3) {
                for (String mes : query.getInfo()) {
                    println(protect(mes));
                }
            }
            println(CCOM);
        }
    }

    String protect(String mes) {
        return mes.replace(OCOM, "").replace(CCOM, "");
    }

    // for JSON subclass
    void newResult() {
    }

    void display(String var, IDatatype dt) {
        if (dt == null) {
            return;
        }
        print(String.format("<binding name='%s'>", getName(var)));
        display(dt);
        println("</binding>");
    }
    
    void display(IDatatype dt) {
        String str = dt.getLabel();
        if (dt.isList()) {
            printList(dt);
        }
        else if (dt.isLiteral()) {
            str = toXML(str);

            if (dt.hasLang()) {
                printf("<literal xml:lang='%s'>%s</literal>", dt.getLang(), str);
            } else if (dt.getDatatype() != null && dt.getCode() != IDatatype.LITERAL) {
                if (dt.isExtension()) {
                    str = toXML(dt.getContent());
                }
                printf("<literal datatype='%s'>%s</literal>",
                        dt.getDatatype().getLabel() ,str);
            } else {
                printf("<literal>%s</literal>" ,str );
            }
        } else if (dt.isTripleWithEdge()) {
            // rdf star triple
            print(dt.getEdge());            
        }          
        else if (dt.isBlank()) {
            printf("<bnode>%s</bnode>", str);
        } 
        else if (dt.isURI()) {
            printf("<uri>%s</uri>", StringEscapeUtils.escapeXml(str));
        }
    }
    
    void printList(IDatatype list) {
        println("<list>");
        for (IDatatype dt : list) {
            display(dt);
            println();
        }
        println("</list>");
    }
    
    void print(Edge e) {
        println("<triple>");
        print("<subject>");
        display(e.getSubjectValue());
        println("</subject>");
        print("<predicate>");
        display(e.getPredicateValue());
        println("</predicate>");
        print("<object>");
        display(e.getObjectValue());
        println("</object>");
        println("</triple>");
    }

    // protect & <
    String toXML(String str) {
        for (String p : XML) {
            if (str.indexOf(p) != -1) {
                str = ODATA + str + CDATA;
                return str;
            }
        }
        return str;
    }

    public void printAsk() {
        String res = "true";
        if (getMappings() == null || getMappings().size() == 0) {
            res = "false";
        }
        print(OBOOLEAN);
        print(res);
        println(CBOOLEAN);
    }

    public void printEmpty(int clause) {
        println(OHEADER);
        println(OHEAD);
        println(CHEAD);
        if (ast.isSelect()) {
            print(ORESULTS);
            println(">");
            println(CRESULTS);
        }
        println(CHEADER);
    }

}
