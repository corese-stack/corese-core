package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * Call to the REGEX function including flags
 */
public class TrinaryRegexAst implements RegexAst {
    private final TermAst stringArg;
    private final TermAst patternArg;
    private final TermAst flags;

    public TrinaryRegexAst(List<TermAst> args) {
        if(args.size() == 3) {
            Objects.requireNonNull(args.getFirst());
            Objects.requireNonNull(args.get(1));
            Objects.requireNonNull(args.getLast());

            this.stringArg = args.getFirst();
            this.patternArg = args.get(1);
            this.flags = args.getLast();
        } else {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for REGEX");
        }
    }

    public TermAst getString() {
        return this.stringArg;
    }

    public TermAst getPattern() {
        return this.patternArg;
    }

    public TermAst getFlags() {
        return this.flags;
    }

    @Override
    public String getName() {
        return "REGEX";
    }
}
