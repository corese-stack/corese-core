package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
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

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SparqlParserSequenceTest extends AbstractSparqlParserFeatureTest {

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
}
