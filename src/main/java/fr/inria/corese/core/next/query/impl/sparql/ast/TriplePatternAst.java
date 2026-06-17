package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;

/**
 * A single triple pattern (s p o) in a BGP.
 */
public record TriplePatternAst(TermAst subject, TermAst predicate, TermAst object) implements VisitableAst {
    public TriplePatternAst {
        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("subject, predicate and object must be non-null");
        }
    }

    /**
     * Builds a triple whose predicate is a single term (IRI, variable, etc.).
     */
    public static TriplePatternAst of(TermAst subject, TermAst predicate, TermAst object) {
        return new TriplePatternAst(subject, PathAst.from(predicate), object);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.subject.accept(visitor);
        this.predicate.accept(visitor);
        this.object.accept(visitor);
    }
}
