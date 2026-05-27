package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.io.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserLoadQueryTest extends AbstractSparqlParserFeatureTest{

    @Test
    public void simpleQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(LoadRequestAst.class, updateRequestAst.operations().getFirst());
        LoadRequestAst loadQueryAst = (LoadRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.fromClause());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNull(loadQueryAst.toClause());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void simpleSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD SILENT <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(LoadRequestAst.class, updateRequestAst.operations().getFirst());
        LoadRequestAst loadQueryAst = (LoadRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.fromClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNull(loadQueryAst.toClause());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void fullQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(LoadRequestAst.class, updateRequestAst.operations().getFirst());
        LoadRequestAst loadQueryAst = (LoadRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.fromClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNotNull(loadQueryAst.toClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst targetGraphIri = loadQueryAst.toClause().graph();
        assertEquals("<http://ns.inria.fr/otherGraph>", targetGraphIri.raw());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void fullSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD SILENT <http://ns.inria.fr/test> INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(LoadRequestAst.class, updateRequestAst.operations().getFirst());
        LoadRequestAst loadQueryAst = (LoadRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.fromClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNotNull(loadQueryAst.toClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst targetGraphIri = loadQueryAst.toClause().graph();
        assertEquals("<http://ns.inria.fr/otherGraph>", targetGraphIri.raw());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void failingDefaultSourceQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD DEFAULT INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    private void failingSyntaxTest(String query) {
        QueryParser parser = newParserDefault();

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    @Test
    public void failingDefaultTargetQueryTest() {
        String query = """
                LOAD <http://ns.inria.fr/test> INTO DEFAULT
                """;
        failingSyntaxTest(query);
    }

    @Test
    public void failingNamedSourceQueryTest() {
        String query = """
                LOAD NAMED INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        failingSyntaxTest(query);
    }

    @Test
    public void failingNamedTargetQueryTest() {
        String query = """
                LOAD <http://ns.inria.fr/test> INTO NAMED
                """;

        failingSyntaxTest(query);
    }

    @Test
    public void failingAllSourceQueryTest() {
        String query = """
                LOAD ALL INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        failingSyntaxTest(query);
    }

    @Test
    public void failingAllTargetQueryTest() {
        String query = """
                LOAD <http://ns.inria.fr/test> INTO ALL
                """;

        failingSyntaxTest(query);
    }
}
