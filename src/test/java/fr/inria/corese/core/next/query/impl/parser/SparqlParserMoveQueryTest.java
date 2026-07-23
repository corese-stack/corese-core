package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.MoveRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserMoveQueryTest extends AbstractSparqlParserFeatureTest {

    private UpdateRequestUnitAst parseUnit(String query) {
        QueryAst ast = newParserDefault().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return update.operations().getFirst();
    }

    @Test
    @DisplayName("MOVE GRAPH <s> TO GRAPH <d>: source and destination named graphs, not silent")
    void moveGraphToGraph() {
        MoveRequestAst move = assertInstanceOf(MoveRequestAst.class,
                parseUnit("MOVE GRAPH <http://example.org/s> TO GRAPH <http://example.org/d>"));
        assertFalse(move.silent());
        assertEquals("<http://example.org/s>", move.source().graph().raw());
        assertEquals("<http://example.org/d>", move.destination().graph().raw());
    }

    @Test
    @DisplayName("MOVE accepts the SPARQL shorthand graph IRI form without GRAPH")
    void moveAcceptsGraphIriShorthand() {
        MoveRequestAst move = assertInstanceOf(MoveRequestAst.class,
                parseUnit("MOVE <http://example.org/s> TO <http://example.org/d>"));
        assertEquals("<http://example.org/s>", move.source().graph().raw());
        assertEquals("<http://example.org/d>", move.destination().graph().raw());
    }

    @Test
    @DisplayName("MOVE SILENT DEFAULT TO GRAPH <d>: silent, source is DEFAULT")
    void moveSilentDefaultToGraph() {
        MoveRequestAst move = assertInstanceOf(MoveRequestAst.class,
                parseUnit("MOVE SILENT DEFAULT TO GRAPH <http://example.org/d>"));
        assertTrue(move.silent());
        assertTrue(move.source().defaultGraph());
        assertEquals("<http://example.org/d>", move.destination().graph().raw());
    }

    @Test
    @DisplayName("MOVE GRAPH <s> TO DEFAULT: destination is DEFAULT")
    void moveGraphToDefault() {
        MoveRequestAst move = assertInstanceOf(MoveRequestAst.class,
                parseUnit("MOVE GRAPH <http://example.org/s> TO DEFAULT"));
        assertFalse(move.silent());
        assertEquals("<http://example.org/s>", move.source().graph().raw());
        assertTrue(move.destination().defaultGraph());
    }

    @Test
    @DisplayName("MOVE DEFAULT TO DEFAULT: both source and destination are DEFAULT")
    void moveDefaultToDefault() {
        MoveRequestAst move = assertInstanceOf(MoveRequestAst.class,
                parseUnit("MOVE DEFAULT TO DEFAULT"));
        assertFalse(move.silent());
        assertTrue(move.source().defaultGraph());
        assertTrue(move.destination().defaultGraph());
    }

    @Test
    @DisplayName("MOVE SILENT DEFAULT TO DEFAULT: silent with both DEFAULT")
    void moveSilentDefaultToDefault() {
        MoveRequestAst move = assertInstanceOf(MoveRequestAst.class,
                parseUnit("MOVE SILENT DEFAULT TO DEFAULT"));
        assertTrue(move.silent());
        assertTrue(move.source().defaultGraph());
        assertTrue(move.destination().defaultGraph());
    }

    @Test
    @DisplayName("chained MOVE operations separated by ';' produce two update operations")
    void chainedMoveProducesTwoOperations() {
        QueryAst ast = newParserDefault().parse(
                "MOVE GRAPH <http://example.org/s> TO GRAPH <http://example.org/d> ; " +
                "MOVE SILENT DEFAULT TO GRAPH <http://example.org/d2>");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(2, update.operations().size());

        MoveRequestAst first = assertInstanceOf(MoveRequestAst.class, update.operations().getFirst());
        assertEquals("<http://example.org/s>", first.source().graph().raw());
        assertEquals("<http://example.org/d>", first.destination().graph().raw());
        assertFalse(first.silent());

        MoveRequestAst second = assertInstanceOf(MoveRequestAst.class, update.operations().get(1));
        assertTrue(second.source().defaultGraph());
        assertEquals("<http://example.org/d2>", second.destination().graph().raw());
        assertTrue(second.silent());
    }

    @Test
    @DisplayName("PREFIX declaration is preserved in the prologue of a MOVE query")
    void moveMustKeepItsPrologue() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX ex: <http://example.org/> MOVE GRAPH ex:s TO GRAPH ex:d");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size());
        assertInstanceOf(MoveRequestAst.class, update.operations().getFirst());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("MOVE without TO keyword should throw a syntax exception")
    void moveMissingToShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("MOVE GRAPH <http://example.org/s>"));
    }

    @Test
    @DisplayName("MOVE without source should throw a syntax exception")
    void moveMissingSourceShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("MOVE TO GRAPH <http://example.org/d>"));
    }
}
