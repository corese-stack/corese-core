package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.*;

import java.util.*;

/**
 * Collects visible and referenced variables from the next SPARQL AST.
 * A visible variable is introduced by a graph pattern in the WHERE clause and
 * can appear in the query solutions. A referenced variable is only mentioned
 * in an expression such as FILTER or ORDER BY; mentioning it there does not
 * make it visible.
 *
 * <p>Referenced-variable collection is intentionally incremental. When a new
 * expression shape matters for semantic validation, support should be added
 * here explicitly instead of inferred elsewhere.</p>
 */
public final class VariableScopeAnalyzer {

    /**
     * Collects variables visible from a Query.
     *
     * @param query the query to inspect
     * @return the set of visible variable names, without {@code ?} or {@code $} in the patterns used for the resolution of the query
     */
    public Set<String> collectVisibleVariables(QueryAst query) {
        Set<String> visibleVariables = new TreeSet<>();
        if(query instanceof WhereClauseQueryAst whereClauseQueryAst) {
            visibleVariables.addAll(collectVisibleVariables(whereClauseQueryAst.whereClause()));
        }
        if(query instanceof SparqlQueryAst sparqlQueryAst) {
            visibleVariables.addAll(collectVisibleVariables(sparqlQueryAst.valuesClause()));
        }
        return visibleVariables;
    }

    /**
     * Collects variables visible from a VALUES clause.
     *
     * @param valuesClause the WHERE clause to inspect
     * @return the set of visible variable names, without {@code ?} or {@code $}
     */
    public Set<String> collectVisibleVariables(ValuesAst valuesClause) {
        Set<String> visibleVariables = new LinkedHashSet<>();

        if (valuesClause == null) {
            return visibleVariables;
        }

        valuesClause.mappings().forEach(valueMappingAst ->
                valueMappingAst.values().keySet().forEach(varAst -> {
                    if (varAst != null) {
                        visibleVariables.add(varAst.name());
                    }
                }));

        return visibleVariables;
    }

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

    /**
     * Collects variables referenced outside aggregate calls in a term or expression.
     *
     * @param term the term or expression to inspect
     * @return the set of referenced variable names that are not shielded by an aggregate
     */
    public Set<String> collectReferencedVariablesOutsideAggregates(TermAst term) {
        Set<String> referencedVariables = new LinkedHashSet<>();
        collectReferencedVariablesOutsideAggregates(term, referencedVariables);
        return referencedVariables;
    }

