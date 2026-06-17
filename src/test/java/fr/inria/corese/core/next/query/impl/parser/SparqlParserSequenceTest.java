package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.WhereClauseQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.AlternativePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.InversePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.NegatedPropertySetPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OneOrMorePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OptionalPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.SequencePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.ZeroOrMorePathAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SparqlParserSequenceTest extends AbstractSparqlParserFeatureTest {

    private static final String PREFIX = """
            PREFIX ex: <http://example.org/>
            """;

    @Test
    void shouldParseSequencePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p1/ex:p2 ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(SequencePathAst.class, triple.predicate());
    }

    @Test
    void shouldParseAlternativePath() {
        SparqlParser parser = newParserDefault();
        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p1|ex:p2 ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(AlternativePathAst.class, triple.predicate());
    }

    @Test
    void shouldParseZeroOrMorePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p* ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(ZeroOrMorePathAst.class, triple.predicate());
    }

    @Test
    void shouldParseOptionalPath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p? ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(OptionalPathAst.class, triple.predicate());
    }

    @Test
    void shouldParseOneOrMorePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p+ ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(OneOrMorePathAst.class, triple.predicate());
    }

    @Test
    void shouldParseInversePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ^ex:p ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(InversePathAst.class, triple.predicate());
    }

    @Test
    void shouldParseNegatedPropertySetPath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s !(ex:p1|ex:p2) ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(NegatedPropertySetPathAst.class, triple.predicate());
    }

    @Test
    void shouldParseSimplePredicatePathFromVerbSimple() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        assertInstanceOf(PredicatePathAst.class, triple.predicate());
    }

    @Test
    void shouldParseBlankNodePropertyListPathAsSubject() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  [ ex:p ?o ] .
                }
                """);

        BgpAst bgp = whereBgp(ast);
        assertEquals(1, bgp.triples().size());

        TriplePatternAst triple = bgp.triples().getFirst();
        assertTrue(((IriAst) triple.subject()).raw().startsWith("_:b"));
        assertEquals("ex:p", predicateIri(triple));
        assertEquals("o", ((VarAst) triple.object()).name());
    }

    @Test
    void shouldParseBlankNodePropertyListPathWithExternalProperties() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  [ ex:p ?o ] ex:q ?x .
                }
                """);

        BgpAst bgp = whereBgp(ast);
        assertEquals(2, bgp.triples().size());

        TriplePatternAst internal = bgp.triples().getFirst();
        TriplePatternAst external = bgp.triples().get(1);
        assertEquals(internal.subject(), external.subject());
        assertEquals("ex:p", predicateIri(internal));
        assertEquals("o", ((VarAst) internal.object()).name());
        assertEquals("ex:q", predicateIri(external));
        assertEquals("x", ((VarAst) external.object()).name());
    }

    @Test
    void shouldParseBlankNodePropertyListPathAsObject() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p [ ex:q ?x ] .
                }
                """);

        BgpAst bgp = whereBgp(ast);
        assertEquals(2, bgp.triples().size());

        TriplePatternAst inner = bgp.triples().getFirst();
        TriplePatternAst outer = bgp.triples().get(1);
        assertEquals("s", ((VarAst) outer.subject()).name());
        assertEquals("ex:p", predicateIri(outer));
        assertEquals(outer.object(), inner.subject());
        assertEquals("ex:q", predicateIri(inner));
        assertEquals("x", ((VarAst) inner.object()).name());
    }

    private static BgpAst whereBgp(QueryAst ast) {
        WhereClauseQueryAst query = (WhereClauseQueryAst) ast;
        return (BgpAst) query.whereClause().patterns().getFirst();
    }

    private static String predicateIri(TriplePatternAst triple) {
        return ((IriAst) ((PredicatePathAst) triple.predicate()).predicate()).raw();
    }
}
