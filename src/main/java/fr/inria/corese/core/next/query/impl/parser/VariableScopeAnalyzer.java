package fr.inria.corese.core.next.query.impl.parser;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OptionalAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UnionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TrinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UnaryConstraintAst;

/**
 * Collects visible and referenced variables from the next SPARQL AST.
 * A visible variable is introduced by a graph pattern in the WHERE clause and
 * can appear in the query solutions. A referenced variable is only mentioned
 * in an expression such as FILTER or ORDER BY; mentioning it there does not
 * make it visible.
 */
public final class VariableScopeAnalyzer {

    /**
     * Collects variables visible from a WHERE clause.
     *
     * @param whereClause the WHERE clause to inspect
     * @return the set of visible variable names, without {@code ?} or {@code $}
     */
    public Set<String> collectVisibleVariables(GroupGraphPatternAst whereClause) {
        Set<String> visibleVariables = new LinkedHashSet<>();
        if (whereClause == null) {
            return visibleVariables;
        }

        for (PatternAst pattern : whereClause.patterns()) {
            collectVisibleVariables(pattern, visibleVariables);
        }

        return visibleVariables;
    }

    /**
     * Collects variables referenced by a term or expression.
     *
     * @param term the term or expression to inspect
     * @return the set of referenced variable names, without {@code ?} or {@code $}
     */
    public Set<String> collectReferencedVariables(TermAst term) {
        Set<String> referencedVariables = new LinkedHashSet<>();
        collectReferencedVariables(term, referencedVariables);
        return referencedVariables;
    }

    private void collectVisibleVariables(PatternAst pattern, Set<String> visibleVariables) {
        if (pattern == null) {
            return;
        }

        switch (pattern) {
            case BgpAst(List<TriplePatternAst> triples) -> {
                // Variables come from triple terms.
                for (TriplePatternAst triple : triples) {
                    addIfVariable(triple.subject(), visibleVariables);
                    addIfVariable(triple.predicate(), visibleVariables);
                    addIfVariable(triple.object(), visibleVariables);
                }
            }

            case GroupGraphPatternAst(List<PatternAst> patterns) -> {
                // Recurse into nested groups.
                for (PatternAst nestedPattern : patterns) {
                    collectVisibleVariables(nestedPattern, visibleVariables);
                }
            }

            case OptionalAst(PatternAst optionalPattern) ->
                // OPTIONAL keeps variables in scope.
                collectVisibleVariables(optionalPattern, visibleVariables);

            case UnionAst(GroupGraphPatternAst left, GroupGraphPatternAst right) -> {
                // UNION exposes variables from both branches.
                collectVisibleVariables(left, visibleVariables);
                collectVisibleVariables(right, visibleVariables);
            }

            case FilterAst ignored -> {
                // FILTER does not make a variable visible by itself.
            }
        }
    }

    private void collectReferencedVariables(TermAst term, Set<String> referencedVariables) {
        if (term == null) {
            return;
        }

        switch (term) {
            // A direct variable reference contributes to scope checks.
            case VarAst(String name) -> referencedVariables.add(name);

            // Recurse into unary expressions such as STR(?x) or BOUND(?x).
            case UnaryConstraintAst unaryConstraint ->
                collectReferencedVariables(unaryConstraint.getArgument(), referencedVariables);

            case BinaryConstraintAst binaryConstraint -> {
                // Recurse into both operands of binary expressions.
                collectReferencedVariables(binaryConstraint.getLeftArgument(), referencedVariables);
                collectReferencedVariables(binaryConstraint.getRightArgument(), referencedVariables);
            }

            case FunctionCallAst(TermAst ignored, List<TermAst> arguments) -> {
                // Recurse into each function argument.
                for (TermAst argument : arguments) {
                    collectReferencedVariables(argument, referencedVariables);
                }
            }

            case TrinaryRegexAst regexAst -> {
                // REGEX may reference variables in the text, pattern or flags.
                collectReferencedVariables(regexAst.getString(), referencedVariables);
                collectReferencedVariables(regexAst.getPattern(), referencedVariables);
                collectReferencedVariables(regexAst.getFlags(), referencedVariables);
            }

            case IriAst ignoredIri -> {
                // Constants do not contribute referenced variables.
            }

            case LiteralAst ignoredLiteral -> {
                // Constants do not contribute referenced variables.
            }

            case ConstraintAst ignored -> {
                // Other constraint shapes are ignored until they are supported here.
            }
        }
    }

    private void addIfVariable(TermAst term, Set<String> variables) {
        if (term instanceof VarAst(String name)) {
            variables.add(name);
        }
    }
}
