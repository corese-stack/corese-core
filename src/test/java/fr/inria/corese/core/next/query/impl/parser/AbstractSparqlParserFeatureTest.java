package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAstTestSupport;

import static fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAstTestSupport.simplePredicateTerm;

public class AbstractSparqlParserFeatureTest {

    /**
     * Expected {@link IriAst} for the SPARQL shortcut {@code a} ({@code rdf:type}), as produced by {@link SparqlAstBuilder#iri(String)}.
     */
    protected static IriAst expectedRdfTypeIriAst() {
        return new IriAst("<" + RDF.type.getIRI().stringValue() + ">");
    }

    /**
     * create a Default Sparql Parser with default Config
     * @return SparqlParser
     */
    protected SparqlParser newParserDefault() {
        return new SparqlParser(new SparqlParserOptions.Builder().build());
    }

    /**
     *  create a Sparql Parser with Option
     * @param failFast option to make the Parsing fail if it encountered an error
     * @param collectErrors option to collect error encountered when parsing
     * @return SparqlParser
     */
    protected SparqlParser newParser(boolean failFast, boolean collectErrors) {
        return new SparqlParser(new SparqlParserOptions.Builder()
                .failFast(failFast)
                .collectErrors(collectErrors)
                .build());
    }

    protected static fr.inria.corese.core.next.query.impl.sparql.ast.TermAst simplePredicateTerm(TriplePatternAst triple) {
        return TriplePatternAstTestSupport.simplePredicateTerm(triple);
    }
}
