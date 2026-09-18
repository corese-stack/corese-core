package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.sparql.ast.ASTConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupByAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.HavingAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AndAst;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VariableScopeAnalyzer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compiles SPARQL solution modifiers (projection, GROUP BY, HAVING, ORDER BY, LIMIT/OFFSET)
 * onto a runtime {@link Query}.
 */
final class SolutionModifierCompiler {

    private SolutionModifierCompiler() {
    }

    /**
     * Maps the {@code SELECT} projection onto the runtime query.
     *
     * <p>{@code SELECT *} reuses the visible nodes collected from the compiled
     * query body. An explicit projection reuses these same runtime nodes and fails
     * fast when a projected variable is not visible in the body.</p>
     */
    static void applyProjection(Query query, ProjectionAst projection, WhereCompiler compiler) {
        List<Exp> selectExpressions = projection.selectAll()
                ? toNodeExpressions(query.selectNodesFromPattern())
                : buildExplicitProjection(query, projection, compiler);
        markDependentAggregates(selectExpressions);
        query.setSelectFun(selectExpressions);
        query.setSelect(selectNodeList(selectExpressions));
        query.setAggregate();
    }

    static void applyGroupBy(
            Query query, SolutionModifierAst solutionModifier, WhereCompiler compiler) {
        if (!solutionModifier.hasGroupBy()) {
            return;
        }
        GroupByAst groupByAst = solutionModifier.groupBy();
        List<Exp> groupByExpressions = new ArrayList<>();
        Set<String> usedAliases = new HashSet<>();
        int syntheticIndex = 0;
        for (TermAst term : groupByAst.expressions()) {
            Exp groupExp = toGroupByExpression(query, groupByAst, term, syntheticIndex++, usedAliases, compiler);
            groupByExpressions.add(groupExp);
        }
        query.setGroupBy(groupByExpressions);
    }

    static void applyHaving(
            Query query, SolutionModifierAst solutionModifier, WhereCompiler compiler) {
        if (!solutionModifier.hasHaving()) {
            return;
        }
        HavingAst havingAst = solutionModifier.having();
        if (havingAst.isEmpty()) {
            return;
        }
        TermAst condition = havingAst.conditions().size() == 1
                ? havingAst.conditions().getFirst()
                : new AndAst(havingAst.conditions());
        Filter filter = new AstBackedExpr(condition, compiler).getFilter();
        query.setHaving(Exp.create(Type.FILTER, filter));
    }

    /**
     * Maps {@code ORDER BY} conditions that are already expressible in runtime KGRAM terms.
     *
     * <p>Variables reuse already-visible query nodes. Other expressions are wrapped as runtime
     * filters and attached to synthetic internal nodes, just like the historical pipeline does.</p>
     */
    static void applyOrderBy(
            Query query, SolutionModifierAst solutionModifier, WhereCompiler compiler) {
        if (!solutionModifier.hasOrderBy()) {
            return;
        }
        List<Exp> orderByExpressions = new ArrayList<>();
        int syntheticIndex = 0;
        for (OrderConditionAst orderCondition : solutionModifier.orderBy()) {
            Exp orderExpression = toOrderByExpression(query, orderCondition, syntheticIndex++, compiler);
            orderExpression.status(orderCondition.orderDirection() == ASTConstants.OrderDirection.DESC);
            orderByExpressions.add(orderExpression);
        }
        query.setOrderBy(orderByExpressions);
    }

    static void applyLimitOffset(Query query, SolutionModifierAst solutionModifier) {
        if (solutionModifier.hasLimit()) {
            query.setLimit(Math.toIntExact(solutionModifier.limit()));
        }
        if (solutionModifier.hasOffset()) {
            query.setOffset(Math.toIntExact(solutionModifier.offset()));
        }
    }

    private static List<Exp> buildExplicitProjection(
            Query query, ProjectionAst projection, WhereCompiler compiler) {
        List<Exp> selectExpressions = new ArrayList<>();
        for (VarAst variable : projection.variables()) {
            TermAst expression = projection.expressionTerms().get(variable.name());
            Exp exp = expression != null
                    ? buildProjectedExpression(variable, expression, compiler)
                    : buildProjectedVariable(query, variable);
            selectExpressions.add(exp);
        }
        return selectExpressions;
    }

    private static Exp buildProjectedExpression(
            VarAst variable, TermAst expression, WhereCompiler compiler) {
        Exp selected = Exp.create(Type.NODE, compiler.termResolver().toNode(variable));
        selected.setFilter(new AstBackedExpr(expression, compiler).getFilter());
        if (new VariableScopeAnalyzer().containsAggregate(expression)) {
            selected.setAggregate(true);
        }
        return selected;
    }

    private static Exp buildProjectedVariable(Query query, VarAst variable) {
        Node node = resolveProjectedNode(query, variable.name());
        return Exp.create(Type.NODE, node);
    }

    private static Node resolveProjectedNode(Query query, String name) {
        Node node = visibleBodyNode(query, name);
        if (node == null) {
            node = groupByNode(query, name);
        }
        if (node == null) {
            throw new IllegalArgumentException(
                    "Projected variable ?" + name + " is not visible in the compiled query body");
        }
        return node;
    }