    /**
     * Returns {@code true} when the given term contains at least one aggregate call.
     *
     * @param term the term or expression to inspect
     * @return {@code true} if an aggregate call occurs anywhere inside the term
     */
    public boolean containsAggregate(TermAst term) {
        if (term == null) {
            return false;
        }

        return switch (term) {
            case AggregateAst ignored -> true;

            case UnaryConstraintAst unaryConstraint ->
                    containsAggregate(unaryConstraint.argument());

            case BinaryConstraintAst binaryConstraint ->
                    containsAggregate(binaryConstraint.getLeftArgument())
                            || containsAggregate(binaryConstraint.getRightArgument());

            case FunctionCallAst(TermAst ignored, List<TermAst> arguments) -> arguments.stream()
                    .anyMatch(this::containsAggregate);

            case BnodeAst bnodeAst -> containsAggregate(bnodeAst.getLabel());

            case TrinaryRegexAst regexAst ->
                    containsAggregate(regexAst.getString())
                            || containsAggregate(regexAst.getPattern())
                            || containsAggregate(regexAst.getFlags());

            case SubstrAst substrAst ->
                    containsAggregate(substrAst.getString())
                            || containsAggregate(substrAst.getStart())
                            || containsAggregate(substrAst.getLength());

            case ReplaceAst replaceAst ->
                    containsAggregate(replaceAst.getString())
                            || containsAggregate(replaceAst.getPattern())
                            || containsAggregate(replaceAst.getReplacement())
                            || (replaceAst.hasFlags() && containsAggregate(replaceAst.getFlags()));

            case IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) ->
                    containsAggregate(condition)
                            || containsAggregate(thenExpr)
                            || containsAggregate(elseExpr);

            case CoalesceAst coalesceAst -> coalesceAst.arguments().stream().anyMatch(this::containsAggregate);

            case ConcatAst concatAst -> concatAst.arguments().stream().anyMatch(this::containsAggregate);

            case InAst(TermAst left, List<TermAst> candidates) ->
                    containsAggregate(left) || candidates.stream().anyMatch(this::containsAggregate);

            case NotInAst(TermAst left, List<TermAst> candidates) ->
                    containsAggregate(left) || candidates.stream().anyMatch(this::containsAggregate);

            default -> false;
        };
    }

    private void collectVisibleVariables(PatternAst pattern, Set<String> visibleVariables) {
        if (pattern == null) {
            return;
        }

        switch (pattern) {
            case BgpAst(List<TriplePatternAst> triples) -> {
                for (TriplePatternAst triple : triples) {
                    addIfVariable(triple.subject(), visibleVariables);
                    collectVisibleVariablesFromPath(triple.predicate(), visibleVariables);
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

            case MinusAst ignored -> {
                // MINUS does not make its inner variables visible outside the pattern.
            }

            case UnionAst(GroupGraphPatternAst left, GroupGraphPatternAst right) -> {
                // UNION exposes variables from both branches.
                collectVisibleVariables(left, visibleVariables);
                collectVisibleVariables(right, visibleVariables);
            }
            case BindAst(TermAst expression, VarAst variable) ->
                visibleVariables.add(variable.name());

            case FilterAst ignored -> {
                // FILTER does not make a variable visible by itself.
            }

            case ServiceAst(TermAst endpoint, boolean silentFlag, GroupGraphPatternAst servicePattern) -> {
                // SERVICE exposes variables from its inner graph pattern.
                collectVisibleVariables(servicePattern, visibleVariables);
                addIfVariable(endpoint, visibleVariables);
            }

            case SubQueryAst(SelectQueryAst select) -> {

                ProjectionAst proj = select.projection();

                if (proj.selectAll()) {
                    collectVisibleVariables(select.whereClause(), visibleVariables);
                } else {
                    for (VarAst v : proj.variables()) {
                        visibleVariables.add(v.name());
                    }
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + pattern);
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
                collectReferencedVariables(unaryConstraint.argument(), referencedVariables);

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

            case BnodeAst bnodeAst ->
                collectReferencedVariables(bnodeAst.getLabel(), referencedVariables);

            case TrinaryRegexAst regexAst -> {
                // REGEX may reference variables in the text, pattern or flags.
                collectReferencedVariables(regexAst.getString(), referencedVariables);
                collectReferencedVariables(regexAst.getPattern(), referencedVariables);
                collectReferencedVariables(regexAst.getFlags(), referencedVariables);
            }

            case SubstrAst substrAst -> {
                collectReferencedVariables(substrAst.getString(), referencedVariables);
                collectReferencedVariables(substrAst.getStart(), referencedVariables);
                collectReferencedVariables(substrAst.getLength(), referencedVariables);
            }

            case ReplaceAst replaceAst -> {
                collectReferencedVariables(replaceAst.getString(), referencedVariables);
                collectReferencedVariables(replaceAst.getPattern(), referencedVariables);
                collectReferencedVariables(replaceAst.getReplacement(), referencedVariables);
                if (replaceAst.hasFlags()) {
                    collectReferencedVariables(replaceAst.getFlags(), referencedVariables);
                }
            }

            case IriAst ignoredIri -> {
                // Constants do not contribute referenced variables.
            }

            case LiteralAst ignoredLiteral -> {
                // Constants do not contribute referenced variables.
            }

            case IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) -> {
                collectReferencedVariables(condition, referencedVariables);
                collectReferencedVariables(thenExpr, referencedVariables);
                collectReferencedVariables(elseExpr, referencedVariables);
            }

            case CoalesceAst coalesceAst -> {
                for (TermAst argument : coalesceAst.arguments()) {
                    collectReferencedVariables(argument, referencedVariables);
                }
            }

            case ConcatAst concatAst -> {
                for (TermAst argument : concatAst.arguments()) {
                    collectReferencedVariables(argument, referencedVariables);
                }
            }

            case InAst(TermAst left, List<TermAst> candidates) -> {
                collectReferencedVariables(left, referencedVariables);
                for (TermAst candidate : candidates) {
                    collectReferencedVariables(candidate, referencedVariables);
                }
            }

            case NotInAst(TermAst left, List<TermAst> candidates) -> {
                collectReferencedVariables(left, referencedVariables);
                for (TermAst candidate : candidates) {
                    collectReferencedVariables(candidate, referencedVariables);
                }
            }

            case AggregateAst(
                    AggregateFunction ignoredFunction, boolean ignoredDistinct, TermAst expression, String ignoredSep) ->
                    collectReferencedVariables(expression, referencedVariables);

            case ConstraintAst ignored -> {
                // Other constraint shapes must be added explicitly when scope validation starts relying on them.
            }
        }
    }

    private void collectReferencedVariablesOutsideAggregates(TermAst term, Set<String> referencedVariables) {
        if (term == null) {
            return;
        }

        switch (term) {
            case VarAst(String name) -> referencedVariables.add(name);

            case AggregateAst ignored -> {
                // Variables used under an aggregate are handled by aggregate semantics, not raw grouping scope.
            }

            case UnaryConstraintAst unaryConstraint ->
                    collectReferencedVariablesOutsideAggregates(unaryConstraint.argument(), referencedVariables);

            case BinaryConstraintAst binaryConstraint -> {
                collectReferencedVariablesOutsideAggregates(binaryConstraint.getLeftArgument(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(binaryConstraint.getRightArgument(), referencedVariables);
            }

            case FunctionCallAst(TermAst ignored, List<TermAst> arguments) -> {
                for (TermAst argument : arguments) {
                    collectReferencedVariablesOutsideAggregates(argument, referencedVariables);
                }
            }

            case BnodeAst bnodeAst ->
                    collectReferencedVariablesOutsideAggregates(bnodeAst.getLabel(), referencedVariables);

            case TrinaryRegexAst regexAst -> {
                collectReferencedVariablesOutsideAggregates(regexAst.getString(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(regexAst.getPattern(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(regexAst.getFlags(), referencedVariables);
            }

            case SubstrAst substrAst -> {
                collectReferencedVariablesOutsideAggregates(substrAst.getString(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(substrAst.getStart(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(substrAst.getLength(), referencedVariables);
            }

            case ReplaceAst replaceAst -> {
                collectReferencedVariablesOutsideAggregates(replaceAst.getString(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(replaceAst.getPattern(), referencedVariables);
                collectReferencedVariablesOutsideAggregates(replaceAst.getReplacement(), referencedVariables);
                if (replaceAst.hasFlags()) {
                    collectReferencedVariablesOutsideAggregates(replaceAst.getFlags(), referencedVariables);
                }
            }

            case IriAst ignoredIri -> {
                // Constants do not contribute referenced variables.
            }

            case LiteralAst ignoredLiteral -> {
                // Constants do not contribute referenced variables.
            }

            case IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) -> {
                collectReferencedVariablesOutsideAggregates(condition, referencedVariables);
                collectReferencedVariablesOutsideAggregates(thenExpr, referencedVariables);
                collectReferencedVariablesOutsideAggregates(elseExpr, referencedVariables);
            }

            case CoalesceAst coalesceAst -> {
                for (TermAst argument : coalesceAst.arguments()) {
                    collectReferencedVariablesOutsideAggregates(argument, referencedVariables);
                }
            }

            case ConcatAst concatAst -> {
                for (TermAst argument : concatAst.arguments()) {
                    collectReferencedVariablesOutsideAggregates(argument, referencedVariables);
                }
            }

            case InAst(TermAst left, List<TermAst> candidates) -> {
                collectReferencedVariablesOutsideAggregates(left, referencedVariables);
                for (TermAst candidate : candidates) {
                    collectReferencedVariablesOutsideAggregates(candidate, referencedVariables);
                }
            }

            case NotInAst(TermAst left, List<TermAst> candidates) -> {
                collectReferencedVariablesOutsideAggregates(left, referencedVariables);
                for (TermAst candidate : candidates) {
                    collectReferencedVariablesOutsideAggregates(candidate, referencedVariables);
                }
            }

            case ConstraintAst ignored -> {
                // Other constraint shapes must be added explicitly when grouped-projection validation starts relying on them.
            }
        }
    }

    private void addIfVariable(TermAst term, Set<String> variables) {
        if (term instanceof VarAst(String name)) {
            variables.add(name);
        }
    }

    private void collectVisibleVariablesFromPath(PathAst path, Set<String> visibleVariables) {
        switch (path) {
            case PredicatePathAst(TermAst predicate) -> addIfVariable(predicate, visibleVariables);
            case SequencePathAst(PathAst left, PathAst right) -> {
                collectVisibleVariablesFromPath(left, visibleVariables);
                collectVisibleVariablesFromPath(right, visibleVariables);
            }
            case AlternativePathAst(PathAst left, PathAst right) -> {
                collectVisibleVariablesFromPath(left, visibleVariables);
                collectVisibleVariablesFromPath(right, visibleVariables);
            }
            case ZeroOrMorePathAst(PathAst inner) -> collectVisibleVariablesFromPath(inner, visibleVariables);
            case OneOrMorePathAst(PathAst inner) -> collectVisibleVariablesFromPath(inner, visibleVariables);
            case OptionalPathAst(PathAst inner) -> collectVisibleVariablesFromPath(inner, visibleVariables);
            case InversePathAst(PathAst inner) -> collectVisibleVariablesFromPath(inner, visibleVariables);
            case NegatedPropertySetPathAst(List<PathAst> excluded) -> {
                for (PathAst excludedPath : excluded) {
                    collectVisibleVariablesFromPath(excludedPath, visibleVariables);
                }
            }
        }
    }
}
