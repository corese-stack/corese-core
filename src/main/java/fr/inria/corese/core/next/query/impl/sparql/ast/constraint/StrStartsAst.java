package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code STRSTARTS(arg1, arg2)}
 * {@code STRSTARTS(string1, string2)}: returns {@code true} if {@code string1}
 * starts with {@code string2}.
 */
public class StrStartsAst extends AbstractBinaryFunctionAst implements BooleanExpressionAst {
    public StrStartsAst(List<TermAst> args) {
        super(args);
    }
}
