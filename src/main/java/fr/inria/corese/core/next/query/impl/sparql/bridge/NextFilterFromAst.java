package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Filter;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.List;
import java.util.Optional;

/**
 * {@link Filter} view for an {@link AstBackedExpr}, delegating metadata to the underlying
 * {@link fr.inria.corese.core.sparql.triple.parser.Expression}
 * while exposing the Corese-next {@link Expr} API.
 */
public final class NextFilterFromAst implements Filter {

    private final AstBackedExpr owner;

    NextFilterFromAst(AstBackedExpr owner) {
        this.owner = owner;
    }

    @Override
    public List<String> getVariables() {
        return owner.asTripleParserExpression().getVariables();
    }

    @Override
    public List<String> getVariables(boolean excludeLocal) {
        return owner.asTripleParserExpression().getVariables(excludeLocal);
    }

    @Override
    public Expr getExp() {
        return owner;
    }

    @Override
    public Expression getFilterExpression() {
        return owner.asTripleParserExpression();
    }

    @Override
    public boolean isBound() {
        return owner.asTripleParserExpression().isBound();
    }

    @Override
    public boolean isAggregate() {
        return owner.asTripleParserExpression().isAggregate();
    }

    @Override
    public boolean isRecAggregate() {
        return owner.asTripleParserExpression().isRecAggregate();
    }

    @Override
    public boolean isFunctional() {
        return owner.asTripleParserExpression().isFunctional();
    }

    @Override
    public boolean isRecExist() {
        return owner.asTripleParserExpression().isRecExist();
    }

    @Override
    public Optional<TermAst> coreseNextSource() {
        return owner.sourceAst();
    }
}
