package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserInsertDataQueryTest extends AbstractSparqlParserFeatureTest {

    private InsertDataRequestAst parseUnit(String query) {
        QueryAst ast = newParserDefault().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return assertInstanceOf(InsertDataRequestAst.class, update.operations().getFirst());
    }

    @Test
    @DisplayName("INSERT DATA { s p o }: single triple in default graph")
    void insertDataSingleTriple() {
        InsertDataRequestAst insert = parseUnit(
                "INSERT DATA { <http://example.org/a> <http://example.org/knows> <http://example.org/b> . }");
        assertEquals(1, insert.data().defaultTriples().size());
        assertTrue(insert.data().namedGraphBlocks().isEmpty());
        TriplePatternAst triple = insert.data().defaultTriples().getFirst();
        assertEquals("<http://example.org/a>", ((IriAst) triple.subject()).raw());
        assertEquals("<http://example.org/b>", ((IriAst) triple.object()).raw());
    }

    @Test
    @DisplayName("INSERT DATA { s p o1 ; p o2 }: multiple objects via semicolon")
    void insertDataMultipleObjects() {
        InsertDataRequestAst insert = parseUnit(
                "INSERT DATA { <http://example.org/a> <http://example.org/p> <http://example.org/o1>, <http://example.org/o2> . }");
        assertEquals(2, insert.data().defaultTriples().size());
    }

    @Test
    @DisplayName("INSERT DATA with PREFIX: prefixed IRI is resolved")
    void insertDataWithPrefix() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX ex: <http://example.org/> INSERT DATA { ex:a ex:knows ex:b . }");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        InsertDataRequestAst insert = assertInstanceOf(InsertDataRequestAst.class, update.operations().getFirst());
        assertEquals(1, insert.data().defaultTriples().size());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("INSERT DATA { GRAPH <g> { s p o } }: triple in named graph")
    void insertDataNamedGraph() {
        InsertDataRequestAst insert = parseUnit(
                "INSERT DATA { GRAPH <http://example.org/g> { <http://example.org/a> <http://example.org/p> <http://example.org/o> . } }");
        assertTrue(insert.data().defaultTriples().isEmpty());
        assertEquals(1, insert.data().namedGraphBlocks().size());
        NamedGraphQuadsAst block = insert.data().namedGraphBlocks().getFirst();
        assertEquals("<http://example.org/g>", ((IriAst) block.graph()).raw());
        assertEquals(1, block.triples().size());
    }

    @Test
    @DisplayName("INSERT DATA with default and named graph triples")
    void insertDataDefaultAndNamedGraph() {
        InsertDataRequestAst insert = parseUnit(
                "INSERT DATA { " +
                "<http://example.org/a> <http://example.org/p> <http://example.org/o> . " +
                "GRAPH <http://example.org/g> { <http://example.org/x> <http://example.org/p> <http://example.org/y> . } " +
                "}");
        assertEquals(1, insert.data().defaultTriples().size());
        assertEquals(1, insert.data().namedGraphBlocks().size());
    }

    @Test
    @DisplayName("chained INSERT DATA operations separated by ';' produce two update operations")
    void chainedInsertDataProducesTwoOperations() {
        QueryAst ast = newParserDefault().parse(
                "INSERT DATA { <http://example.org/a> <http://example.org/p> <http://example.org/o1> . } ; " +
                "INSERT DATA { <http://example.org/b> <http://example.org/p> <http://example.org/o2> . }");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(2, update.operations().size());
        assertInstanceOf(InsertDataRequestAst.class, update.operations().getFirst());
        assertInstanceOf(InsertDataRequestAst.class, update.operations().get(1));
    }

    @Test
    @DisplayName("INSERT DATA without braces should throw a syntax exception")
    void insertDataMissingBracesShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("INSERT DATA <http://example.org/a> <http://example.org/p> <http://example.org/o>"));
    }
}
