package fr.inria.corese.core.engine;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.inria.corese.core.compiler.eval.QuerySolver;
import fr.inria.corese.core.EdgeFactory;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.GraphStore;
import fr.inria.corese.core.api.Engine;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.query.QueryEngine;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.storage.api.Parameters;

/**
 * Test rule engines and pipeline
 *
 */
public class TestRuleEngine {

    static String data = TestRuleEngine.class.getResource("/data-test/").getPath();
    static Graph graph;
    static Engine rengine;
    static RuleEngine fengine;

    @BeforeClass
    public static void init() throws EngineException {
        QuerySolver.definePrefix("c", "http://www.inria.fr/acacia/comma#");

        graph = createGraph(true);
        Load load = Load.create(graph);
        QueryProcess.setPlanDefault(Query.QP_HEURISTICS_BASED);
        try {
            System.out.println(data + "engine/ontology/test.rdfs");
            System.out.println(data + "engine/data/test.rdf");
            System.out.println(data + "engine/rule/test2.brul");
            System.out.println(data + "engine/rule/meta.rul");

            load.parse(data + "engine/ontology/test.rdfs");
            load.parse(data + "engine/data/test.rdf");
            load.parse(data + "engine/rule/test2.brul");
            load.parse(data + "engine/rule/meta.rul", "meta.rul");
        } catch (LoadException e) {
            e.printStackTrace();
        }

        fengine = load.getRuleEngine();
        fengine.setSpeedUp(true);

        QueryProcess.create(graph);
    }

    @AfterClass
    static public void finish() {
        EdgeFactory.trace();
    }

    static GraphStore createGraph() {
        return createGraph(false);
    }

    static GraphStore createGraph(boolean b) {
        GraphStore g = GraphStore.create(b);
        Parameters p = Parameters.create();
        p.add(Parameters.type.MAX_LIT_LEN, 2);
        return g;
    }

    @Test
    public void testEnt() throws LoadException, EngineException {
        Graph g = Graph.create();
        String q = "select (xt:entailment() as ?g) where {"
                + "}";
        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(q);
        IDatatype dt = (IDatatype) map.getValue("?g");
        Graph gg = (Graph) dt.getPointerObject();
        assertEquals(7, gg.size());
    }

