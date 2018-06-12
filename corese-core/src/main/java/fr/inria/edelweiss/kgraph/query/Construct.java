package fr.inria.edelweiss.kgraph.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.triple.parser.Dataset;
import fr.inria.edelweiss.kgram.api.core.Edge;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.core.Exp;
import fr.inria.edelweiss.kgram.core.Mapping;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgraph.core.Event;
import fr.inria.edelweiss.kgtool.util.Duplicate;

/**
 * construct where describe where delete where
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class Construct
        implements Comparator<Node> {

    private static Logger logger = LogManager.getLogger(Construct.class);
    static final String BLANK = "_:b_";
    static final String DOT = ".";
    int count = 0, ruleIndex = 0, index = 0;
    String root;
    Query query;
    GraphManager graph;
    Node defaultGraph;
    //IDatatype dtDefaultGraph;
    List<Entity> lInsert, lDelete;
    Dataset ds;
    boolean isDebug = false,
            isDelete = false,
            isRule = false,
            isInsert = false,
            isBuffer = false;
    private boolean test = false;
    Object rule;
    HashMap<Node, Node> table;
    Duplicate duplicate;
    private int loopIndex = -1;
    private Node prov;
    

    Construct(Query q, Dataset ds) {
        this(q);
        this.ds = ds;
    }

    Construct(Query q) {
        query = q;
        table = new HashMap<Node, Node>();
        count = 0;
        //dtDefaultGraph = DatatypeMap.createResource(src);
        duplicate =  Duplicate.create();
    }

    public static Construct create(Query q) {
        Construct cons = new Construct(q);
        if (q.isDetail()) {
            if (q.isConstruct()) {
                cons.setInsertList(new ArrayList<Entity>());
            } else {
                cons.setDeleteList(new ArrayList<Entity>());
            }
        }
        return cons;
    }
    
    public static Construct create(Query q, GraphManager g) {
        Construct cons = create(q);
        cons.set(g);
        return cons;
    }

    public static Construct create(Query q, Dataset ds) {
        Construct cons = new Construct(q, ds);
        return cons;
    }

    public void setDefaultGraph(Node n) {
        defaultGraph = n;
    }

    public void setBuffer(boolean b) {
        isBuffer = b;
    }    
    
    public void set(GraphManager g){
       graph = g; 
    }
    
     public boolean isBuffer() {
        return isBuffer;
    }

    public void setDelete(boolean b) {
        isDelete = b;
    }

    public void setInsert(boolean b) {
        isInsert = b;
    }

    public void setRule(Object r, int n, Node prov) {
        isRule = true;
        rule = r;
        root = BLANK + n + DOT;
        this.prov = prov;
    }

    public void setDebug(boolean b) {
        isDebug = b;
    }

    public void setInsertList(List<Entity> l) {
        lInsert = l;
    }

    public List<Entity> getInsertList() {
        return lInsert;
    }

    public void setDeleteList(List<Entity> l) {
        lDelete = l;
    }

    public List<Entity> getDeleteList() {
        return lDelete;
    }

    public void construct(Mappings map) {
         construct(map, null);
    }
    
    public void delete(Mappings map, Dataset ds) {
        setDelete(true);
        if (ds != null && ds.isUpdate()) {
            this.ds = ds;
        }
        construct(map, null);
    }

    public void insert(Mappings map, Dataset ds) {
        setInsert(true);
        if (ds != null && ds.isUpdate()) {
            this.ds = ds;
        }
        construct(map, null);
    }
         
    Event event() {
        return (isDelete) ? Event.Delete : (isInsert) ? Event.Insert : Event.Construct ;
    }
    
    public void construct(Mappings map, Environment env) {  
        graph.start(event());
        Exp exp = query.getConstruct();
        if (isDelete) {
            exp = query.getDelete();
        }

        if (exp.first().isGraph()) {
            // draft: graph kg:system { }
            // in GraphStore
            Node gNode = exp.first().getGraphName();
            if (gNode.isConstant()){
                GraphManager m = graph.getNamedGraph(gNode.getLabel());
                if (m != null){
                    set(m);
                }
            }
        }


        //init();

        Node gNode = defaultGraph;
        if (gNode == null) {
            //gNode = graph.getResourceNode(dtDefaultGraph);
            gNode = graph.getDefaultGraphNode();
        }

        if (isDelete) {
            gNode = null;
        }

        if (lInsert != null) {
            map.setInsert(lInsert);
        }
        if (lDelete != null) {
            map.setDelete(lDelete);
        }

        if (env != null) {
            // construct one solution in env
            clear();
            construct(gNode, exp, map, env);
        } else {
            for (Mapping m : map) {
                // each map has its own blank nodes:
                clear();
                construct(gNode, exp, map, m);
            }
        }

        graph.finish(event());
        
    }

    /**
     * Recursive construct of exp with map Able to process construct graph ?g
     * {exp}
     */
    void construct(Node gNode, Exp exp, Mappings map, Environment env) {
        if (exp.isGraph()) {
            gNode = exp.getGraphName();
            exp = exp.rest();
        }

        for (Exp ee : exp.getExpList()) {
            if (ee.isEdge()) {
                Entity ent = construct(gNode, ee.getEdge(), env);
                if (ent != null) {
                    // RuleEngine loop index
                    ent.getEdge().setIndex(loopIndex);
                    if (isDelete) {
                        if (isDebug) {
                            logger.debug("** Delete: " + ent);
                        }
                        List<Entity> list = null;
                        if (gNode == null && ds != null && ds.hasFrom()) {
                            // delete in default graph
                            list = graph.delete(ent, ds.getFrom());
                        } else {
                            // delete in all named graph
                            list = graph.delete(ent);
                        }
                        if (list != null) {
                            map.setNbDelete(map.nbDelete() + list.size());

                            if (lDelete != null) {
                                lDelete.addAll(list);
                            }
                        }

                    } else {
                        if (isDebug) {
                            logger.debug("** Construct: " + ent);
                        }
                        if (!isBuffer) {
                            // isBuffer means: bufferise edges in a list 
                            // that will be processed later by RuleEngine
                            // otherwise, store edge in graph rigth now
                            ent = graph.insert(ent);
                        }
                        if (ent != null) {
                            map.setNbInsert(map.nbInsert() + 1);
                            if (lInsert != null) {
                                // buffer where to store edges
                                lInsert.add(ent);
                            }

                            if (isInsert) {
                                // When insert in new graph g, update dataset named += g
                                if (ds != null) {
                                    String name = ent.getGraph().getLabel();
                                    if (! graph.isDefaultGraphNode(name)) {
                                        ds.addNamed(name);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                construct(gNode, ee, map, env);
            }
        }
    }

    /**
     * Clear blank node table
     */
    void clear() {
        table.clear();
    }

    void init() {
        table.clear();
        count = 0;
    }

    /**
     * Construct target edge from query edge and map
     */
    Entity construct(Node gNode, Edge edge, Environment env) {

        Node pred = edge.getEdgeVariable();
        if (pred == null) {
            pred = edge.getEdgeNode();
        }

        Node source = null;
        if (gNode != null) {
            source = construct(gNode, env);
        }
        Node property = construct(pred, env);

        Node subject = construct(source, edge.getNode(0), env);
        Node object  = construct(source, edge.getNode(1), env);

        if ((source == null && !isDelete) || subject == null || property == null || object == null) {
            return null;
        }
        
        if (isBuffer){
            // optimized mode for Rule Engine
            if (graph.exist(property, subject, object)){
                // edge already exists: skip it
                return null;
            } 
            if (duplicate.exist(property, subject, object)){
                // edge already recorded: skip it
                return null;
            }
        }

        if (isDelete) {
            if (gNode == null) {
                source = null;
            }
        } else {
            graph.add(subject);
            graph.add(object);
            graph.addPropertyNode(property);
            source = graph.addGraphNode(source);
            
        }

        Entity ee;

        if (edge.nbNode() > 2) {
            // tuple()
            ArrayList<Node> list = new ArrayList<Node>();
            list.add(subject);
            list.add(object);

            for (int i = 2; i < edge.nbNode(); i++) {
                Node n = construct(source, edge.getNode(i), env);
                if (n != null) {
                    graph.add(n);
                    list.add(n);
                }
            }

            ee = create(source, property, list);
        } else {
            ee = create(source, subject, property, object);
        }
        if (prov != null){
            ee.setProvenance(prov);
        }
        return ee;
    }

    Entity create(Node source, Node property, List<Node> list) {
        if (isDelete) {
            return graph.createDelete(source, property, list);
        } else {
            return graph.create(source, property, list);
        }
    }

    Entity create(Node source, Node subject, Node property, Node object) {
        if (isDelete) {
            return graph.createDelete(source, subject, property, object);
        } else {
            return graph.create(source, subject, property, object);
        }
    }

    /**
     * Construct target node from query node and map
     */
    Node construct(Node qNode, Environment map) {
        return construct(null, qNode, map);
    }

    Node construct(Node gNode, Node qNode, Environment map) {
        // search target node
        Node node = table.get(qNode);

        if (node == null) {
            // target node not yet created
            // search map node
            Node nn = map.getNode(qNode.getLabel());
            Object value = null;
            if (nn != null) {
                value = nn.getValue();
            }
            IDatatype dt = null;

            if (value == null) {
                if (qNode.isBlank()) {
                    dt = blank(qNode, map);
                } else if (qNode.isConstant()) {
                    // constant
                    dt = getValue(qNode);
                } else {
                    // unbound variable
                    return null;
                }
            } else {
                dt = (IDatatype) value;
            }
            node = graph.getNode(gNode, dt);
            table.put(qNode, node);
        }

        return node;
    }
    
    Node getCreateNode(Node gNode, IDatatype dt){
        if (isDelete){
            return graph.getNode(gNode, dt);
        }
        return graph.getCreateNode(gNode, dt);
    }

    IDatatype blank(Node qNode, Environment map) {
        String str;
        if (isRule) {
            str = blankRule(qNode, map);
        } else {
            str = graph.newBlankID();
        }
        IDatatype dt = graph.createBlank(str);
        return dt;
    }

    /**
     * Create a unique BN ID according to (Rule, qNode & Mapping) If the rule
     * runs twice on same mapping, it will create same BN graph will detect it,
     * hence engine will not loop
     *
     */
    String blankRule(Node qNode, Environment map) {
        // _:b + rule ID + "." + qNode ID
        StringBuffer sb = new StringBuffer(root);
        sb.append(getIndex(qNode));

        for (Node qnode : map.getQueryNodes()) {
            if (qnode != null && qnode.isVariable() && !qnode.isBlank()) {
                // node value ID
                sb.append(DOT);
                sb.append(map.getNode(qnode).getIndex());
            }
        }

        return sb.toString();
    }

    /**
     * Generate an index for construct Node
     */
    int getIndex(Node qNode) {
        int n = qNode.getIndex();
        if (n == -1) {
            n = index++;
            qNode.setIndex(n);
        }
        return n;
    }

    IDatatype getValue(Node node) {
        return (IDatatype) node.getValue();
    }

    String getID(Mapping map) {
        String str = "";
        List<Node> list = new ArrayList<Node>();
        for (Node node : map.getQueryNodes()) {
            list.add(node);
        }
        Collections.sort(list, this);
        int n = 0;
        for (Node qNode : list) {
            Object value = map.getValue(qNode);
            n++;
            if (value != null && !qNode.isConstant()) {
                IDatatype dt = (IDatatype) value;
                str += qNode.getLabel() + "." + dt.toSparql() + ".";
            }
        }
        return str;
    }

    public int compare(Node n1, Node n2) {
        return n1.compare(n2);
    }

    public void setLoopIndex(int n) {
        loopIndex = n;
    }

    /**
     * @return the test
     */
    public boolean isTest() {
        return test;
    }

    /**
     * @param test the test to set
     */
    public void setTest(boolean test) {
        this.test = test;
    }
}
