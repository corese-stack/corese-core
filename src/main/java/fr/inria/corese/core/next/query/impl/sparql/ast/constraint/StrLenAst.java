package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Function {@code STRLEN(string)} in SPARQL 1.1
 * Returns the length of a string.
 */
public class StrLenAst extends AbstractUnaryConstraintAst {

    public StrLenAst(TermAst arg) {
        super(arg);
    }

    public StrLenAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "STRLEN";
    }
}