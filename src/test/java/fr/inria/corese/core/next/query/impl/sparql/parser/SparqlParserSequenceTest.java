package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.data.api.support.io.IOConstants;
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
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.SequencePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.ZeroOrMorePathAst;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SparqlParserSequenceTest extends AbstractSparqlParserFeatureTest {

    private static final String PREFIX = """
            PREFIX ex: <http://example.org/>
            """;
    private static final String USER_BLANK_NODE_LABEL = IOConstants.BLANK_NODE_PREFIX + "b0";

    @Test
    void shouldParseSequencePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p1/ex:p2 ?o .
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        SequencePathAst sequence = assertInstanceOf(SequencePathAst.class, triple.predicate());
        assertPredicate("ex:p1", sequence.left());
        assertPredicate("ex:p2", sequence.right());
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
        AlternativePathAst alternative = assertInstanceOf(AlternativePathAst.class, triple.predicate());
        assertPredicate("ex:p1", alternative.left());
        assertPredicate("ex:p2", alternative.right());
    }

    @Test
    void shouldGiveSequenceHigherPrecedenceThanAlternativePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p1/ex:p2|ex:p3 ?o .
                }
                """);

        AlternativePathAst alternative = assertInstanceOf(
                AlternativePathAst.class,
                firstWhereTriple(ast).predicate());
        SequencePathAst sequence = assertInstanceOf(SequencePathAst.class, alternative.left());
        assertPredicate("ex:p1", sequence.left());
        assertPredicate("ex:p2", sequence.right());
        assertPredicate("ex:p3", alternative.right());
    }

    @Test
    void shouldKeepParenthesizedAlternativeInsideSequencePath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p1/(ex:p2|ex:p3) ?o .
                }
                """);

        SequencePathAst sequence = assertInstanceOf(
                SequencePathAst.class,
                firstWhereTriple(ast).predicate());
        assertPredicate("ex:p1", sequence.left());
        AlternativePathAst alternative = assertInstanceOf(AlternativePathAst.class, sequence.right());
        assertPredicate("ex:p2", alternative.left());
        assertPredicate("ex:p3", alternative.right());
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
        ZeroOrMorePathAst path = assertInstanceOf(ZeroOrMorePathAst.class, triple.predicate());
        assertPredicate("ex:p", path.pathAst());
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
        OptionalPathAst path = assertInstanceOf(OptionalPathAst.class, triple.predicate());
        assertPredicate("ex:p", path.pathAst());
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
        OneOrMorePathAst path = assertInstanceOf(OneOrMorePathAst.class, triple.predicate());
        assertPredicate("ex:p", path.pathAst());
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
        InversePathAst path = assertInstanceOf(InversePathAst.class, triple.predicate());
        assertPredicate("ex:p", path.pathAst());
    }

    @Test
    void shouldApplyInverseToModifiedPathElt() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ^ex:p+ ?o .
                }
                """);

        InversePathAst inverse = assertInstanceOf(
                InversePathAst.class,
                firstWhereTriple(ast).predicate());
        OneOrMorePathAst plus = assertInstanceOf(OneOrMorePathAst.class, inverse.pathAst());
        assertPredicate("ex:p", plus.pathAst());
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
        NegatedPropertySetPathAst negated = assertInstanceOf(NegatedPropertySetPathAst.class, triple.predicate());
        assertEquals(2, negated.excluded().size());
        assertPredicate("ex:p1", negated.excluded().get(0));
        assertPredicate("ex:p2", negated.excluded().get(1));
    }

    @Test
    void shouldParseInverseInsideNegatedPropertySetPath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s !(^ex:p|ex:q) ?o .
                }
                """);

        NegatedPropertySetPathAst negated = assertInstanceOf(
                NegatedPropertySetPathAst.class,
                firstWhereTriple(ast).predicate());
        assertEquals(2, negated.excluded().size());
        InversePathAst inverse = assertInstanceOf(InversePathAst.class, negated.excluded().getFirst());
        assertPredicate("ex:p", inverse.pathAst());
        assertPredicate("ex:q", negated.excluded().get(1));
    }

    @Test
    void shouldKeepMixedPropertyListPathPredicatesInSourceOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:p1/ex:p2 ?o ;
                     ?p ?x ;
                     ex:p3|ex:p4 ?y .
                }
                """);

        BgpAst bgp = whereBgp(ast);
        assertEquals(3, bgp.triples().size());
        assertInstanceOf(SequencePathAst.class, bgp.triples().get(0).predicate());
        assertPredicateVariable("p", bgp.triples().get(1).predicate());
        assertInstanceOf(AlternativePathAst.class, bgp.triples().get(2).predicate());
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
    void generatedBlankNodeShouldNotCollideWithUserBlankNodeLabel() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(PREFIX + """
                SELECT * WHERE {
                  ?s ex:link [ ex:p ?o ] .
                  %s ex:p ?o .
                }
                """.formatted(USER_BLANK_NODE_LABEL));

        List<TriplePatternAst> triples = ((WhereClauseQueryAst) ast).whereClause().patterns().stream()
                .filter(BgpAst.class::isInstance)
                .map(BgpAst.class::cast)
                .flatMap(bgp -> bgp.triples().stream())
                .toList();
        assertEquals(3, triples.size());

        IriAst userBlankNode = triples.stream()
                .map(TriplePatternAst::subject)
                .filter(IriAst.class::isInstance)
                .map(IriAst.class::cast)
                .filter(subject -> USER_BLANK_NODE_LABEL.equals(subject.raw()))
                .findFirst()
                .orElseThrow();

        List<IriAst> generatedBlankNodes = triples.stream()
                .flatMap(triple -> Stream.of(triple.subject(), triple.object()))
                .filter(IriAst.class::isInstance)
                .map(IriAst.class::cast)
                .filter(term -> term.raw().startsWith(IOConstants.BLANK_NODE_PREFIX))
                .filter(term -> !USER_BLANK_NODE_LABEL.equals(term.raw()))
                .toList();

        assertEquals(2, generatedBlankNodes.size());
        assertEquals(1, generatedBlankNodes.stream().distinct().count());
        assertNotEquals(userBlankNode, generatedBlankNodes.getFirst());
    }

    private static void assertPredicate(String expectedRaw, PathAst path) {
        PredicatePathAst predicatePath = assertInstanceOf(PredicatePathAst.class, path);
        IriAst predicate = assertInstanceOf(IriAst.class, predicatePath.predicate());
        assertEquals(expectedRaw, predicate.raw());
    }

    private static void assertPredicateVariable(String expectedName, PathAst path) {
        PredicatePathAst predicatePath = assertInstanceOf(PredicatePathAst.class, path);
        VarAst predicate = assertInstanceOf(VarAst.class, predicatePath.predicate());
        assertEquals(expectedName, predicate.name());
    }

    private static BgpAst whereBgp(QueryAst ast) {
        WhereClauseQueryAst query = (WhereClauseQueryAst) ast;
        return (BgpAst) query.whereClause().patterns().getFirst();
    }
}
