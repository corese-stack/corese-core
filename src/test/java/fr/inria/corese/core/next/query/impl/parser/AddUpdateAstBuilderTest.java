package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.AddRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddUpdateAstBuilderTest extends AbstractSparqlParserFeatureTest {

    private UpdateRequestUnitAst parseUnit(String query) {
        QueryAst ast = newParserDefault().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return update.operations().getFirst();
    }

    @Test
    @DisplayName("ADD GRAPH <s> TO GRAPH <d>: source and destination named graphs, not silent")
    void addGraphToGraph() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD GRAPH <http://example.org/s> TO GRAPH <http://example.org/d>"));
        assertFalse(add.silent());
        assertEquals("<http://example.org/s>", add.source().graph().raw());
        assertEquals("<http://example.org/d>", add.destination().graph().raw());
    }

    @Test
    @DisplayName("ADD accepts the SPARQL shorthand graph IRI form without GRAPH")
    void addAcceptsGraphIriShorthand() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD <http://example.org/s> TO <http://example.org/d>"));
        assertEquals("<http://example.org/s>", add.source().graph().raw());
        assertEquals("<http://example.org/d>", add.destination().graph().raw());
    }

    @Test
    @DisplayName("ADD SILENT DEFAULT TO GRAPH <d>: silent, source is DEFAULT")
    void addSilentDefaultToGraph() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD SILENT DEFAULT TO GRAPH <http://example.org/d>"));
        assertTrue(add.silent());
        assertTrue(add.source().defaultGraph());
        assertEquals("<http://example.org/d>", add.destination().graph().raw());
    }

    @Test
    @DisplayName("ADD GRAPH <s> TO DEFAULT: destination is DEFAULT")
    void addGraphToDefault() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD GRAPH <http://example.org/s> TO DEFAULT"));
        assertFalse(add.silent());
        assertEquals("<http://example.org/s>", add.source().graph().raw());
        assertTrue(add.destination().defaultGraph());
    }

    @Test
    @DisplayName("ADD DEFAULT TO DEFAULT: both source and destination are DEFAULT")
    void addDefaultToDefault() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD DEFAULT TO DEFAULT"));
        assertFalse(add.silent());
        assertTrue(add.source().defaultGraph());
        assertTrue(add.destination().defaultGraph());
    }

    @Test
    @DisplayName("ADD SILENT DEFAULT TO DEFAULT: silent with both DEFAULT")
    void addSilentDefaultToDefault() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD SILENT DEFAULT TO DEFAULT"));
        assertTrue(add.silent());
        assertTrue(add.source().defaultGraph());
        assertTrue(add.destination().defaultGraph());
    }

    @Test
    @DisplayName("chained ADD operations separated by ';' produce two update operations")
    void chainedAddProducesTwoOperations() {
        QueryAst ast = newParserDefault().parse(
                "ADD GRAPH <http://example.org/s> TO GRAPH <http://example.org/d> ; " +
                "ADD SILENT DEFAULT TO GRAPH <http://example.org/d2>");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(2, update.operations().size());

        AddRequestAst first = assertInstanceOf(AddRequestAst.class, update.operations().get(0));
        assertEquals("<http://example.org/s>", first.source().graph().raw());
        assertEquals("<http://example.org/d>", first.destination().graph().raw());
        assertFalse(first.silent());

        AddRequestAst second = assertInstanceOf(AddRequestAst.class, update.operations().get(1));
        assertTrue(second.source().defaultGraph());
        assertEquals("<http://example.org/d2>", second.destination().graph().raw());
        assertTrue(second.silent());
    }

    @Test
    @DisplayName("PREFIX declaration is preserved in the prologue of an ADD query")
    void addMustKeepItsPrologue() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX ex: <http://example.org/> ADD GRAPH ex:s TO GRAPH ex:d");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size());
        assertInstanceOf(AddRequestAst.class, update.operations().getFirst());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("ADD without TO keyword should throw a syntax exception")
    void addMissingToShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("ADD GRAPH <http://example.org/s>"));
    }

    @Test
    @DisplayName("ADD without source should throw a syntax exception")
    void addMissingSourceShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("ADD TO GRAPH <http://example.org/d>"));
    }
}
