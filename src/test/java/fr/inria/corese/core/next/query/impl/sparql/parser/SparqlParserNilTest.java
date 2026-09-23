package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests that the SPARQL NIL token {@code ()} is mapped to {@code rdf:nil}
 * and not left as the raw text {@code ()}.
 */
@DisplayName("SPARQL parser - NIL () maps to rdf:nil")
class SparqlParserNilTest extends AbstractSparqlParserFeatureTest {

    private static final String RDF_NIL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil";

    @Test
    @DisplayName("NIL () in object position resolves to rdf:nil IRI")
    void nilInObjectPositionResolvesToRdfNil() {
        var ast = newParserDefault().parse("""
                SELECT * WHERE {
                  ?s ?p ()
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        IriAst object = assertInstanceOf(IriAst.class, triple.object(),
                "NIL () must produce an IriAst, not a raw text term");
        assertEquals("<" + RDF_NIL + ">", object.raw(),
                "NIL () must be mapped to the rdf:nil IRI, not to '()'");
    }

    @Test
    @DisplayName("NIL () in subject position resolves to rdf:nil IRI")
    void nilInSubjectPositionResolvesToRdfNil() {
        var ast = newParserDefault().parse("""
                SELECT * WHERE {
                  () ?p ?o
                }
                """);

        TriplePatternAst triple = firstWhereTriple(ast);
        IriAst subject = assertInstanceOf(IriAst.class, triple.subject(),
                "NIL () in subject must produce an IriAst");
        assertEquals("<" + RDF_NIL + ">", subject.raw());
    }
}
