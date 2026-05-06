package fr.inria.corese.core.next.query.impl.sparql.ast;

public record AggregateAst(
        AggregateFunction function,
        boolean distinct,
        TermAst expression,
        /**
         * Lexical SEPARATOR literal for {@link AggregateFunction#GROUP_CONCAT}; {@code null} when absent
         * or irrelevant. Same conventions as SPARQL string tokens from the lexer (quotes may appear).
         */
        String groupConcatSeparator
) implements ConstraintAst {

    public AggregateAst {
        if (function == null) {
            throw new IllegalArgumentException("function is null");
        }
        if (groupConcatSeparator != null && function != AggregateFunction.GROUP_CONCAT) {
            throw new IllegalArgumentException("groupConcatSeparator only allowed for GROUP_CONCAT");
        }
    }
}