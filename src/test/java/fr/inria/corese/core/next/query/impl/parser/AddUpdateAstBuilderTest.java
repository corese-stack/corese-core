package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.AddRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddUpdateAstBuilderTest {

    private UpdateRequestUnitAst parseUnit(String query) {
        QueryAst ast = new SparqlParser().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return update.operations().get(0);
    }

    @Test
    @DisplayName("ADD GRAPH <s> TO GRAPH <d>: source and destination named graphs, not silent")
    void addGraphToGraph() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD GRAPH <http://example.org/s> TO GRAPH <http://example.org/d>"));
        assertFalse(add.silent());
        assertNotNull(add.source().graph());
        assertNotNull(add.destination().graph());
    }

    @Test
    @DisplayName("ADD SILENT DEFAULT TO GRAPH <d>: silent, source is DEFAULT")
    void addSilentDefaultToGraph() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD SILENT DEFAULT TO GRAPH <http://example.org/d>"));
        assertTrue(add.silent());
        assertTrue(add.source().defaultGraph());
        assertNotNull(add.destination().graph());
    }

    @Test
    @DisplayName("ADD GRAPH <s> TO DEFAULT: destination is DEFAULT")
    void addGraphToDefault() {
        AddRequestAst add = assertInstanceOf(AddRequestAst.class,
                parseUnit("ADD GRAPH <http://example.org/s> TO DEFAULT"));
        assertNotNull(add.source().graph());
        assertTrue(add.destination().defaultGraph());
    }
}