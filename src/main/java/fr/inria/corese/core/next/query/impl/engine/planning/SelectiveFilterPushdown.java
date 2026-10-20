package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import java.util.ArrayList;
import java.util.List;

/** Conservative seed selection for a pure triple group with simple filters. */
final class SelectiveFilterPushdown {
    private SelectiveFilterPushdown() {
    }

    static int seed(Exp group, int start, int end) {
        if (!isPureGroup(group)) {
            return -1;
        }
        for (Exp expression : group) {
            if (expression.isFilter()) {
                return bindingEdge(group, expression.getFilter().getVariables().getFirst(), start, end);
            }
        }
        return -1;
    }

    private static boolean isPureGroup(Exp group) {
        for (Exp expression : group) {
            if (!isTriple(expression) && !isSimpleFilter(expression)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTriple(Exp expression) {
        return expression.isEdge() && !(expression.getEdge() instanceof PropertyPathEdge);
    }

    private static boolean isSimpleFilter(Exp expression) {
        if (!expression.isFilter() || expression.isPostpone()) {
            return false;
        }
        Expr predicate = expression.getFilter().getExp();
        return isComparison(predicate.oper()) && predicate.arity() == 2
                && predicate.getExp(0).isVariable() && predicate.getExp(1).isConstant();
    }

    private static boolean isComparison(int operator) {
        return switch (operator) {
            case ExprType.EQ, ExprType.NE, ExprType.LT, ExprType.LE, ExprType.GT, ExprType.GE -> true;
            default -> false;
        };
    }

    private static int bindingEdge(Exp group, String variable, int start, int end) {
        for (int i = start; i < end; i++) {
            Exp expression = group.get(i);
            if (binds(expression, variable)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean binds(Exp expression, String variable) {
        List<String> variables = new ArrayList<>();
        expression.addBind(variables);
        return variables.contains(variable);
    }
}
