package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.AlternativePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OptionalPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.SequencePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.ZeroOrMorePathAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SparqlParserSequenceTest extends AbstractSparqlParserFeatureTest {

    @Test
    void shouldParseSequencePath() {
        SparqlParser parser = newParserDefault();

        SELECT * WHERE {
          ?s ex:p1/ex:p2 ?o .
        }
        """);

    }

    @Test
    void shouldParseAlternativePath() {
        SparqlParser parser = newParserDefault();
        SELECT * WHERE {
          ?s ex:p1|ex:p2 ?o .
        }
        """);

    }

    @Test
    void shouldParseZeroOrMorePath() {
        SparqlParser parser = newParserDefault();

        SELECT * WHERE {
          ?s ex:p* ?o .
        }
        """);

    }

    @Test
    void shouldParseOptionalPath() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
        SELECT * WHERE {
        }
        """);




}
