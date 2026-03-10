package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

public sealed interface ExprAst
        permits TermExprAst, UnaryExprAst, BinaryExprAst, FunctionCallExprAst {
}

/**
 * ?s
 * "hello"
 * <http://example.org/person>
 * @param term
 */
record TermExprAst(TermAst term) implements ExprAst {
    public TermExprAst {
        if (term == null) throw new IllegalArgumentException("term is null");
    }
}

/**
 * ! ?x
 * - ?x
 * @param operator
 * @param operand
 */
record UnaryExprAst(UnaryOperator operator, ExprAst operand) implements ExprAst { }

/**
 * ?x + 5
 * ?x = ?y
 * ?age > 18
 * ?x && ?y
 * @param operator
 * @param left
 * @param right
 */
record BinaryExprAst(BinaryOperator operator, ExprAst left, ExprAst right) implements ExprAst { }

/**
 * LANG(?label)
 * STR(?uri)
 * BOUND(?x)
 * REGEX(?label, "abc")
 * @param functionName
 * @param arguments
 */
record FunctionCallExprAst(BuiltinFunction functionName, List<ExprAst> arguments) implements ExprAst {
    public FunctionCallExprAst {
        arguments = arguments != null ? List.copyOf(arguments) : List.of();
    }
}
