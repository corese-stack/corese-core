package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.ClearRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserClearQueryTest extends AbstractSparqlParserFeatureTest {
    @Test
    void graphQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        IriAst graphIri = clearQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertFalse(clearQueryAst.silent());
    }

    @Test
    void graphSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertNotNull(clearQueryAst.graphRef().graph());
        IriAst graphIri = clearQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertTrue(clearQueryAst.silent());
    }

    @Test
    void namedQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR NAMED
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertTrue(clearQueryAst.graphRef().named());
        assertFalse(clearQueryAst.silent());
    }

    @Test
    void namedSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT NAMED
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertTrue(clearQueryAst.graphRef().named());
        assertTrue(clearQueryAst.silent());
    }

    @Test
    void defaultQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR DEFAULT
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertTrue(clearQueryAst.graphRef().defaultGraph());
        assertFalse(clearQueryAst.silent());
    }

    @Test
    void defaultSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT DEFAULT
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertTrue(clearQueryAst.graphRef().defaultGraph());
        assertTrue(clearQueryAst.silent());
    }

    @Test
    void allQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR ALL
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertTrue(clearQueryAst.graphRef().all());
        assertFalse(clearQueryAst.silent());
    }

    @Test
    void allSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT ALL
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst clearQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(clearQueryAst.graphRef());
        assertTrue(clearQueryAst.graphRef().all());
        assertTrue(clearQueryAst.silent());
    }

    @Test
    void failingTooManyQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT ALL NAMED GRAPH <http://ns.inria.fr/test>
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }
}
