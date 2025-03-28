package fr.inria.corese.core.print;

import static fr.inria.corese.core.sparql.triple.parser.URLParam.LINK;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import fr.inria.corese.core.compiler.parser.Pragma;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.print.rdfc10.HashingUtility.HashAlgorithm;
import fr.inria.corese.core.transform.Transformer;
import fr.inria.corese.core.util.MappingsGraph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.Dataset;
import fr.inria.corese.core.sparql.triple.parser.Metadata;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.parser.URLParam;

/**
 * Select Result format according to query form and @display annotation.,
 * Olivier Corby Edelweiss INRIA 2011 - Wimmics 2016
 */
public class ResultFormat implements ResultFormatDef {

    public static final String SPARQL_RESULTS_XML = "application/sparql-results+xml";
    public static final String SPARQL_RESULTS_JSON = "application/sparql-results+json";
    public static final String SPARQL_RESULTS_CSV = "text/csv";
    public static final String SPARQL_RESULTS_TSV = "text/tab-separated-values";
    public static final String SPARQL_RESULTS_MD = "text/markdown";

    static final String HEADER = "<html>\n"
            + "<head>\n"
            + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
            + "</head>\n"
            + "<body>\n"
            + "<pre>%s</pre>"
            + "</body>\n"
            + "</html>\n";

    public static final String XML = "application/xml";
    public static final String HTML = "text/html";
    public static final String SPARQL_QUERY = "application/sparql-query";
    public static final String JSON_LD = "application/ld+json";
    public static final String JSON = "application/json";
    public static final String RDF_XML = "application/rdf+xml";
    public static final String TRIG = "application/trig";
    public static final String TURTLE = "application/turtle";
    public static final String TURTLE_TEXT = "text/turtle";
    public static final String TRIG_TEXT = "text/trig";
    public static final String NT_TEXT = "text/nt";
    public static final String TEXT = "text/plain";
    public static final String N3 = "text/n3";
    public static final String N_TRIPLES = "application/n-triples";
    public static final String N_QUADS = "application/n-quads";

    public static ResultFormatDef.format DEFAULT_SELECT_FORMAT = ResultFormatDef.format.XML_FORMAT;
    public static ResultFormatDef.format DEFAULT_CONSTRUCT_FORMAT = ResultFormatDef.format.RDF_XML_FORMAT;

    private Mappings map;
    private Graph graph;
    private Binding bind;
    private Context context;
    private NSManager nsmanager;
    ResultFormatDef.format type = ResultFormatDef.format.UNDEF_FORMAT;
    private ResultFormatDef.format transformType = ResultFormatDef.format.UNDEF_FORMAT;
    private ResultFormatDef.format construct_format = DEFAULT_CONSTRUCT_FORMAT;
    private ResultFormatDef.format select_format = DEFAULT_SELECT_FORMAT;
    private long nbResult = Long.MAX_VALUE;
    private int nbTriple = Integer.MAX_VALUE;
    private boolean selectAll = false;
    private boolean transformer;
    private String transformation;
    private String contentType;


    static HashMap<String, ResultFormatDef.format> table;
    static HashMap<String, ResultFormatDef.format> format;
    static HashMap<ResultFormatDef.format, String> content;

    static {
        init();
        initFormat();
    }

    static void init() {
        table = new HashMap<>();
        table.put(Metadata.DISPLAY_TURTLE, ResultFormatDef.format.TURTLE_FORMAT);
        table.put(Metadata.DISPLAY_RDF_XML, ResultFormatDef.format.RDF_XML_FORMAT);
        table.put(Metadata.DISPLAY_JSON_LD, ResultFormatDef.format.JSONLD_FORMAT);

        table.put(Metadata.DISPLAY_RDF, ResultFormatDef.format.RDF_FORMAT);
        table.put(Metadata.DISPLAY_XML, ResultFormatDef.format.XML_FORMAT);
        table.put(Metadata.DISPLAY_JSON, ResultFormatDef.format.JSON_FORMAT);

    }

