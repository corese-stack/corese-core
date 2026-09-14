package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BoundAst;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Filter view exposing native AST metadata through the Corese-next expression API. */
public final class NextFilterFromAst implements Filter {

    private final AstBackedExpr owner;

    NextFilterFromAst(AstBackedExpr owner) {
        this.owner = owner;
    }

    @Override
    public List<String> getVariables() {
        // Scheduling dependencies include variables inside EXISTS patterns (including
        // GRAPH names and inner FILTERs) only if they are bound in the enclosing query scope.
        // Purely local/existential variables of EXISTS must not be declared, so they do not
        // cause postponement of the filter in OPTIONAL patterns (KGRAM Exp.optional/simpleBind).
        Set<String> names = new LinkedHashSet<>();
        Set<String> inScope = (owner.whereCompiler() != null)
                ? owner.whereCompiler().inScopeVariables()
                : Set.of();
        boolean recExist = owner.isRecExist();
        owner.sourceAst().orElseThrow().accept(new AbstractAstVisitor() {
            @Override
            public void visit(TermAst term) {
                if (term instanceof VarAst(String name) && (!recExist || inScope.isEmpty() || inScope.contains(name))) {
                    names.add(name);
                }
            }
        });
        return List.copyOf(names);
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
