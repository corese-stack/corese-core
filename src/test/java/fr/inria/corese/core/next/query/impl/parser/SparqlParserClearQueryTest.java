package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.io.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.ClearRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserClearQueryTest extends AbstractSparqlParserFeatureTest {
    @Test
    public void graphQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        IriAst graphIri = loadQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void graphSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertNotNull(loadQueryAst.graphRef().graph());
        IriAst graphIri = loadQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void namedQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR NAMED
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertTrue(loadQueryAst.graphRef().named());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void namedSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT NAMED
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertTrue(loadQueryAst.graphRef().named());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void defaultQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR DEFAULT
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertTrue(loadQueryAst.graphRef().defaultGraph());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void defaultSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT DEFAULT
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertTrue(loadQueryAst.graphRef().defaultGraph());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void allQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR ALL
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertTrue(loadQueryAst.graphRef().all());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void allSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT ALL
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(ClearRequestAst.class, updateRequestAst.operations().getFirst());
        ClearRequestAst loadQueryAst = (ClearRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(loadQueryAst.graphRef());
        assertTrue(loadQueryAst.graphRef().all());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void failingTooManyQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CLEAR SILENT ALL NAMED GRAPH <http://ns.inria.fr/test>
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }
}