    static void initFormat() {
        format = new HashMap<>();
        content = new HashMap<>();

        // use case: template without format
        defContent(TEXT, ResultFormatDef.format.TEXT_FORMAT);

        // Mappings
        defContent(SPARQL_RESULTS_JSON, ResultFormatDef.format.JSON_FORMAT);
        defContent(SPARQL_RESULTS_XML, ResultFormatDef.format.XML_FORMAT);
        defContent(SPARQL_RESULTS_CSV, ResultFormatDef.format.CSV_FORMAT);
        defContent(SPARQL_RESULTS_TSV, ResultFormatDef.format.TSV_FORMAT);
        defContent(SPARQL_RESULTS_MD, ResultFormatDef.format.MARKDOWN_FORMAT);

        // Graph
        defContent(RDF_XML, ResultFormatDef.format.RDF_XML_FORMAT);
        defContent(TURTLE_TEXT, ResultFormatDef.format.TURTLE_FORMAT);
        defContent(TRIG, ResultFormatDef.format.TRIG_FORMAT);
        defContent(JSON_LD, ResultFormatDef.format.JSONLD_FORMAT);
        defContent(N_TRIPLES, ResultFormatDef.format.NTRIPLES_FORMAT);
        defContent(N_QUADS, ResultFormatDef.format.NQUADS_FORMAT);

        format.put(TRIG_TEXT, ResultFormatDef.format.TRIG_FORMAT);
        format.put(NT_TEXT, ResultFormatDef.format.TURTLE_FORMAT);
        format.put(TURTLE, ResultFormatDef.format.TURTLE_FORMAT);
        format.put(XML, ResultFormatDef.format.XML_FORMAT);
        format.put(HTML, ResultFormatDef.format.HTML_FORMAT);

        // shortcut for HTTP parameter format=
        format.put("text", ResultFormatDef.format.TEXT_FORMAT);
        format.put("html", ResultFormatDef.format.HTML_FORMAT);

        format.put("json", ResultFormatDef.format.JSON_FORMAT);
        format.put("xml", ResultFormatDef.format.XML_FORMAT);
        format.put("csv", ResultFormatDef.format.CSV_FORMAT);
        format.put("tsv", ResultFormatDef.format.TSV_FORMAT);
        format.put("markdown", ResultFormatDef.format.MARKDOWN_FORMAT);

        format.put("jsonld", ResultFormatDef.format.JSONLD_FORMAT);
        format.put("rdf", ResultFormatDef.format.TURTLE_FORMAT);
        format.put("turtle", ResultFormatDef.format.TURTLE_FORMAT);
        format.put("trig", ResultFormatDef.format.TRIG_FORMAT);
        format.put("rdfxml", ResultFormatDef.format.RDF_XML_FORMAT);
        format.put("nt", ResultFormatDef.format.NTRIPLES_FORMAT);
        format.put("nq", ResultFormatDef.format.NQUADS_FORMAT);
    }

    static void defContent(String f, ResultFormatDef.format t) {
        format.put(f, t);
        content.put(t, f);
    }

    ResultFormat(Mappings m) {
        map = m;
    }

    ResultFormat(Graph g) {
        graph = g;
    }

    ResultFormat(Mappings m, ResultFormatDef.format type) {
        this(m);
        this.type = type;
    }

    ResultFormat(Mappings m, ResultFormatDef.format sel, ResultFormatDef.format cons) {
        this(m);
        this.select_format = sel;
        this.construct_format = cons;
    }

    ResultFormat(Graph g, ResultFormatDef.format type) {
        this(g);
        this.type = type;
    }
    
    ResultFormat(Graph g, NSManager nsm, ResultFormatDef.format type) {
        this(g);
        setNsmanager(nsm);
        this.type = type;
    }

    public static ResultFormat create(Mappings m) {
        return new ResultFormat(m, type(m));
    }

    /**
     * format: application/sparql-results+xml
     * format may be null
     */
    public static ResultFormat create(Mappings m, String format) {
        String myFormat = tuneFormat(m, format);
        if (myFormat == null) {
            return create(m);
        }
        ResultFormatDef.format type = getType(myFormat);
        return new ResultFormat(m, type);
    }

    public static ResultFormat create(Mappings m, String format, String trans) {
        ResultFormat rf = createFromTrans(m, trans);
        if (rf != null) {
            return rf;
        }
        return create(m, format).transform(trans);
    }

    public static ResultFormat create(Mappings m, ResultFormatDef.format type, String trans) {
        ResultFormat rf = createFromTrans(m, trans);
        if (rf != null) {
            // remember the type in case it is html asked by bowser
            rf.setTransformType(type);
            return rf;
        }
        return create(m, type).transform(trans);
    }

