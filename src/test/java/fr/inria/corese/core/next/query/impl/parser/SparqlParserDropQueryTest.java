package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.DropRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserDropQueryTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("should parse DROP GRAPH <iri>")
    void graphQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        IriAst graphIri = dropQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertFalse(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP SILENT GRAPH <iri> and set the silent flag")
    void graphSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP SILENT GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        IriAst graphIri = dropQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertTrue(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP NAMED")
    void namedQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP NAMED
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        assertTrue(dropQueryAst.graphRef().named());
        assertFalse(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP SILENT NAMED and set the silent flag")
    void namedSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP SILENT NAMED
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        assertTrue(dropQueryAst.graphRef().named());
        assertTrue(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP DEFAULT")
    void defaultQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP DEFAULT
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        assertTrue(dropQueryAst.graphRef().defaultGraph());
        assertFalse(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP SILENT DEFAULT and set the silent flag")
    void defaultSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP SILENT DEFAULT
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        assertTrue(dropQueryAst.graphRef().defaultGraph());
        assertTrue(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP ALL")
    void allQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP ALL
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        assertTrue(dropQueryAst.graphRef().all());
        assertFalse(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse DROP SILENT ALL and set the silent flag")
    void allSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP SILENT ALL
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(DropRequestAst.class, updateRequestAst.operations().getFirst());
        DropRequestAst dropQueryAst = (DropRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(dropQueryAst.graphRef());
        assertTrue(dropQueryAst.graphRef().all());
        assertTrue(dropQueryAst.silent());
    }

    @Test
    @DisplayName("should parse two chained DROP operations separated by ';'")
    void chainedDropShouldCreateTwoUpdateOperations() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP GRAPH <http://example.org/g1> ;
                DROP SILENT GRAPH <http://example.org/g2>
                """;

        QueryAst queryAst = parser.parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, queryAst);
        assertEquals(2, update.operations().size());

        DropRequestAst first = assertInstanceOf(DropRequestAst.class, update.operations().getFirst());
        assertEquals("<http://example.org/g1>", first.graphRef().graph().raw());
        assertFalse(first.silent());

        DropRequestAst second = assertInstanceOf(DropRequestAst.class, update.operations().get(1));
        assertEquals("<http://example.org/g2>", second.graphRef().graph().raw());
        assertTrue(second.silent());
    }

    @Test
    @DisplayName("should keep the prologue when DROP uses a prefixed graph name")
    void dropMustKeepItsPrologue() {
        QueryParser parser = newParserDefault();
        QueryAst ast = parser.parse("PREFIX ex: <http://example.org/> DROP GRAPH ex:g");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);

        assertEquals(1, update.operations().size());
        assertInstanceOf(DropRequestAst.class, update.operations().getFirst());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("should reject invalid DROP syntax with multiple graph references")
    void failingTooManyQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                DROP SILENT ALL NAMED GRAPH <http://ns.inria.fr/test>
                """;

        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
    }
}
