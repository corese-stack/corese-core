package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.GraphAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OptionalAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ServiceAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UnionAst;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the SPARQL simplified-group transformation on the AST.
 *
 * <p>Per SPARQL 1.1 algebra (§18.2.2), a nested {@link GroupGraphPatternAst}
 * whose top-level elements contain no structural operators (OPTIONAL, MINUS,
 * UNION) is <em>simple</em> and therefore transparent: its contents are merged
 * directly into the enclosing group ({@code { { P } } ≡ { P_elements }}).</p>
 *
 * <p>This transformation is applied before compilation so that FILTERs inside
 * such groups are visible as direct children of the compiled pattern and can be
 * correctly detected and deferred to merge time by the OPTIONAL evaluation
 * machinery ({@code Exp.optional()}).</p>
 */
final class AstSimplifier {

    /**
     * Returns a new {@link GroupGraphPatternAst} with the first leading simple
     * nested group flattened into the enclosing group, applied recursively.
     *
     * <p>Only the <em>first</em> element of a group is eligible for flattening.
     * A FILTER in a nested group that follows other patterns (e.g.
     * {@code { BGP . { FILTER } }}) must remain scoped to its inner group and
     * cannot see variables bound by the preceding BGP — this is the W3C
     * {@code filter-nested-2} semantics. Flattening only the leading element
     * mirrors the {@code body.size() == 0} condition in
     * {@link WhereCompiler#compileGroup}.</p>
     */
    GroupGraphPatternAst simplify(GroupGraphPatternAst group) {
        List<PatternAst> result = new ArrayList<>();
        boolean canFlattenNext = true;
        for (PatternAst pattern : group.patterns()) {
            PatternAst simplified = simplifyPattern(pattern);
            if (canFlattenNext
                    && simplified instanceof GroupGraphPatternAst nested
                    && isSimple(nested)) {
                // First leading simple nested group is transparent: inline its elements
                result.addAll(nested.patterns());
            } else {
                result.add(simplified);
            }
            canFlattenNext = false;
        }
        return new GroupGraphPatternAst(result);
    }

    private PatternAst simplifyPattern(PatternAst pattern) {
        return switch (pattern) {
            case GroupGraphPatternAst g -> simplify(g);
            case OptionalAst(PatternAst inner) ->
                    new OptionalAst(inner instanceof GroupGraphPatternAst g ? simplify(g) : inner);
            case MinusAst(GroupGraphPatternAst inner) -> new MinusAst(simplify(inner));
            case UnionAst(GroupGraphPatternAst left, GroupGraphPatternAst right) ->
                    new UnionAst(simplify(left), simplify(right));
            case GraphAst(TermAst name, GroupGraphPatternAst g) ->
                    new GraphAst(name, simplify(g));
            case ServiceAst(TermAst endpoint, boolean silent, GroupGraphPatternAst g) ->
                    new ServiceAst(endpoint, silent, simplify(g));
            default -> pattern;
        };
    }

    /**
     * A group is <em>simple</em> (transparent) when none of its top-level
     * elements is a structural operator (OPTIONAL, MINUS, UNION).
     */
    private boolean isSimple(GroupGraphPatternAst group) {
        for (PatternAst p : group.patterns()) {
            if (p instanceof OptionalAst || p instanceof MinusAst || p instanceof UnionAst) {
                return false;
            }
        }
        return true;
    }
}
