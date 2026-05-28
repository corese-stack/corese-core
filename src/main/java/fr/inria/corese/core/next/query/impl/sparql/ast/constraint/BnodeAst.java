package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.ExprAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * Function {@code BNODE()} or {@code BNODE(label)} in SPARQL 1.1.
 */
public final class BnodeAst implements ExprAst {
    private final TermAst label;

    public BnodeAst(List<TermAst> args) {
        if (args.size() > 1) {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for BNODE");
        }
        this.label = args.isEmpty() ? null : Objects.requireNonNull(args.getFirst());
    }

    public TermAst getLabel() {
        return this.label;
    }

    @Override
    public String getName() {
        return "BNODE";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.label.accept(visitor);
    }
}
