package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.io.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SparqlParserLoadQueryTest extends AbstractSparqlParserFeatureTest{

    @Test
    public void simpleQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertEquals(1, loadQueryAst.fromClause().size());
        IriAst graphIri = (IriAst) loadQueryAst.fromClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertEquals(0, loadQueryAst.toClause().size());
        assertEquals(false, loadQueryAst.silent());
    }

    @Test
    public void simpleSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD SILENT <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertEquals(1, loadQueryAst.fromClause().size());
        IriAst graphIri = (IriAst) loadQueryAst.fromClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertEquals(0, loadQueryAst.toClause().size());
        assertEquals(true, loadQueryAst.silent());
    }

    @Test
    public void fullQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertEquals(1, loadQueryAst.fromClause().size());
        IriAst graphIri = (IriAst) loadQueryAst.fromClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertEquals(1, loadQueryAst.toClause().size());
        IriAst targetGraphIri = (IriAst) loadQueryAst.toClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/otherGraph>", targetGraphIri.raw());
        assertEquals(false, loadQueryAst.silent());
    }

    @Test
    public void fullSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertEquals(1, loadQueryAst.fromClause().size());
        IriAst graphIri = (IriAst) loadQueryAst.fromClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertEquals(1, loadQueryAst.toClause().size());
        IriAst targetGraphIri = (IriAst) loadQueryAst.toClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/otherGraph>", targetGraphIri.raw());
        assertEquals(true, loadQueryAst.silent());
    }
}
