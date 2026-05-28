package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * Call to the REGEX function with a tested string and a pattern
 */
public class BinaryRegexAst extends AbstractBinaryFunctionAst implements RegexAst {

    public BinaryRegexAst(List<TermAst> args) {
        super(args);
        if(args.size() == 2) {
            Objects.requireNonNull(args.getFirst());
            Objects.requireNonNull(args.getLast());

            this.setLeftArgument(args.getFirst());
            this.setRightArgument(args.getLast());
        } else {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for REGEX");
        }
    }

    public TermAst getString() {
        return this.getLeftArgument();
    }

    public TermAst getPattern() {
        return this.getRightArgument();
    }

    @Override
    public String getName() {
        return "REGEX";
    }
}