    static ResultFormat createFromTrans(Mappings m, String trans) {
        if (trans == null) {
            return null;
        }
        switch (NSManager.nsm().toNamespace(trans)) {
            case Transformer.XML:
                return create(m, ResultFormatDef.format.XML_FORMAT);
            case Transformer.JSON:
                return create(m, ResultFormatDef.format.JSON_FORMAT);
            case Transformer.JSON_LD:
                return create(m, ResultFormatDef.format.JSONLD_FORMAT);
            case Transformer.RDF:
                return create(m, ResultFormatDef.format.RDF_FORMAT);
            case Transformer.RDFXML:
                return create(m, ResultFormatDef.format.RDF_XML_FORMAT);
            default:
                return null;
        }
    }

    ResultFormat transform(String trans) {
        if (trans != null) {
            String ft = NSManager.nsm().toNamespace(trans);
            setTransformer(true);
            setTransformation(ft);
        }
        return this;
    }

    // special case: template without format considered as text format
    static String tuneFormat(Mappings m, String format) {
        if (m.getQuery() != null) {
            if (format == null) {
                if (m.getQuery().isTemplate()) {
                    return TEXT; // "text/plain";
                }
            }
        }
        return format;
    }

    // in case where type = text
    static ResultFormatDef.format defaultType(Mappings map) {
        return map.getGraph() == null ? DEFAULT_SELECT_FORMAT : ResultFormatDef.format.TURTLE_FORMAT;
    }

    static public ResultFormat format(Mappings m) {
        return new ResultFormat(m, DEFAULT_SELECT_FORMAT, ResultFormatDef.format.TURTLE_FORMAT);
    }

    static public ResultFormat create(Mappings m, ResultFormatDef.format type) {
        return new ResultFormat(m, type);
    }

    static public ResultFormat create(Mappings m, ResultFormatDef.format sel, ResultFormatDef.format cons) {
        return new ResultFormat(m, sel, cons);
    }

    static public ResultFormat create(Graph g) {
        return new ResultFormat(g);
    }

    static public ResultFormat create(Graph g, ResultFormatDef.format type) {
        return new ResultFormat(g, type);
    }
    
    static public ResultFormat create(Graph g, NSManager nsm, ResultFormatDef.format type) {
        return new ResultFormat(g, nsm, type);
    }

    static public ResultFormat create(Graph g, String type) {
        return new ResultFormat(g, getSyntax(type));
    }

    // no type was given at creation
    static ResultFormatDef.format type(Mappings m) {
        ResultFormatDef.format type = ResultFormatDef.format.UNDEF_FORMAT;
        if (m.getQuery().isTemplate()) {
            return ResultFormatDef.format.TEXT_FORMAT;
        }
        ASTQuery ast = (ASTQuery) m.getAST();
        if (ast != null && ast.hasMetadata(Metadata.DISPLAY)) {
            String val = ast.getMetadata().getValue(Metadata.DISPLAY);
            type = table.get(val);
            if (type == null) {
                type = ResultFormatDef.format.UNDEF_FORMAT;
            }
        }
        return type;
    }

    // str = application/sparql-results+json OR json
    public static ResultFormatDef.format getFormat(String str) {
        if (str != null && format.containsKey(str)) {
            return format.get(str);
        }
        return DEFAULT_SELECT_FORMAT;
    }

    public static ResultFormatDef.format getFormatUndef(String str) {
        if (str != null && format.containsKey(str)) {
            return format.get(str);
        }
        return ResultFormatDef.format.UNDEF_FORMAT;
    }

    public static String getFormat(ResultFormatDef.format type) {
        String ft = content.get(type);
        if (ft == null) {
            return getFormat(DEFAULT_SELECT_FORMAT);
        }
        return ft;
    }

    // json -> application/json
    public static String decode(String ft) {
        return getFormat(getFormat(ft));
    }

    // sparql update load URL format
    // rdfxml -> application/rdf+xml
    public static String decodeLoadFormat(String ft) {
        if (format.containsKey(ft)) {
            ResultFormatDef.format type = format.get(ft);
            return content.get(type);
        }
        return null;
    }

    public static String decodeOrText(String ft) {
        ResultFormatDef.format type = getFormatUndef(ft);
        if (type == ResultFormatDef.format.UNDEF_FORMAT) {
            return TEXT;
        }
        return getFormat(type);
    }

    static ResultFormatDef.format getType(String ft) {
        return getFormat(ft);
    }

    @Override
    public String toString() {
        if (isTransformer()) {
            return transformer();
        } else if (getMappings() == null) {
            return graphToString();
        } else {
            return mapToString();
        }
    }

    String transformer() {
        Transformer t = Transformer.create(theGraph(), getMappings(), getTransformation());
        if (getContext() != null) {
            t.setContext(getContext());
            if (getContext().hasValue(URLParam.DEBUG)) {
                t.setDebug(true);
            }
        }
        if (getBind() != null) {
            t.setBinding(getBind());
        }
        return t.toString();
    }

