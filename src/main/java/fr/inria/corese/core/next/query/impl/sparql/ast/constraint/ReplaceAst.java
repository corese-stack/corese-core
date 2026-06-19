package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * Call to the SPARQL {@code REPLACE} function.
 * Supports {@code REPLACE(arg, pattern, replacement)} and
 * {@code REPLACE(arg, pattern, replacement, flags)}.
 */
public class ReplaceAst implements SimpleLiteralExpressionAst {
    private final TermAst stringArg;
    private final TermAst patternArg;
    private final TermAst replacementArg;
    private final TermAst flagsArg;

    public ReplaceAst(List<TermAst> args) {
        if (args.size() == 3 || args.size() == 4) {
            Objects.requireNonNull(args.get(0));
            Objects.requireNonNull(args.get(1));
            Objects.requireNonNull(args.get(2));

            this.stringArg = args.get(0);
            this.patternArg = args.get(1);
            this.replacementArg = args.get(2);
            this.flagsArg = args.size() == 4 ? Objects.requireNonNull(args.get(3)) : null;
        } else {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for REPLACE");
        }
    }

    public TermAst getString() {
        return stringArg;
    }

    public TermAst getPattern() {
        return patternArg;
    }

    public TermAst getReplacement() {
        return replacementArg;
    }

    public TermAst getFlags() {
        return flagsArg;
    }

    public boolean hasFlags() {
        return flagsArg != null;
    }

    @Override
    public String getName() {
        return "REPLACE";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.stringArg.accept(visitor);
        this.patternArg.accept(visitor);
        this.replacementArg.accept(visitor);
        if(flagsArg != null) {
            this.flagsArg.accept(visitor);
        }
    }
}
