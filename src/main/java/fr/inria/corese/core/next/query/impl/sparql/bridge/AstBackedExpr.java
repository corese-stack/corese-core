package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.next.query.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.kgram.adapter.BindingAdapter;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps a legacy {@link Expression} as a {@link fr.inria.corese.core.next.query.kgram.api.core.Expr}.
 *
 * <p>{@link #evalWE} forwards to the legacy tree and therefore requires:
 * <ul>
 *   <li>{@code eval} implementing {@link Computer}</li>
 *   <li>{@code b} as {@link BindingAdapter} (or raw {@link Binding} via adapter)</li>
 *   <li>{@code env} and {@code p} as legacy {@code fr.inria.corese.core.kgram.api.query.*} types</li>
 * </ul>
 */
public final class AstBackedExpr implements Expr {

    private final Expression delegate;
    private final Optional<TermAst> sourceAst;
    private final NextFilterFromAst filterView;

    public AstBackedExpr(Expression delegate) {
        this(delegate, Optional.empty());
    }

    public AstBackedExpr(Expression delegate, Optional<TermAst> sourceAst) {
        this.delegate = Objects.requireNonNull(delegate);
        this.sourceAst = sourceAst == null ? Optional.empty() : sourceAst;
        this.filterView = new NextFilterFromAst(this);
    }

    public Expression legacyExpression() {
        return delegate;
    }

    public Optional<TermAst> sourceAst() {
        return sourceAst;
    }

    private fr.inria.corese.core.kgram.api.core.Expr legacy() {
        return delegate;
    }

    private static Expr wrap(fr.inria.corese.core.kgram.api.core.Expr e) {
        if (e == null) {
            return null;
        }
        if (e instanceof Expression ex) {
            return new AstBackedExpr(ex);
        }
        throw new IllegalArgumentException("Cannot wrap as AstBackedExpr: " + e);
    }

    @Override
    public Filter getFilter() {
        return filterView;
    }

    @Override
    public Object getPattern() {
        return legacy().getPattern();
    }

    @Override
    public boolean isSystem() {
        return legacy().isSystem();
    }

    @Override
    public boolean isPublic() {
        return legacy().isPublic();
    }

    @Override
    public void setPublic(boolean b) {
        legacy().setPublic(b);
    }

    @Override
    public boolean isDynamic() {
        return legacy().isDynamic();
    }

    @Override
    public boolean isTrace() {
        return legacy().isTrace();
    }

    @Override
    public boolean isDebug() {
        return legacy().isDebug();
    }

    @Override
    public String getLabel() {
        return legacy().getLabel();
    }

    @Override
    public String getModality() {
        return legacy().getModality();
    }

    @Override
    public List<Expr> getExpList() {
        List<fr.inria.corese.core.kgram.api.core.Expr> in = legacy().getExpList();
        List<Expr> out = new ArrayList<>(in.size());
        for (fr.inria.corese.core.kgram.api.core.Expr e : in) {
            out.add(wrap(e));
        }
        return out;
    }

    @Override
    public Expr getExp(int i) {
        fr.inria.corese.core.kgram.api.core.Expr e = legacy().getExp(i);
        return e == null ? null : wrap(e);
    }

    @Override
    public void setExp(int i, Expr e) {
        if (e instanceof AstBackedExpr ab) {
            legacy().setExp(i, ab.delegate);
        } else {
            throw new IllegalArgumentException("Expr must be AstBackedExpr");
        }
    }

    @Override
    public Expr getArg() {
        fr.inria.corese.core.kgram.api.core.Expr a = legacy().getArg();
        return a == null ? null : wrap(a);
    }

    @Override
    public void setArg(Expr exp) {
        if (exp instanceof AstBackedExpr ab) {
            legacy().setArg(ab.delegate);
        } else {
            throw new IllegalArgumentException("Expr must be AstBackedExpr");
        }
    }

    @Override
    public DatatypeValue getValue() {
        return NextDatatypeValueAdapter.ofNullable(legacy().getValue());
    }

    @Override
    public DatatypeValue getDatatypeValue() {
        return NextDatatypeValueAdapter.ofNullable(legacy().getDatatypeValue());
    }

    @Override
    public int type() {
        return legacy().type();
    }

    @Override
    public int subtype() {
        return legacy().subtype();
    }

    @Override
    public void setSubtype(int n) {
        legacy().setSubtype(n);
    }

    @Override
    public int oper() {
        return legacy().oper();
    }

    @Override
    public boolean match(int oper) {
        return legacy().match(oper);
    }

    @Override
    public void setOper(int n) {
        legacy().setOper(n);
    }

    @Override
    public boolean isAggregate() {
        return legacy().isAggregate();
    }

    @Override
    public boolean isRecAggregate() {
        return legacy().isRecAggregate();
    }

    @Override
    public boolean isExist() {
        return legacy().isExist();
    }

    @Override
    public boolean isRecExist() {
        return legacy().isRecExist();
    }

    @Override
    public boolean isVariable() {
        return legacy().isVariable();
    }

    @Override
    public boolean isConstant() {
        return legacy().isConstant();
    }

    @Override
    public boolean isFuncall() {
        return legacy().isFuncall();
    }

    @Override
    public boolean isBound() {
        return legacy().isBound();
    }

    @Override
    public boolean isDistinct() {
        return legacy().isDistinct();
    }

    @Override
    public int arity() {
        return legacy().arity();
    }

    @Override
    public int getIndex() {
        return legacy().getIndex();
    }

    @Override
    public void setIndex(int index) {
        legacy().setIndex(index);
    }

    @Override
    public Expr getDefine() {
        fr.inria.corese.core.kgram.api.core.Expr d = legacy().getDefine();
        return d == null ? null : wrap(d);
    }

    @Override
    public void setDefine(Expr exp) {
        if (exp instanceof AstBackedExpr ab) {
            legacy().setDefine(ab.delegate);
        } else {
            throw new IllegalArgumentException("Expr must be AstBackedExpr");
        }
    }

    @Override
    public Expr getFunction() {
        fr.inria.corese.core.kgram.api.core.Expr f = legacy().getFunction();
        return f == null ? null : wrap(f);
    }

    @Override
    public Expr getBody() {
        fr.inria.corese.core.kgram.api.core.Expr b = legacy().getBody();
        return b == null ? null : wrap(b);
    }

    @Override
    public Expr getVariable() {
        fr.inria.corese.core.kgram.api.core.Expr v = legacy().getVariable();
        return v == null ? null : wrap(v);
    }

    @Override
    public Expr getDefinition() {
        fr.inria.corese.core.kgram.api.core.Expr d = legacy().getDefinition();
        return d == null ? null : wrap(d);
    }

    @Override
    public boolean hasMetadata(String name) {
        return legacy().hasMetadata(name);
    }

    @Override
    public IDatatype evalWE(Evaluator eval, BindingContext b, Environment env, Producer p) {
        if (!(eval instanceof Computer computer)) {
            throw new QueryEvaluationException("Evaluator must implement Computer for legacy Expression evaluation");
        }
        Binding binding = bindingFrom(b);
        if (!(env instanceof fr.inria.corese.core.kgram.api.query.Environment legacyEnv)) {
            throw new QueryEvaluationException("Environment must be legacy fr.inria.corese.core.kgram.api.query.Environment");
        }
        if (!(p instanceof fr.inria.corese.core.kgram.api.query.Producer legacyProducer)) {
            throw new QueryEvaluationException("Producer must be legacy fr.inria.corese.core.kgram.api.query.Producer");
        }
        try {
            return delegate.evalWE(computer, binding, legacyEnv, legacyProducer);
        } catch (EngineException e) {
            throw new QueryEvaluationException(e.getMessage(), e);
        }
    }

    private static Binding bindingFrom(BindingContext b) {
        if (b instanceof BindingAdapter ba) {
            return ba.delegate();
        }
        if (b instanceof Binding binding) {
            return binding;
        }
        throw new QueryEvaluationException("BindingContext must be BindingAdapter or Binding");
    }
}