    public ResultFormat init(Dataset ds) {
        setContext(ds.getContext());
        setBind(ds.getBinding());
        return this;
    }

    Graph theGraph() {
        if (getGraph() != null) {
            return getGraph();
        } else if (getMappings().getGraph() != null) {
            return (Graph) getMappings().getGraph();
        } else {
            return Graph.create();
        }
    }

    public String toString(IDatatype dt) {
        Node node = getGraph().getNode(dt);
        if (node == null) {
            return dt.toString();
        }
        return graphToString(node);
    }

    static ResultFormatDef.format getSyntax(String syntax) {
        if (syntax.equals(Transformer.RDFXML)) {
            return ResultFormatDef.format.RDF_XML_FORMAT;
        }
        return ResultFormatDef.format.TURTLE_FORMAT;
    }

    String graphToString() {
        return graphToString(null);
    }

    String graphToString(Node node) {
        if (type() == ResultFormatDef.format.UNDEF_FORMAT) {
            setType(getConstructFormat());
        }
        switch (type) {
            case RDF_XML_FORMAT:
                return RDFFormat.create(getGraph()).toString();
            case TRIG_FORMAT:
                return TripleFormat.create(getGraph(), getNsmanager(), true)
                        .setNbTriple(getNbTriple()).toString(node);
            case JSONLD_FORMAT:
                return JSONLDFormat.create(getGraph()).toString();
            case NTRIPLES_FORMAT:
                return NTriplesFormat.create(getGraph()).toString();
            case NQUADS_FORMAT:
                return NQuadsFormat.create(getGraph()).toString();
            case RDFC10_FORMAT:
                return CanonicalRdf10Format.create(getGraph(), HashAlgorithm.SHA_256).toString();
            case RDFC10_SHA384_FORMAT:
                return CanonicalRdf10Format.create(getGraph(), HashAlgorithm.SHA_384).toString();
            case TURTLE_FORMAT:
            default:
                // e.g. HTML
                TripleFormat tf = TripleFormat.create(getGraph(), getNsmanager());
                String str = tf.setNbTriple(getNbTriple()).toString(node);
                if (type() == ResultFormatDef.format.HTML_FORMAT) {
                    return html(str);
                }
                return str;
        }
    }

    String mapToString() {
        Query q = getMappings().getQuery();
        if (q == null) {
            return "";
        }

        if (q.isTemplate()) {
            return getMappings().getTemplateStringResult();
        } else if (q.hasPragma(Pragma.TEMPLATE) && getMappings().getGraph() != null) {
            return TemplateFormat.create(getMappings()).toString();
        } else {
            if (type() == ResultFormatDef.format.UNDEF_FORMAT) {
                if (q.isConstruct()) {
                    setType(getConstructFormat());
                } else {
                    setType(getSelectFormat());
                }
            }

            return process(getMappings());
        }
    }

