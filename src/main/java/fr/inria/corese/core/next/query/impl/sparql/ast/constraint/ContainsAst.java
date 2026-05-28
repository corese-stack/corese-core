package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code CONTAINS(arg1, arg2)}
 * {@code CONTAINS(string1, string2)}: returns {@code true} if {@code string1}
 * contains {@code string2}.
 */
public class ContainsAst extends AbstractBinaryFunctionAst implements BooleanExpressionAst {
    public ContainsAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "CONTAINS";
    }
}
