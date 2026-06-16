package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;

/**
 * A single triple pattern (s p o) in a BGP.
 */
public record TriplePatternAst(TermAst subject, TermAst predicate, TermAst object) {
    public TriplePatternAst {
        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("subject, predicate and object must be non-null");
        }
    }

    public TriplePatternAst(TermAst subject, TermAst predicate, TermAst object) {
        this(subject, new PredicatePathAst(predicate), object);
    }
}
