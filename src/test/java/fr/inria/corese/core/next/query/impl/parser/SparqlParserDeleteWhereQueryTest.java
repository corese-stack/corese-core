package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserDeleteWhereQueryTest extends AbstractSparqlParserFeatureTest {

    private DeleteWhereRequestAst parseUnit(String query) {
        QueryAst ast = newParserDefault().parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(1, update.operations().size(), "one update operation");
        return assertInstanceOf(DeleteWhereRequestAst.class, update.operations().getFirst());
    }

    @Test
    @DisplayName("DELETE WHERE { ?s p o }: pattern with variable subject")
    void deleteWhereVariableSubject() {
        DeleteWhereRequestAst delete = parseUnit(
                "DELETE WHERE { ?s <http://example.org/temp> <http://example.org/o> . }");
        assertEquals(1, delete.pattern().defaultTriples().size());
        assertTrue(delete.pattern().namedGraphBlocks().isEmpty());
        TriplePatternAst triple = delete.pattern().defaultTriples().getFirst();
        assertEquals("s", ((VarAst) triple.subject()).name());
    }

    @Test
    @DisplayName("DELETE WHERE { ?s ?p ?o }: pattern with all variables")
    void deleteWhereAllVariables() {
        DeleteWhereRequestAst delete = parseUnit(
                "DELETE WHERE { ?s ?p ?o . }");
        assertEquals(1, delete.pattern().defaultTriples().size());
        TriplePatternAst triple = delete.pattern().defaultTriples().getFirst();
        assertEquals("s", ((VarAst) triple.subject()).name());
        assertEquals("o", ((VarAst) triple.object()).name());
    }

    @Test
    @DisplayName("DELETE WHERE { s p o }: ground triple pattern")
    void deleteWhereGroundTriple() {
        DeleteWhereRequestAst delete = parseUnit(
                "DELETE WHERE { <http://example.org/a> <http://example.org/p> <http://example.org/o> . }");
        assertEquals(1, delete.pattern().defaultTriples().size());
        assertTrue(delete.pattern().namedGraphBlocks().isEmpty());
    }

    @Test
    @DisplayName("DELETE WHERE { GRAPH <g> { ?s ?p ?o } }: pattern in named graph")
    void deleteWhereNamedGraph() {
        DeleteWhereRequestAst delete = parseUnit(
                "DELETE WHERE { GRAPH <http://example.org/g> { ?s ?p ?o . } }");
        assertTrue(delete.pattern().defaultTriples().isEmpty());
        assertEquals(1, delete.pattern().namedGraphBlocks().size());
        NamedGraphQuadsAst block = delete.pattern().namedGraphBlocks().getFirst();
        assertEquals("<http://example.org/g>", ((IriAst) block.graph()).raw());
        assertEquals(1, block.triples().size());
    }

    @Test
    @DisplayName("DELETE WHERE with PREFIX: prologue is preserved")
    void deleteWhereMustKeepItsPrologue() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX ex: <http://example.org/> DELETE WHERE { ?s ex:temp ?o . }");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertInstanceOf(DeleteWhereRequestAst.class, update.operations().getFirst());
        assertNotNull(update.prologue());
        assertEquals(1, update.prologue().prefixDeclarations().size());
    }

    @Test
    @DisplayName("chained DELETE WHERE operations separated by ';' produce two update operations")
    void chainedDeleteWhereProducesTwoOperations() {
        QueryAst ast = newParserDefault().parse(
                "DELETE WHERE { ?s <http://example.org/p1> ?o . } ; " +
                "DELETE WHERE { ?s <http://example.org/p2> ?o . }");
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, ast);
        assertEquals(2, update.operations().size());
        assertInstanceOf(DeleteWhereRequestAst.class, update.operations().getFirst());
        assertInstanceOf(DeleteWhereRequestAst.class, update.operations().get(1));
    }

    @Test
    @DisplayName("DELETE WHERE without braces should throw a syntax exception")
    void deleteWhereMissingBracesShouldFail() {
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse("DELETE WHERE ?s ?p ?o"));
    }
}
