package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.AlternativePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.InversePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.NegatedPropertySetPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OneOrMorePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OptionalPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.SequencePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.ZeroOrMorePathAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}
