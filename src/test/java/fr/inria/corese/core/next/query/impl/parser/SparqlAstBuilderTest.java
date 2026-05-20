package fr.inria.corese.core.next.query.impl.parser;


import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.parser.AbstractSparqlParserFeatureTest.expectedRdfTypeIriAst;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SparqlAstBuilder (no ANTLR, no parser).
 */
class SparqlAstBuilderTest {

    private SparqlAstBuilder newBuilder() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .failFast(true)
                .collectErrors(true)
                .build();
        return new SparqlAstBuilder(opts);
    }

    // ---------- Happy paths ----------

    @Test
    void shouldBuildEmptyWhereGroupWhenNoTriples() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();
        b.exitGroup();
        b.exitSelectQuery();

        QueryAst ast = b.getResult();
        assertNotNull(ast);
        assertNotNull(ast.whereClause());
        assertEquals(0, ast.whereClause().patterns().size());
    }

    @Test
    void shouldBuildSingleBgpWithOneTriple() {
        SparqlAstBuilder b = newBuilder();

        b.setBaseUri(IOConstants.getDefaultBaseURI());
        b.enterSelectQuery();
        b.enterGroup();
        b.enterBgp();
        b.addTriple(new VarAst("s"), new VarAst("p"), new VarAst("o"));
        b.exitBgp();
        b.exitGroup();
        b.exitSelectQuery();

        QueryAst actual = b.getResult();

        QueryAst expected = new SelectQueryAst(
                new GroupGraphPatternAst(List.of(
                        new BgpAst(List.of(
                                new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))
                        ))
                ))
        );

        assertEquals(expected, actual);
    }

    @Test
    void shouldBuildSingleBgpWithMultipleTriples() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();
        b.enterBgp();
        b.addTriple(new VarAst("s"), b.iri("a"), b.iri("foaf:Person"));
        b.addTriple(new VarAst("s"), b.iri("foaf:name"), new VarAst("n"));
        b.exitBgp();
        b.exitGroup();
        b.exitSelectQuery();

        QueryAst ast = b.getResult();
        BgpAst bgp = singleBgp(ast);

        assertEquals(2, bgp.triples().size());
        assertEquals(new TriplePatternAst(new VarAst("s"), expectedRdfTypeIriAst(), new IriAst("foaf:Person")), bgp.triples().get(0));
        assertEquals(new TriplePatternAst(new VarAst("s"), new IriAst("foaf:name"), new VarAst("n")), bgp.triples().get(1));
    }

    @Test
    void shouldWrapMinusGroupAsMinusAst() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();

        b.enterBgp();
        b.addTriple(new VarAst("s"), new VarAst("p"), new VarAst("o"));
        b.exitBgp();

        b.enterMinus();
        b.enterGroup();
        b.enterBgp();
        b.addTriple(new VarAst("s"), new VarAst("q"), new VarAst("hidden"));
        b.exitBgp();
        b.exitGroup();
        b.exitMinus();

        b.exitGroup();
        b.exitSelectQuery();

        SelectQueryAst select = (SelectQueryAst) b.getResult();
        assertNotNull(select);

        GroupGraphPatternAst where = select.whereClause();
        assertEquals(2, where.patterns().size());
        assertInstanceOf(BgpAst.class, where.patterns().getFirst());

        MinusAst minus = assertInstanceOf(MinusAst.class, where.patterns().get(1));
        assertEquals(1, minus.pattern().patterns().size());
        assertInstanceOf(BgpAst.class, minus.pattern().patterns().getFirst());
    }

    @Test
    void shouldAllowMultipleBgpsInSameGroup() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();

        b.enterBgp();
        b.addTriple(new VarAst("s"), new VarAst("p"), new VarAst("o"));
        b.exitBgp();

        b.enterBgp();
        b.addTriple(new VarAst("x"), new VarAst("y"), new VarAst("z"));
        b.exitBgp();

        b.exitGroup();
        b.exitSelectQuery();

        QueryAst ast = b.getResult();
        GroupGraphPatternAst where = ast.whereClause();

        assertEquals(2, where.patterns().size());
        assertInstanceOf(BgpAst.class, where.patterns().get(0));
        assertInstanceOf(BgpAst.class, where.patterns().get(1));

        BgpAst bgp1 = (BgpAst) where.patterns().get(0);
        BgpAst bgp2 = (BgpAst) where.patterns().get(1);

        assertEquals(1, bgp1.triples().size());
        assertEquals(1, bgp2.triples().size());
    }

    @Test
    void shouldNotAddEmptyBgpToGroup() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();
        b.enterBgp();
        b.exitBgp(); // no triples
        b.exitGroup();
        b.exitSelectQuery();

        QueryAst ast = b.getResult();
        assertEquals(0, ast.whereClause().patterns().size(), "Empty TriplesBlock should not create a BGP pattern");
    }

    @Test
    void shouldBuildLiteralTerms() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();
        b.enterBgp();
        b.addTriple(
                new VarAst("s"),
                b.iri("<http://example/p>"),
                b.literal("\"salut\"", "fr", null)
        );
        b.addTriple(
                new VarAst("s"),
                b.iri("<http://example/age>"),
                b.literal("\"12\"", null, "xsd:integer")
        );
        b.exitBgp();
        b.exitGroup();
        b.exitSelectQuery();

        BgpAst bgp = singleBgp(b.getResult());
        assertEquals(2, bgp.triples().size());

        LiteralAst l1 = (LiteralAst) bgp.triples().get(0).object();
        assertEquals("\"salut\"", l1.lexical());
        assertEquals("fr", l1.lang());
        assertNull(l1.datatype());

        LiteralAst l2 = (LiteralAst) bgp.triples().get(1).object();
        assertEquals("\"12\"", l2.lexical());
        assertNull(l2.lang());
        assertEquals("xsd:integer", l2.datatype());
    }

    // ---------- Error cases (stack misuse) ----------

    @Test
    void addTripleOutsideBgpShouldThrow() {
        SparqlAstBuilder b = newBuilder();

        b.enterGroup();
        assertThrows(IllegalStateException.class, () ->
                b.addTriple(new VarAst("s"), new VarAst("p"), new VarAst("o"))
        );
    }

    @Test
    void exitGroupWithOpenBgpShouldThrow() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();
        b.enterBgp();
        b.addTriple(new VarAst("s"), new VarAst("p"), new VarAst("o"));

        assertThrows(IllegalStateException.class, b::exitGroup, "Should not exit group while BGP is open");

        b.exitBgp();
        b.exitGroup();
        b.exitSelectQuery();
    }

    @Test
    void exitBgpWithoutEnterBgpShouldThrow() {
        SparqlAstBuilder b = newBuilder();

        b.enterSelectQuery();
        b.enterGroup();
        assertThrows(Exception.class, b::exitBgp); // ArrayDeque pop throws NoSuchElementException
    }

    /**
     * SPARQL allows only SELECT as a subquery inside { ... }; ASK/CONSTRUCT/DESCRIBE are top-level only.
     * The ANTLR grammar enforces this; the builder rejects inconsistent listener order.
     */
    @Test
    void askConstructDescribeMustNotStartInsideGraphPattern() {
        SparqlAstBuilder b = newBuilder();
        b.enterSelectQuery();
        b.enterWhereClause();
        b.enterGroup();
        assertThrows(QuerySyntaxException.class, b::enterAskQuery);

        b = newBuilder();
        b.enterSelectQuery();
        b.enterWhereClause();
        b.enterGroup();
        assertThrows(QuerySyntaxException.class, b::enterConstructQuery);

        b = newBuilder();
        b.enterSelectQuery();
        b.enterWhereClause();
        b.enterGroup();
        assertThrows(QuerySyntaxException.class, b::enterDescribeQuery);
    }

    @Test
    void askConstructDescribeMustNotStartInsideSelectSubqueryFrame() {
        SparqlAstBuilder b = newBuilder();
        b.enterSelectQuery();
        b.enterWhereClause();
        b.enterGroup();
        b.enterSelectQuery();
        b.enterWhereClause();
        b.enterGroup();
        assertThrows(QuerySyntaxException.class, b::enterAskQuery);
    }

    // ---------- Defensive copy / immutability checks ----------

    @Test
    void bgpAstShouldDefensivelyCopyTriplesList() {
        List<TriplePatternAst> triples = new ArrayList<>();
        triples.add(new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")));

        BgpAst bgp = new BgpAst(triples);
        triples.clear();

        assertEquals(1, bgp.triples().size());
    }

    @Test
    void groupGraphPatternAstShouldDefensivelyCopyPatternsList() {
        List<PatternAst> patterns = new ArrayList<>();
        patterns.add(new BgpAst(List.of(new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")))));

        GroupGraphPatternAst group = new GroupGraphPatternAst(patterns);
        patterns.clear();

        assertEquals(1, group.patterns().size());
    }

    // ---------- Helpers ----------

    private static BgpAst singleBgp(QueryAst ast) {
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(1, where.patterns().size(), "Expected exactly 1 pattern in WHERE");
        assertInstanceOf(BgpAst.class, where.patterns().getFirst(), "Expected first pattern to be a BGP");
        return (BgpAst) where.patterns().getFirst();
    }
}
