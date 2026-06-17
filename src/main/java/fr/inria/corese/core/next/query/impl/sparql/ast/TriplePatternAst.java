package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;

/**
 * A single triple pattern (s p o) in a BGP.
 */
public record TriplePatternAst(TermAst subject, PathAst predicate, TermAst object) {
    public TriplePatternAst {
        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("subject, predicate and object must be non-null");
        }
    }

    /**
     * Builds a triple whose predicate is a single term (IRI, variable, etc.).
     */
    public static TriplePatternAst of(TermAst subject, TermAst predicate, TermAst object) {
        return new TriplePatternAst(subject, new PredicatePathAst(predicate), object);
    }
}