    @Test
    public void test57() {
        Graph graph = createGraph();

        QueryProcess exec = QueryProcess.create(graph);

        RuleEngine re = RuleEngine.create(graph);

        String rule = "prefix e: <htp://example.org/>"
                + "construct {[a e:Parent; e:term(?x ?y)]}"
                + "where     {[a e:Father; e:term(?x ?y)]}";

        String rule2 = "prefix e: <htp://example.org/>" +
                "construct {[a e:Father;   e:term(?x ?y)]}"
                + "where     {[a e:Parent;   e:term(?x ?y)]}";

        try {
            re.defRule(rule);
            re.defRule(rule2);
        } catch (EngineException e1) {
            e1.printStackTrace();
        }

        String init = "prefix e: <htp://example.org/>" + "insert data {"
                + "[ a e:Father ; e:term(<John> <Jack>) ]"
                + "}";

        String query = "prefix e: <htp://example.org/>" + "select  * where {"
                + // "?x foaf:knows ?z " +
                "[a e:Parent; e:term(?x ?y)]"
                + "}";

        try {
            exec.query(init);
            // re.setDebug(true);
            re.process();
            Mappings map = exec.query(query);
            //// System.out.println(map);
            assertEquals("Result", 1, map.size());

        } catch (EngineException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testOWLRL() throws EngineException, IOException, LoadException {
        GraphStore gs = GraphStore.create();
        QueryProcess exec = QueryProcess.create(gs);
        Load ld = Load.create(gs);
        // ld.setLevel(Access.Level.USER);
        try {
            ld.parse(data + "template/owl/data/primer.owl");
            ld.parse(data + "owlrule/owlrllite.rul");
        } catch (LoadException ex) {
            System.out.println(ex);
            throw ex;
        }
        RuleEngine re = ld.getRuleEngine();
        // Date d1 = new Date();
        re.setProfile(RuleEngine.OWL_RL_FULL);
        re.process();

        String q = "prefix f: <http://example.com/owl/families/>"
                + "select * "
                + "where {"
                + "graph kg:rule {"
                + "?x ?p ?y "
                + "filter (isURI(?x) && strstarts(?x, f:) "
                + "    && isURI(?y) && strstarts(?y, f:))"
                + "}"
                + "filter not exists {graph ?g {?x ?p ?y } filter(?g != kg:rule)}"
                + "}"
                + "order by ?x ?p ?y";
        Mappings map = exec.query(q);
        assertEquals(103, map.size());

    }

    @Test
    public void testOWLRL2() throws EngineException, IOException {
        GraphStore gs = GraphStore.create();
        QueryProcess exec = QueryProcess.create(gs);
        Load ld = Load.create(gs);
        try {
            ld.parse(data + "template/owl/data/primer.owl");
            ld.parse(data + "owlrule/owlrllite.rul");
        } catch (LoadException ex) {
            System.out.println(ex);
        }
        RuleEngine re = ld.getRuleEngine();
        // Date d1 = new Date();
        // re.setProfile(re.OWL_RL);
        re.process();

        String q = "prefix f: <http://example.com/owl/families/>"
                + "select * "
                + "where {"
                + "graph kg:rule {"
                + "?x ?p ?y "
                + "filter (isURI(?x) && strstarts(?x, f:) "
                + "    && isURI(?y) && strstarts(?y, f:))"
                + "}"
                + "filter not exists {graph ?g {?x ?p ?y } filter(?g != kg:rule)}"
                + "}"
                + "order by ?x ?p ?y";

        Mappings map = exec.query(q);
        assertEquals(103, map.size());

    }

    @Test
    public void testOWLRL22() throws EngineException, IOException {
        GraphStore gs = GraphStore.create();
        QueryProcess exec = QueryProcess.create(gs);
        Load ld = Load.create(gs);
        try {
            ld.parse(data + "template/owl/data/primer.owl");
        } catch (LoadException ex) {
            System.out.println(ex);
        }
        RuleEngine re = RuleEngine.create(gs);
        re.setProfile(RuleEngine.OWL_RL);
        // Date d1 = new Date();
        re.process();

        String q = "prefix f: <http://example.com/owl/families/>"
                + "select * "
                + "where {"
                + "graph kg:rule {"
                + "?x ?p ?y "
                + "filter (isURI(?x) && strstarts(?x, f:) "
                + "    && isURI(?y) && strstarts(?y, f:))"
                + "}"
                + "filter not exists {graph ?g {?x ?p ?y } filter(?g != kg:rule)}"
                + "}"
                + "order by ?x ?p ?y";

        Mappings map = exec.query(q);
        // assertEquals(114, map.size());
        assertEquals(122, map.size());

    }

    public void testOWLRL3() throws LoadException, EngineException {
        GraphStore g = GraphStore.create();
        Load ld = Load.create(g);
        ld.parse(data + "template/owl/data/primer.owl");
        RuleEngine re = RuleEngine.create(g);
        re.setProfile(RuleEngine.OWL_RL_LITE);
        re.process();

        String q = "select * "
                + "from kg:rule "
                + "where { ?x ?p ?y }";

        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(q);

        assertEquals(611, map.size());

        String qq = "select distinct ?p ?pr "
                + "from kg:rule "
                + "where { ?x ?p ?y bind (kg:provenance(?p) as ?pr) }";

        map = exec.query(qq);

        // assertEquals(31, map.size());

        String qqq = "select distinct ?q  "
                + "from kg:rule "
                + "where { "
                + "?x ?p ?y bind (kg:provenance(?p) as ?pr) "
                + "graph ?pr { [] sp:predicate ?q }"
                + "} order by ?q";

        map = exec.query(qqq);

        // assertEquals(19, map.size());

        String q4 = "select ?q  "
                + "where { "
                + "graph eng:engine { ?q a sp:Construct }"
                + "} ";

        map = exec.query(q4);

        assertEquals(64, map.size());

        String q5 = "select ?q  "
                + "where { "
                + "graph eng:record { ?r a kg:Index }"
                + "} ";

        map = exec.query(q5);

        assertEquals(159, map.size());

        String q6 = "select ?r  "
                + "where { "
                + "graph kg:re2 {  ?r a kg:Index  }"
                + "} ";

        map = exec.query(q6);
        assertEquals(3, map.size());

        String q7 = "select ?r  "
                + "where { "
                + "graph eng:queries {  ?r a sp:Construct  }"
                + "} ";

        map = exec.query(q7);
        assertEquals(4, map.size());
    }

    @Test
    public void testOWLRL4() throws LoadException, EngineException {
        GraphStore g = GraphStore.create();
        Load ld = Load.create(g);
        ld.parse(data + "template/owl/data/primer.owl");
        RuleEngine re = RuleEngine.create(g);
        re.setProfile(RuleEngine.OWL_RL_LITE);
        // re.process();
        g.addEngine(re);
        String q = "select * "
                + "from kg:rule "
                + "where { ?x ?p ?y }";
        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(q);

        assertEquals(597, map.size());
    }

    @Test

    public void testRuleOptimization() throws LoadException, EngineException {
        Graph g1 = testRuleOpt();
        Graph g2 = testRuleNotOpt();

        QueryProcess e1 = QueryProcess.create(g1, true);
        QueryProcess e2 = QueryProcess.create(g2, true);

        String q = "prefix c: <http://www.inria.fr/acacia/comma#>"
                + "select distinct ?x where {"
                + "?x a c:Person ; "
                + " c:hasCreated ?doc "
                + "?doc a c:Document"
                + "}";

        Mappings m1 = e1.query(q);
        Mappings m2 = e2.query(q);
        assertEquals(m1.size(), m2.size());
    }

    public Graph testRuleOpt() throws LoadException, EngineException {
        RuleEngine re = testRules();
        Graph g = re.getRDFGraph();

        re.setSpeedUp(true);
        System.out.println("Graph: " + g.size());
        Date d1 = new Date();
        re.process();
        Date d2 = new Date();
        System.out.println("** Time opt: " + (d2.getTime() - d1.getTime()) / (1000.0));
        validate(g, 37735);

        assertEquals(54028, g.size());
        return g;
    }

    public Graph testRuleNotOpt() throws LoadException, EngineException {
        RuleEngine re = testRules();
        Graph g = re.getRDFGraph();

        System.out.println("Graph: " + g.size());
        Date d1 = new Date();
        re.process();
        Date d2 = new Date();
        System.out.println("** Time std: " + (d2.getTime() - d1.getTime()) / (1000.0));

        validate(g, 41109);
        assertEquals(57402, g.size());
        return g;

    }

    RuleEngine testRules() throws LoadException {
        Graph g = createGraph();
        Load ld = Load.create(g);
        ld.parse(data + "comma/comma.rdfs");
        ld.parseDir(data + "comma/data");
        ld.parseDir(data + "comma/data2");
        try {
            ld.parse(data + "owlrule/owlrllite-junit.rul");
        } catch (LoadException e) {
            e.printStackTrace();
        }
        RuleEngine re = ld.getRuleEngine();
        return re;

    }

    void validate(Graph g, int n) throws EngineException {
        QueryProcess exec = QueryProcess.create(g);
        String q = "select * "
                + "from kg:rule "
                + "where {?x ?p ?y}";

        Mappings map = exec.query(q);
        assertEquals(n, map.size());
    }

    // //@Test
    // public void test1(){
    //
    // String query =
    // "prefix c: <http://www.inria.fr/acacia/comma#>" +
    // "select ?x ?y where { " +
    // "?y c:hasSister ?z" +
    // "?x c:hasBrother ?y " +
    // "}";
    //
    // LBind bind = rengine.SPARQLProve(query);
    // assertEquals("Result", 13, bind.size());
    // }
    //
    //
    // //@Test
    // public void test2(){
    //
    // String query =
    // "prefix c: <http://www.inria.fr/acacia/comma#>" +
    // "select * where {" +
    // "?x c:hasGrandParent c:Pierre " +
    // "}";
    //
    // LBind bind = rengine.SPARQLProve(query);
    // assertEquals("Result", 4, bind.size());
    // }
    //
    //
    // //@Test
    // public void test3(){
    //
    // String query =
    // "prefix c: <http://www.inria.fr/acacia/comma#>" +
    // "select * where {" +
    // "?x c:hasGrandParent c:Pierre ?x c:hasID ?id " +
    // "}";
    //
    // LBind bind = rengine.SPARQLProve(query);
    // assertEquals("Result", 0, bind.size());
    // }

    @Test
    public void test4() throws EngineException {

        String query = "prefix c: <http://www.inria.fr/acacia/comma#>" +
                "select     * where {" +
                "?x c:hasGrandParent c:Pierre " +
                "}";

        fengine.process();
        QueryProcess exec = QueryProcess.create(graph);
        Mappings map;
        try {
            map = exec.query(query);
            assertEquals("Result", 4, map.size());
        } catch (EngineException e) {
            assertEquals("Result", 4, e);
        }
    }

    @Test
    public void test44() throws EngineException {

        String query = "prefix c: <http://www.inria.fr/acacia/comma#>" +
                "select     * where {" +
                "?x c:hasGrandParent c:Pierre " +
                "}";

        String ent = "select * where {graph kg:entailment {?x ?p ?y}}";

        graph.process(fengine);
        QueryProcess exec = QueryProcess.create(graph);
        Mappings map;
        try {
            map = exec.query(query);
            assertEquals("Result", 4, map.size());

            map = exec.query(ent);
            // System.out.println(map);

        } catch (EngineException e) {
            assertEquals("Result", 4, e);
        }
    }

    @Test
    public void test8() throws EngineException {
        Graph g = createGraph();
        // QueryProcess exec = QueryProcess.create(g);

        QueryEngine qe = QueryEngine.create(g);
        String query = "insert data { <John> rdfs:label 'John' }";
        qe.addQuery(query);

        qe.process();

        assertEquals("Result", 1, g.size());
    }

    @Test
    public void testWF() {

        String query = "prefix c: <http://www.inria.fr/acacia/comma#>" +
                "select     * where {" +
                "?x c:hasGrandParent c:Pierre " +
                "}";

        String ent = "select * where {graph kg:entailment {?x ?p ?y}}";

        graph.addEngine(fengine);

        QueryProcess exec = QueryProcess.create(graph);
        Mappings map;
        try {
            map = exec.query(query);
            assertEquals("Result", 4, map.size());

            map = exec.query(ent);
            // System.out.println(map);

        } catch (EngineException e) {
            assertEquals("Result", 4, e);
        }
    }

}
