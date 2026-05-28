package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * Function {@code SUBSTR(string, start)} or {@code SUBSTR(string, start, length)}.
 */
public final class SubstrAst implements SimpleLiteralExpressionAst {
    private final TermAst stringArg;
    private final TermAst startArg;
    private final TermAst lengthArg;

    public SubstrAst(List<TermAst> args) {
        if (args.size() != 2 && args.size() != 3) {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for SUBSTR");
        }

        this.stringArg = Objects.requireNonNull(args.getFirst());
        this.startArg = Objects.requireNonNull(args.get(1));
        this.lengthArg = args.size() == 3 ? Objects.requireNonNull(args.getLast()) : null;
    }

    public TermAst getString() {
        return this.stringArg;
    }

    public TermAst getStart() {
        return this.startArg;
    }

    public TermAst getLength() {
        return this.lengthArg;
    }

    @Override
    public String getName() {
        return "SUBSTR";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.stringArg.accept(visitor);
        this.startArg.accept(visitor);
        this.lengthArg.accept(visitor);
    }
}