    private static void markDependentAggregates(List<Exp> selectExpressions) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Exp exp : selectExpressions) {
                if (shouldMarkAsAggregate(exp, selectExpressions)) {
                    exp.setAggregate(true);
                    changed = true;
                }
            }
        }
    }

    private static boolean shouldMarkAsAggregate(Exp exp, List<Exp> selectExpressions) {
        if (exp.isAggregate() || exp.getFilter() == null) {
            return false;
        }
        List<String> vars = exp.getFilter().getVariables();
        for (Exp other : selectExpressions) {
            if (other.isAggregate() && vars.contains(other.getNode().getLabel())) {
                return true;
            }
        }
        return false;
    }

    private static Exp toGroupByExpression(
            Query query,
            GroupByAst groupByAst,
            TermAst term,
            int syntheticIndex,
            Set<String> usedAliases,
            WhereCompiler compiler) {
        String alias = findGroupByAlias(groupByAst, term, usedAliases);
        if (alias != null) {
            usedAliases.add(alias);
            Node node = compiler.termResolver().toNode(new VarAst(alias));
            Exp exp = Exp.create(Type.NODE, node);
            exp.setFilter(new AstBackedExpr(term, compiler).getFilter());
            return exp;
        }
        if (term instanceof VarAst(String name)) {
            Node node = visibleBodyNode(query, name);
            if (node == null) {
                node = compiler.termResolver().toNode(term);
            }
            return Exp.create(Type.NODE, node);
        }
        Node node = createSyntheticGroupByNode(syntheticIndex);
        Exp exp = Exp.create(Type.NODE, node);
        exp.setFilter(new AstBackedExpr(term, compiler).getFilter());
        return exp;
    }

    private static String findGroupByAlias(GroupByAst groupByAst, TermAst term, Set<String> usedAliases) {
        for (Map.Entry<String, TermAst> entry : groupByAst.expressionTerms().entrySet()) {
            if (!usedAliases.contains(entry.getKey()) && Objects.equals(entry.getValue(), term)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Node createSyntheticGroupByNode(int syntheticIndex) {
        return NodeImpl.forVariable("__group_by_" + syntheticIndex);
    }

    private static Exp toOrderByExpression(
            Query query,
            OrderConditionAst orderCondition,
            int syntheticIndex,
            WhereCompiler compiler) {
        TermAst expression = orderCondition.expression();
        if (expression instanceof VarAst(String name)) {
            return toOrderByVarExpression(query, name);
        }
        return toOrderBySyntheticExpression(query, expression, syntheticIndex, compiler);
    }

    private static Exp toOrderByVarExpression(Query query, String name) {
        Exp selectExpression = query.getSelectExp(name);
        Node node = selectExpression != null ? selectExpression.getNode() : resolveOrderByFallbackNode(query, name);
        if (node == null) {
            throw new IllegalArgumentException(
                    "ORDER BY variable ?" + name + " is not visible in the compiled query");
        }
        Exp orderExp = Exp.create(Type.NODE, node);
        if (selectExpression != null && selectExpression.isAggregate()) {
            orderExp.setAggregate(true);
        }
        return orderExp;
    }

    private static Node resolveOrderByFallbackNode(Query query, String name) {
        Node groupNode = groupByNode(query, name);
        return groupNode != null ? groupNode : visibleBodyNode(query, name);
    }

    private static Exp toOrderBySyntheticExpression(
            Query query,
            TermAst expression,
            int syntheticIndex,
            WhereCompiler compiler) {
        Filter filter = new AstBackedExpr(expression, compiler).getFilter();
        Exp exp = Exp.create(Type.NODE, createSyntheticOrderNode(syntheticIndex));
        exp.setFilter(filter);
        if (isOrderByAggregate(query, expression, filter)) {
            exp.setAggregate(true);
        }
        return exp;
    }

    private static boolean isOrderByAggregate(Query query, TermAst expression, Filter filter) {
        if (new VariableScopeAnalyzer().containsAggregate(expression)) {
            return true;
        }
        if (filter != null) {
            for (String varName : filter.getVariables()) {
                Exp selExp = query.getSelectExp(varName);
                if (selExp != null && selExp.isAggregate()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Node createSyntheticOrderNode(int syntheticIndex) {
        return NodeImpl.forVariable("__order_by_" + syntheticIndex);
    }

    private static List<Exp> toNodeExpressions(List<Node> nodes) {
        List<Exp> expressions = new ArrayList<>();
        for (Node node : nodes) {
            expressions.add(Exp.create(Type.NODE, node));
        }
        return expressions;
    }

    private static List<Node> selectNodeList(List<Exp> selectExpressions) {
        LinkedHashSet<Node> selectNodes = new LinkedHashSet<>();
        for (Exp selectExpression : selectExpressions) {
            selectNodes.add(selectExpression.getNode());
        }
        return new ArrayList<>(selectNodes);
    }

    private static Node groupByNode(Query query, String name) {
        if (query.getGroupBy() == null) {
            return null;
        }
        for (Exp exp : query.getGroupBy()) {
            if (exp.getNode() != null && name.equals(exp.getNode().getLabel())) {
                return exp.getNode();
            }
        }
        return null;
    }

    /**
     * Resolves only variables visible from the outer body scope. This deliberately
     * excludes nodes collected from MINUS/EXISTS bodies, which KGRAM stores as query
     * nodes for internal evaluation but which are not projectable SPARQL bindings.
     */
    private static Node visibleBodyNode(Query query, String name) {
        for (Node node : query.selectNodesFromPattern()) {
            if (node.getLabel().equals(name)) {
                return node;
            }
        }
        return null;
    }
}
