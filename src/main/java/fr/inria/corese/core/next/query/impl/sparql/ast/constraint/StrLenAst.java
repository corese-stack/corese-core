package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

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