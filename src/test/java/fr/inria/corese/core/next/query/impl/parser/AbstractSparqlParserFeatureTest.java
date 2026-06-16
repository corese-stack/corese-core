package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAstTestSupport;
import fr.inria.corese.core.next.query.impl.sparql.ast.WhereClauseQueryAst;

import static fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAstTestSupport.simplePredicateTerm;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    protected static TriplePatternAst firstWhereTriple(QueryAst ast) {
        WhereClauseQueryAst query = (WhereClauseQueryAst) ast;
        BgpAst bgp = (BgpAst) query.whereClause().patterns().getFirst();
        return bgp.triples().getFirst();
    }
}
