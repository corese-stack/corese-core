package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test helpers for {@link TriplePatternAst} predicates wrapped in {@link PredicatePathAst}.
 */
public final class TriplePatternAstTestSupport {

    private TriplePatternAstTestSupport() {
    }

    public static TermAst simplePredicateTerm(TriplePatternAst triple) {
        PathAst predicate = triple.predicate();
        assertInstanceOf(PredicatePathAst.class, predicate,
                "expected a simple predicate wrapped in PredicatePathAst");
        return ((PredicatePathAst) predicate).predicate();
    }

    public static PredicatePathAst predicatePath(TermAst term) {
        return new PredicatePathAst(term);
    }
}
