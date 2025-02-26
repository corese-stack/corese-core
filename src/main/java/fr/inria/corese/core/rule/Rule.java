package fr.inria.corese.core.rule;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.QueryLoad;
import fr.inria.corese.core.logic.Closure;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.core.UUIDFunction;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.printer.SPIN;
import fr.inria.corese.core.util.SPINProcess;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Rule {
    public static final String RULE_TYPE = NSManager.RULE + "rule";
    public static final String CONSTRAINT_TYPE = NSManager.RULE + "constraint";
    public static final String AXIOM = NSManager.OWL + "propertyChainAxiom";
    static final String TPVAR = "?t";
    static final int UNDEF = -1;
    static final int DEFAULT = 0;
    // transitive without triple pattern p a owl:TransitiveProperty
    // c1 subClassOf c3 where c1 subClassOf c2 . c2 subClassOf c3
    static final int TRANSITIVE = 1;
    // transitive with triple pattern p a owl:TransitiveProperty
    static final int GENERIC_TRANSITIVE = 2;
    // s type c2 where s type c1 . c1 subClassOf c2
    static final int PSEUDO_TRANSITIVE = 3;
    static String TRANS_QUERY = "";
    static String TRANS_PSEUDO_QUERY = "";
    static int COUNT = 0;

    static {
        try {
            QueryLoad ql = QueryLoad.create();
            TRANS_QUERY = ql.readWE(Rule.class.getResourceAsStream("/query/transitive.rq"));
            TRANS_PSEUDO_QUERY = ql.readWE(Rule.class.getResourceAsStream("/query/transitivepseudo.rq"));
        } catch (LoadException ex) {
            LoggerFactory.getLogger(Rule.class.getName()).error("", ex);
        }
    }

    Query query;
    List<Node> predicates;
    String name;
    int num;
    int rtype = UNDEF;
    private Closure closure;
    private Record ruleRecord;
    private double time = 0.0;
    private boolean isGeneric = false;
    private boolean isClosure = false;
    private Node provenance;
    private String type = RULE_TYPE;
    private boolean constraint = false;
    private boolean optimize = true;

    public Rule(String n, Query q) {
        query = q;
        name = n;
        q.setURI(n);
    }

    public Rule(String n, Query q, String type) {
        this(n, q);
        if (type != null) {
            setRuleType(type);
            setConstraint(type.equals(CONSTRAINT_TYPE));
        }
    }

    public static Rule create(String n, Query q) {
        return new Rule(n, q);
    }

    public static Rule create(String n, Query q, String type) {
        return new Rule(n, q, type);
    }

    public static Rule create(Query q) {
        return new Rule(UUIDFunction.getUUID(), q);
    }

    String toGraph() {
        ASTQuery ast = getQuery().getAST();
        SPIN sp = SPIN.create();
        sp.visit(ast, "kg:r" + getIndex());
        return sp.toString();
    }

    void set(List<Node> list) {
        predicates = list;
    }

    List<Node> getPredicates() {
        return predicates;
    }

    public Query getQuery() {
        return query;
    }

    public Object getAST() {
        return query.getAST();
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return num;
    }

    public void setIndex(int n) {
        num = n;
    }

    /**
     * Index of edge with predicate p If there is only one occurrence of p
     */
    public int getIndex(Node p) {

        return -1;
    }

    public boolean isGeneric() {
        return isGeneric;
    }

    void setGeneric(boolean b) {
        isGeneric = true;
    }

    public int type() {
        if (rtype == UNDEF) {
            rtype = getType();
        }
        return rtype;
    }

    /**
     * Detect if rule is transitive
     */
    int getType() {
        try {
            SPINProcess sp = SPINProcess.create();
            Graph g = sp.toSpinGraph((ASTQuery) getAST());
            QueryProcess exec = QueryProcess.create(g);
            Mappings map = exec.query(TRANS_QUERY);

            int res = DEFAULT;
            if (map.size() > 0) {
                if (map.getNode(TPVAR) == null) {
                    // without ?p a owl:TransitiveProperty
                    res = TRANSITIVE;
                } else {
                    // with ?p a owl:TransitiveProperty
                    res = GENERIC_TRANSITIVE;
                }
            } else {
                map = exec.query(TRANS_PSEUDO_QUERY);
                if (map.size() > 0) {
                    // s type c2 where s type c1 . c1 subClassOf c2
                    res = PSEUDO_TRANSITIVE;
                }
            }
            return res;

        } catch (EngineException ex) {
            return DEFAULT;
        }

    }

    public int getNewEdgeIndex() {
        switch (type()) {
            case TRANSITIVE:
            case PSEUDO_TRANSITIVE:
                return 0;
            case GENERIC_TRANSITIVE:
                // edge 0 is ?p a owl:TransitiveProperty
                // edge 1 is ?s ?p ?v, consider new edges for this one 
                return 1;
            default:
                return -1;
        }
    }

    public boolean isAnyTransitive() {
        return (type() == TRANSITIVE
                || type() == GENERIC_TRANSITIVE);
    }

    public boolean isTransitive() {
        return (type() == TRANSITIVE);
    }

    public boolean isGTransitive() {
        return type() == GENERIC_TRANSITIVE;
    }

    public boolean isPseudoTransitive() {
        return (type() == PSEUDO_TRANSITIVE);
    }

    /**
     * this = x type c2        :- x type c1        & c1 subclassof c2
     * r    = c1 subclassof c3 :- c1 subclassof c2 & c2 subclassof c3
     */
    public boolean isPseudoTransitive(Rule r) {
        if (isPseudoTransitive() && r.isTransitive()) {
            Node p = r.getUniquePredicate();
            Node pp = getQuery().getBody().get(1).getEdge().getEdgeNode();
            return p.equals(pp);
        }
        return false;
    }


    public boolean isClosure() {
        return isClosure;
    }

    public Node getPredicate(int i) {
        return query.getBody().get(i).getEdge().getEdgeNode();
    }

    public Node getUniquePredicate() {
        Exp cons = query.getConstruct();
        if (cons.size() == 1 && cons.get(0).isEdge()) {
            Edge edge = cons.get(0).getEdge();
            if (edge.getEdgeVariable() == null) {
                return edge.getEdgeNode();
            }
        }
        return null;
    }

    /**
     * @return the closure
     */
    public Closure getClosure() {
        return closure;
    }

    public void setClosure(boolean b) {
        isClosure = b;
    }

    /**
     * @param closure the closure to set
     */
    public void setClosure(Closure closure) {
        this.closure = closure;
    }

    public void clean() {
        closure = null;
        ruleRecord = null;
    }

    /**
     * @return the time
     */
    public double getTime() {
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(double time) {
        this.time = time;
    }

    /**
     * @return the provenance
     */
    public Node getProvenance() {
        return provenance;
    }

    /**
     * @param provenance the provenance to set
     */
    public void setProvenance(Node provenance) {
        this.provenance = provenance;
    }

    /**
     * @return the record
     */
    public Record getRuleRecord() {
        return ruleRecord;
    }

    /**
     * @param ruleRecord the record to set
     */
    public void setRuleRecord(Record ruleRecord) {
        this.ruleRecord = ruleRecord;
    }

    public String getRuleType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setRuleType(String type) {
        this.type = type;
    }

    /**
     * @return the constraint
     */
    public boolean isConstraint() {
        return constraint;
    }

    /**
     * @param constraint the constraint to set
     */
    public void setConstraint(boolean constraint) {
        this.constraint = constraint;
    }

    public boolean isOptimize() {
        return optimize;
    }

    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }
}