    boolean isGraphFormat(ResultFormatDef.format type) {
        switch (type) {
            case RDF_XML_FORMAT:
            case TURTLE_FORMAT:
            case TRIG_FORMAT:
            case JSONLD_FORMAT:
            case NTRIPLES_FORMAT:
            case NQUADS_FORMAT:
            case RDFC10_FORMAT:
            case RDFC10_SHA384_FORMAT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Tune the format
     */
    String process(Mappings map) {
        ResultFormatDef.format mytype = type();
        if (isGraphFormat(mytype) && map.getGraph() == null) {
            // return Mappings as W3C RDF Graph Mappings
            // map.
            map.setGraph(MappingsGraph.create(map).getGraph());
        } else if (mytype == ResultFormatDef.format.TEXT_FORMAT || mytype == ResultFormatDef.format.HTML_FORMAT) {
            // Chose appropriate format
            // Content-Type remains the same, do not setType()
            mytype = defaultType(map);
        }

        String res = processBasic(map, mytype);

        if ((type() == ResultFormatDef.format.HTML_FORMAT) || (getTransformType() == ResultFormatDef.format.HTML_FORMAT && (getContext() == null || !getContext().hasValue(LINK)))) {
            // browser need html
            // transform=st:xml and no mode=link : browser nee html
            return html(res);
        }
        return res;
    }

    /**
     * Main function
     * map may contain a graph (construct OR W3C RDF graph format for Mappings)
     */
    String processBasic(Mappings map, ResultFormatDef.format type) {
        switch (type) {
            // map is graph
            case RDF_XML_FORMAT:
                return RDFFormat.create(map).toString();
            case TURTLE_FORMAT:
                return TripleFormat.create(map).setNbTriple(getNbTriple()).toString();
            case TRIG_FORMAT:
                return TripleFormat.create(map, true).setNbTriple(getNbTriple()).toString();
            case JSONLD_FORMAT:
                return JSONLDFormat.create(map).toString();
            case NTRIPLES_FORMAT:
                return NTriplesFormat.create(map).toString();
            case NQUADS_FORMAT:
                return NQuadsFormat.create(map).toString();
            case RDFC10_FORMAT:
                return CanonicalRdf10Format.create(map, HashAlgorithm.SHA_256).toString();
            case RDFC10_SHA384_FORMAT:
                return CanonicalRdf10Format.create(map, HashAlgorithm.SHA_384).toString();

            case RDF_FORMAT:
                // W3C RDF Graph Mappings
                return RDFResultFormat.create(map).toString();

            case JSON_FORMAT:
                return JSONFormat.create(map)
                        .init(getContext()).toString();

            case CSV_FORMAT:
                return CSVFormat.create(map)
                        .init(getContext()).toString();

            case MARKDOWN_FORMAT:
                return MarkdownFormat.create(map)
                        .init(getContext()).toString();

            case TSV_FORMAT:
                return TSVFormat.create(map)
                        .init(getContext()).toString();

            // map is query result
            case XML_FORMAT:
            default:
                XMLFormat ft = XMLFormat.create(map);
                ft.init(getContext());
                ft.setSelectAll(isSelectAll());
                ft.setNbResult(nbResult);
                return ft.toString();
        }
    }

    String html(String str) {
        return String.format(HEADER, str.replace("<", "&lt;"));
    }

    public void write(String name) throws IOException {
        FileWriter fw = new FileWriter(name);
        String str = toString();
        fw.write(str);
        fw.flush();
        fw.close();
    }

    /**
     * @return the construct_format
     */
    public ResultFormatDef.format getConstructFormat() {
        return construct_format;
    }

    /**
     * @param construct_format the construct_format to set
     */
    public void setConstructFormat(ResultFormatDef.format construct_format) {
        this.construct_format = construct_format;
    }

    /**
     * @return the select_format
     */
    public ResultFormatDef.format getSelectFormat() {
        return select_format;
    }

    /**
     * @param select_format the select_format to set
     */
    public void setSelectFormat(ResultFormatDef.format select_format) {
        this.select_format = select_format;
    }

    public long getNbResult() {
        return nbResult;
    }

    public ResultFormat setNbResult(long nbResult) {
        this.nbResult = nbResult;
        return this;
    }

    public ResultFormatDef.format type() {
        return type;
    }

    ResultFormatDef.format setType(ResultFormatDef.format t) {
        type = t;
        return t;
    }

    public String getContentType() {
        String ct = content.get(type());
        if (ct == null) {
            ct = content.get(DEFAULT_SELECT_FORMAT);
        }
        return ct;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the selectAll
     */
    public boolean isSelectAll() {
        return selectAll;
    }

    /**
     * @param selectAll the selectAll to set
     */
    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    /**
     * @return the transformer
     */
    public boolean isTransformer() {
        return transformer;
    }

    /**
     * @param transformer the transformer to set
     */
    public void setTransformer(boolean transformer) {
        this.transformer = transformer;
    }

    /**
     * @return the transformation
     */
    public String getTransformation() {
        return transformation;
    }

    /**
     * @param transformation the transformation to set
     */
    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    /**
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * @param graph the graph to set
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * @return the map
     */
    public Mappings getMappings() {
        return map;
    }

    /**
     * @param map the map to set
     */
    public void setMappings(Mappings map) {
        this.map = map;
    }

    /**
     * @return the bind
     */
    public Binding getBind() {
        return bind;
    }

    /**
     * @param bind the bind to set
     */
    public void setBind(Binding bind) {
        this.bind = bind;
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(Context context) {
        this.context = context;
    }

    public ResultFormatDef.format getTransformType() {
        return transformType;
    }

    public void setTransformType(ResultFormatDef.format transformType) {
        this.transformType = transformType;
    }

    public int getNbTriple() {
        return nbTriple;
    }

    public ResultFormat setNbTriple(int nbTriple) {
        this.nbTriple = nbTriple;
        return this;
    }

    public NSManager getNsmanager() {
        return nsmanager;
    }

    public void setNsmanager(NSManager nsmanager) {
        this.nsmanager = nsmanager;
    }

}
