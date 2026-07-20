package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.CreateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserCreateQueryTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("should parse a CREATE GRAPH <iri> query")
    void graphQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(CreateRequestAst.class, updateRequestAst.operations().getFirst());
        CreateRequestAst createQueryAst = (CreateRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(createQueryAst.graphRef());
        IriAst graphIri = createQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertFalse(createQueryAst.silent());
    }

    @Test
    @DisplayName("should parse a CREATE SILENT GRAPH <iri> query and set the silent flag")
    void graphSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE SILENT GRAPH <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(UpdateRequestAst.class, queryAst);
        UpdateRequestAst updateRequestAst = (UpdateRequestAst) queryAst;
        assertEquals(1, updateRequestAst.operations().size());
        assertInstanceOf(CreateRequestAst.class, updateRequestAst.operations().getFirst());
        CreateRequestAst createQueryAst = (CreateRequestAst) updateRequestAst.operations().getFirst();
        assertNotNull(createQueryAst.graphRef());
        assertNotNull(createQueryAst.graphRef().graph());
        IriAst graphIri = createQueryAst.graphRef().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertTrue(createQueryAst.silent());
    }

    @Test
    @DisplayName("should parse two chained CREATE operations separated by ';'")
    void chainedCreateShouldCreateTwoUpdateOperations() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE GRAPH <http://example.org/g1> ;
                CREATE SILENT GRAPH <http://example.org/g2>
                """;

        QueryAst queryAst = parser.parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, queryAst);
        assertEquals(2, update.operations().size());

        CreateRequestAst first = assertInstanceOf(CreateRequestAst.class, update.operations().getFirst());
        assertEquals("<http://example.org/g1>", first.graphRef().graph().raw());
        assertFalse(first.silent());

        CreateRequestAst second = assertInstanceOf(CreateRequestAst.class, update.operations().get(1));
        assertEquals("<http://example.org/g2>", second.graphRef().graph().raw());
        assertTrue(second.silent());
    }

    @Test
    @DisplayName("should keep the prologue when CREATE uses a prefixed graph name")
    void createMustKeepItsPrologue() {
        QueryParser parser = newParserDefault();
        QueryAst ast = parser.parse("PREFIX ex: <http://example.org/> CREATE GRAPH ex:g");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);

        assertEquals(1, update.operations().size());
        assertInstanceOf(CreateRequestAst.class, update.operations().getFirst());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("should reject CREATE DEFAULT (only GRAPH <iri> is allowed)")
    void failingDefaultTargetTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE DEFAULT
                """;

        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
    }

    @Test
    @DisplayName("should reject CREATE NAMED (only GRAPH <iri> is allowed)")
    void failingNamedTargetTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE NAMED
                """;

        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
    }

    @Test
    @DisplayName("should reject CREATE ALL (only GRAPH <iri> is allowed)")
    void failingAllTargetTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE ALL
                """;

        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
    }

    @Test
    @DisplayName("should reject CREATE without a graph reference")
    void failingMissingGraphTest() {
        QueryParser parser = newParserDefault();
        String query = """
                CREATE SILENT
                """;

        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
    }
}