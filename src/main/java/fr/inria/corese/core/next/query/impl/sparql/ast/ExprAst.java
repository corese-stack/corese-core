package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

public sealed interface ExprAst
        permits TermExprAst, UnaryExprAst, BinaryExprAst, FunctionCallExprAst {
}

/**
 * A SPARQL filter expression that wraps a single RDF term.
 *
 * <p>Examples of terms that can appear directly as expressions:
 * <ul>
 *   <li>Variable: {@code ?s}</li>
 *   <li>Literal: {@code "hello"}</li>
 *   <li>IRI: {@code <http://example.org/person>}</li>
 * </ul>
 *
 * @param term the RDF term wrapped by this expression; must not be {@code null}
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
