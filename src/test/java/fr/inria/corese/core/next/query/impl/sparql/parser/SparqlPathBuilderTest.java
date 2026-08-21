package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SparqlPathBuilder}.
 * <p>
 * Direct tests cover fold helpers that do not need ANTLR contexts.
 * Integration tests parse SPARQL property-path queries and assert the AST produced.
 */
class SparqlPathBuilderTest extends AbstractSparqlParserFeatureTest {

    private SparqlPathBuilder pathBuilder;

    @BeforeEach
    void setUp() {
        SparqlTermBuilder terms = new SparqlTermBuilder(new SparqlParserOptions.Builder().build());
        pathBuilder = new SparqlPathBuilder(terms);
    }

    // -------------------------------------------------------------------------
    // Direct unit tests — no ANTLR required
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("foldAlternatives()")
    class FoldAlternatives {

        @Test
        @DisplayName("single path is returned as-is")
        void singlePath() {
            PathAst p = new PredicatePathAst(new IriAst("<http://ex.org/p>"));
            PathAst result = pathBuilder.foldAlternatives(List.of(p));
            assertSame(p, result);
        }

        @Test
        @DisplayName("two paths produce AlternativePathAst")
        void twoPaths() {
            PathAst p1 = new PredicatePathAst(new IriAst("<http://ex.org/p1>"));
            PathAst p2 = new PredicatePathAst(new IriAst("<http://ex.org/p2>"));
            PathAst result = pathBuilder.foldAlternatives(List.of(p1, p2));
            assertInstanceOf(AlternativePathAst.class, result);
            AlternativePathAst alt = (AlternativePathAst) result;
            assertSame(p1, alt.left());
            assertSame(p2, alt.right());
        }

        @Test
        @DisplayName("three paths fold left-associatively")
        void threePathsFoldLeftAssociatively() {
            PathAst p1 = new PredicatePathAst(new IriAst("<http://ex.org/p1>"));
            PathAst p2 = new PredicatePathAst(new IriAst("<http://ex.org/p2>"));
            PathAst p3 = new PredicatePathAst(new IriAst("<http://ex.org/p3>"));
            PathAst result = pathBuilder.foldAlternatives(List.of(p1, p2, p3));
            // expected: (p1|p2)|p3
            assertInstanceOf(AlternativePathAst.class, result);
            AlternativePathAst outer = (AlternativePathAst) result;
            assertInstanceOf(AlternativePathAst.class, outer.left());
            assertSame(p3, outer.right());
        }

        @Test
        @DisplayName("empty list throws QueryEvaluationException")
        void emptyListThrows() {
            assertThrows(QueryEvaluationException.class, () -> pathBuilder.foldAlternatives(List.of()));
        }
    }

    @Nested
    @DisplayName("foldSequences()")
    class FoldSequences {

        @Test
        @DisplayName("single path is returned as-is")
        void singlePath() {
            PathAst p = new PredicatePathAst(new IriAst("<http://ex.org/p>"));
            PathAst result = pathBuilder.foldSequences(List.of(p));
            assertSame(p, result);
        }

        @Test
        @DisplayName("two paths produce SequencePathAst")
        void twoPaths() {
            PathAst p1 = new PredicatePathAst(new IriAst("<http://ex.org/p1>"));
            PathAst p2 = new PredicatePathAst(new IriAst("<http://ex.org/p2>"));
            PathAst result = pathBuilder.foldSequences(List.of(p1, p2));
            assertInstanceOf(SequencePathAst.class, result);
            SequencePathAst seq = (SequencePathAst) result;
            assertSame(p1, seq.left());
            assertSame(p2, seq.right());
        }

        @Test
        @DisplayName("empty list throws QueryEvaluationException")
        void emptyListThrows() {
            assertThrows(QueryEvaluationException.class, () -> pathBuilder.foldSequences(List.of()));
        }
    }

    // -------------------------------------------------------------------------
    // Integration tests via SPARQL parsing
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Property path parsing")
    class PropertyPathParsing {

        @Test
        @DisplayName("simple IRI predicate produces PredicatePathAst")
        void simpleIriPredicate() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s <http://ex.org/p> ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(PredicatePathAst.class, triple.predicate());
        }

        @Test
        @DisplayName("sequence path p/q produces SequencePathAst")
        void sequencePath() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s <http://ex.org/p>/<http://ex.org/q> ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(SequencePathAst.class, triple.predicate());
        }

        @Test
        @DisplayName("alternative path p|q produces AlternativePathAst")
        void alternativePath() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s <http://ex.org/p>|<http://ex.org/q> ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(AlternativePathAst.class, triple.predicate());
        }

        @Test
        @DisplayName("inverse path ^p produces InversePathAst")
        void inversePath() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s ^<http://ex.org/p> ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(InversePathAst.class, triple.predicate());
        }

        @Test
        @DisplayName("zero-or-more path p* produces ZeroOrMorePathAst")
        void zeroOrMorePath() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s <http://ex.org/p>* ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(ZeroOrMorePathAst.class, triple.predicate());
        }

        @Test
        @DisplayName("one-or-more path p+ produces OneOrMorePathAst")
        void oneOrMorePath() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s <http://ex.org/p>+ ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(OneOrMorePathAst.class, triple.predicate());
        }

        @Test
        @DisplayName("optional path p? produces OptionalPathAst")
        void optionalPath() {
            QueryAst ast = newParserDefault().parse(
                    "SELECT ?s WHERE { ?s <http://ex.org/p>? ?o }");
            TriplePatternAst triple = firstWhereTriple(ast);
            assertInstanceOf(OptionalPathAst.class, triple.predicate());
        }
    }
}
