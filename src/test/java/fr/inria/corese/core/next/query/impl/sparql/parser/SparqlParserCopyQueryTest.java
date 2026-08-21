package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.CopyRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserCopyQueryTest extends AbstractSparqlParserFeatureTest {

    private UpdateRequestUnitAst parseUnit(String query) {
        QueryAst ast = newParserDefault().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return update.operations().getFirst();
    }

    @Test
    @DisplayName("COPY GRAPH <s> TO GRAPH <d>: source and destination named graphs, not silent")
    void copyGraphToGraph() {
        CopyRequestAst copy = assertInstanceOf(CopyRequestAst.class,
                parseUnit("COPY GRAPH <http://example.org/s> TO GRAPH <http://example.org/d>"));
        assertFalse(copy.silent());
        assertEquals("<http://example.org/s>", copy.source().graph().raw());
        assertEquals("<http://example.org/d>", copy.destination().graph().raw());
    }

    @Test
    @DisplayName("COPY SILENT DEFAULT TO GRAPH <d>: silent, source is DEFAULT")
    void copySilentDefaultToGraph() {
        CopyRequestAst copy = assertInstanceOf(CopyRequestAst.class,
                parseUnit("COPY SILENT DEFAULT TO GRAPH <http://example.org/d>"));
        assertTrue(copy.silent());
        assertTrue(copy.source().defaultGraph());
        assertEquals("<http://example.org/d>", copy.destination().graph().raw());
    }

    @Test
    @DisplayName("COPY GRAPH <s> TO DEFAULT: destination is DEFAULT")
    void copyGraphToDefault() {
        CopyRequestAst copy = assertInstanceOf(CopyRequestAst.class,
                parseUnit("COPY GRAPH <http://example.org/s> TO DEFAULT"));
        assertFalse(copy.silent());
        assertEquals("<http://example.org/s>", copy.source().graph().raw());
        assertTrue(copy.destination().defaultGraph());
    }

    @Test
    @DisplayName("COPY DEFAULT TO DEFAULT: both source and destination are DEFAULT")
    void copyDefaultToDefault() {
        CopyRequestAst copy = assertInstanceOf(CopyRequestAst.class,
                parseUnit("COPY DEFAULT TO DEFAULT"));
        assertFalse(copy.silent());
        assertTrue(copy.source().defaultGraph());
        assertTrue(copy.destination().defaultGraph());
    }

    @Test
    @DisplayName("COPY SILENT DEFAULT TO DEFAULT: silent with both DEFAULT")
    void copySilentDefaultToDefault() {
        CopyRequestAst copy = assertInstanceOf(CopyRequestAst.class,
                parseUnit("COPY SILENT DEFAULT TO DEFAULT"));
        assertTrue(copy.silent());
        assertTrue(copy.source().defaultGraph());
        assertTrue(copy.destination().defaultGraph());
    }

    @Test
    @DisplayName("chained COPY operations separated by ';' produce two update operations")
    void chainedCopyProducesTwoOperations() {
        QueryAst ast = newParserDefault().parse(
                "COPY GRAPH <http://example.org/s> TO GRAPH <http://example.org/d> ; " +
                "COPY SILENT DEFAULT TO GRAPH <http://example.org/d2>");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(2, update.operations().size());

        CopyRequestAst first = assertInstanceOf(CopyRequestAst.class, update.operations().get(0));
        assertEquals("<http://example.org/s>", first.source().graph().raw());
        assertEquals("<http://example.org/d>", first.destination().graph().raw());
        assertFalse(first.silent());

        CopyRequestAst second = assertInstanceOf(CopyRequestAst.class, update.operations().get(1));
        assertTrue(second.source().defaultGraph());
        assertEquals("<http://example.org/d2>", second.destination().graph().raw());
        assertTrue(second.silent());
    }

    @Test
    @DisplayName("PREFIX declaration is preserved in the prologue of a COPY query")
    void copyMustKeepItsPrologue() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX ex: <http://example.org/> COPY GRAPH ex:s TO GRAPH ex:d");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size());
        assertInstanceOf(CopyRequestAst.class, update.operations().getFirst());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("COPY without TO keyword should throw a syntax exception")
    void copyMissingToShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("COPY GRAPH <http://example.org/s>"));
    }

    @Test
    @DisplayName("COPY without source should throw a syntax exception")
    void copyMissingSourceShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("COPY TO GRAPH <http://example.org/d>"));
    }
}
