package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PrefixDeclarationAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SparqlTermResolverTest {

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
}
