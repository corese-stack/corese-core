package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code sameTerm(term1, term2)}
 */
public class SameTermAst extends AbstractBinaryFunctionAst implements BooleanExpressionAst {

    public SameTermAst(List<TermAst> args) {
        super(args);
        if (args.size() != 2) {
            throw new QuerySyntaxException("Unexpected number of arguments for sameTerm function");
        }
    }

    @Override
    public String getName() {
        return "SAMETERM";
    }
}
