package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public sealed interface PathAst
        permits PredicatePathAst,
        SequencePathAst,
        AlternativePathAst,
        ZeroOrMorePathAst,
        OneOrMorePathAst,
        OptionalPathAst,
        InversePathAst,
        NegatedPropertySetPathAst {

    /**
     * Wraps a single term (IRI, variable, etc.) as a property path.
     */
    static PathAst from(TermAst term) {
        return new PredicatePathAst(term);
    }
}