package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PrefixDeclarationAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SparqlTermResolverTest {

    @Test
    void decodesSparqlEscapesExactlyOnce() {
        SparqlTermResolver resolver = new SparqlTermResolver(null);

        assertEquals("\t\n\r\b\f\"'\\", resolver.unquoteLexical("\"\\t\\n\\r\\b\\f\\\"\\'\\\\\""));
        assertEquals("\\n", resolver.unquoteLexical("\"\\\\n\""));
        assertEquals("a\nb", resolver.unquoteLexical("\"\"\"a\\nb\"\"\""));
        assertEquals("a\tb", resolver.unquoteLexical("'''a\\tb'''"));
    }

    @Test
    void preservesUnknownEscapesAndTrailingBackslash() {
        assertEquals("\\d", SparqlTermResolver.processSparqlEscapes("\\d"));
        assertEquals("end\\", SparqlTermResolver.processSparqlEscapes("end\\"));
        assertEquals("", SparqlTermResolver.processSparqlEscapes(""));
    }

    @Test
    void resolvesTermsFromItsOwnPrologue() {
        QueryPrologueAst prologue = new QueryPrologueAst(
                List.of(new PrefixDeclarationAst("ex:", new IriAst("http://example.org/"))),
                new IriAst("http://base.example/dir/"));
        SparqlTermResolver resolver = new SparqlTermResolver(prologue);

        assertEquals("http://example.org/name", resolver.resolveIri("ex:name"));
        assertEquals("http://base.example/dir/name", resolver.resolveIri("<name>"));
        assertEquals(RDF.type.getIRI().stringValue(), resolver.resolveIri("a"));
    }

    @Test
    void supportsStandardPrefixesWithoutLegacyNamespaceState() {
        SparqlTermResolver resolver = new SparqlTermResolver(null);

        assertEquals(XSD.xsdInteger.getIRI().stringValue(),
                resolver.normalizeDatatypeIri("xsd:integer"));
    }

    @Test
    void absoluteIriIsNeverReinterpretedAsAPrefixedName() {
        QueryPrologueAst prologue = new QueryPrologueAst(
                List.of(new PrefixDeclarationAst("http:", new IriAst("http://wrong.example/"))),
                new IriAst("http://base.example/"));
        SparqlTermResolver resolver = new SparqlTermResolver(prologue);

        assertEquals("http://example.org/resource",
                resolver.resolveIri("http://example.org/resource"));
    }

    @Test
    void extractsLiteralLexicalForm() {
        SparqlTermResolver resolver = new SparqlTermResolver(null);

        assertEquals("hello", resolver.unquoteLexical("\"hello\"@en"));
        assertEquals("plain", resolver.unquoteLexical("plain"));
    }

    @Test
    void decodesUnicodeInIrisAndDatatypes() {
        var resolver = new SparqlTermResolver(new QueryPrologueAst(
                List.of(new PrefixDeclarationAst("ex:", new IriAst("http://example.org/A/"))),
                new IriAst("http://example.org/B/")));
        assertEquals("http://example.org/A/name", resolver.resolveIri("ex:name"));
        assertEquals("http://example.org/B/C", resolver.resolveIri("<\\u0043>"));
        assertEquals("http://example.org/😀", resolver.resolveIri("<http://example.org/\\U0001F600>"));
        assertEquals("http://example.org/A", resolver.normalizeDatatypeIri("<http://example.org/\\u0041>"));
    }

    @Test
    void rejectsNonScalarUnicodeInIris() {
        var resolver = new SparqlTermResolver(null);
        for (String escape : List.of("uD800", "U0000DFFF", "U00110000", "UFFFFFFFF")) {
            assertThrows(QuerySyntaxException.class,
                    () -> resolver.resolveIri("<http://example.org/\\" + escape + ">"));
        }
    }
}
