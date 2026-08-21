package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserDeleteDataQueryTest extends AbstractSparqlParserFeatureTest {

    private DeleteDataRequestAst parseUnit(String query) {
        QueryAst ast = newParserDefault().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return assertInstanceOf(DeleteDataRequestAst.class, update.operations().getFirst());
    }

    @Test
    @DisplayName("DELETE DATA { s p o }: single triple in default graph")
    void deleteDataSingleTriple() {
        DeleteDataRequestAst delete = parseUnit(
                "DELETE DATA { <http://example.org/a> <http://example.org/knows> <http://example.org/b> . }");
        assertEquals(1, delete.data().defaultTriples().size());
        assertTrue(delete.data().namedGraphBlocks().isEmpty());
        TriplePatternAst triple = delete.data().defaultTriples().getFirst();
        assertEquals("<http://example.org/a>", ((IriAst) triple.subject()).raw());
        assertEquals("<http://example.org/b>", ((IriAst) triple.object()).raw());
    }

    @Test
    @DisplayName("DELETE DATA { s p o1 , o2 }: multiple objects")
    void deleteDataMultipleObjects() {
        DeleteDataRequestAst delete = parseUnit(
                "DELETE DATA { <http://example.org/a> <http://example.org/p> <http://example.org/o1>, <http://example.org/o2> . }");
        assertEquals(2, delete.data().defaultTriples().size());
    }

    @Test
    @DisplayName("DELETE DATA with PREFIX: prefixed IRI is resolved")
    void deleteDataWithPrefix() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX ex: <http://example.org/> DELETE DATA { ex:a ex:knows ex:b . }");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        DeleteDataRequestAst delete = assertInstanceOf(DeleteDataRequestAst.class, update.operations().getFirst());
        assertEquals(1, delete.data().defaultTriples().size());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("DELETE DATA { GRAPH <g> { s p o } }: triple in named graph")
    void deleteDataNamedGraph() {
        DeleteDataRequestAst delete = parseUnit(
                "DELETE DATA { GRAPH <http://example.org/g> { <http://example.org/a> <http://example.org/p> <http://example.org/o> . } }");
        assertTrue(delete.data().defaultTriples().isEmpty());
        assertEquals(1, delete.data().namedGraphBlocks().size());
        NamedGraphQuadsAst block = delete.data().namedGraphBlocks().getFirst();
        assertEquals("<http://example.org/g>", ((IriAst) block.graph()).raw());
        assertEquals(1, block.triples().size());
    }

    @Test
    @DisplayName("chained DELETE DATA operations separated by ';' produce two update operations")
    void chainedDeleteDataProducesTwoOperations() {
        QueryAst ast = newParserDefault().parse(
                "DELETE DATA { <http://example.org/a> <http://example.org/p> <http://example.org/o1> . } ; " +
                "DELETE DATA { <http://example.org/b> <http://example.org/p> <http://example.org/o2> . }");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(2, update.operations().size());
        assertInstanceOf(DeleteDataRequestAst.class, update.operations().getFirst());
        assertInstanceOf(DeleteDataRequestAst.class, update.operations().get(1));
    }

    @Test
    @DisplayName("DELETE DATA without braces should throw a syntax exception")
    void deleteDataMissingBracesShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("DELETE DATA <http://example.org/a> <http://example.org/p> <http://example.org/o>"));
    }
}
