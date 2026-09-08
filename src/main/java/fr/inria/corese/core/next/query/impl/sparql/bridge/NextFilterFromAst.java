package fr.inria.corese.core.next.query.impl.sparql.bridge;


import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BoundAst;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VariableScopeAnalyzer;

import java.util.List;
import java.util.Optional;

/**
 * {@link Filter} view exposing native AST metadata through the Corese-next {@link Expr} API.
 */
public final class NextFilterFromAst implements Filter {

    private final AstBackedExpr owner;
    private final VariableScopeAnalyzer variables = new VariableScopeAnalyzer();

    NextFilterFromAst(AstBackedExpr owner) {
        this.owner = owner;
    }

    @Override
    public List<String> getVariables() {
        return List.copyOf(variables.collectReferencedVariables(owner.sourceAst().orElseThrow()));
    }

    @Override
    public List<String> getVariables(boolean excludeLocal) {
        return getVariables();
    }

    @Override
    public Expr getExp() {
        return owner;
    }

    @Override
    public TermAst getFilterExpression() {
        return owner.sourceAst().orElseThrow();
    }

    @Override
    public boolean isBound() {
        return owner.contains(BoundAst.class);
    }

    @Override
    public boolean isAggregate() {
        return owner.isAggregate();
    }

    @Override
    public boolean isRecAggregate() {
        return owner.isRecAggregate();
    }

    @Override
    public boolean isFunctional() {
        return owner.oper() == fr.inria.corese.core.next.query.impl.engine.model.ExprType.UNNEST;
    }

    @Override
    public boolean isRecExist() {
        return owner.isRecExist();
    }

    @Override
    public Optional<TermAst> coreseNextSource() {
        return owner.sourceAst();
    }
}
