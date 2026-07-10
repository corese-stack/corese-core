package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.next.query.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.kgram.adapter.BindingAdapter;
import fr.inria.corese.core.next.query.kgram.adapter.TripleParserEvalSupport;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps a {@link fr.inria.corese.core.sparql.triple.parser.Expression} (SPARQL interpreter tree) as a
 * {@link fr.inria.corese.core.next.query.kgram.api.core.Expr}.
 *
 */
public final class AstBackedExpr implements Expr {

    private final Expression delegate;
    private final Optional<TermAst> sourceAst;
    private final NextFilterFromAst filterView;

    public AstBackedExpr(Expression delegate) {
        this(delegate, Optional.empty());
    }

    public AstBackedExpr(Expression delegate, Optional<TermAst> sourceAst) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst");
        this.filterView = new NextFilterFromAst(this);
    }

    /**
     * The underlying SPARQL interpreter {@link Expression} (triple.parser), for metadata and {@link Filter#getFilterExpression()}.
     */
    public Expression asTripleParserExpression() {
        return delegate;
    }

    public Optional<TermAst> sourceAst() {
        return sourceAst;
    }

    private static Expr wrapInterpreterSubexpr(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Expression ex) {
            return new AstBackedExpr(ex);
        }
        throw new IllegalArgumentException("Cannot wrap as AstBackedExpr: " + node);
    }

    @Override
    public Filter getFilter() {
        return filterView;
    }

    @Override
    public Object getPattern() {
        if (delegate instanceof AstBackedExistTerm exist) {
            return exist.compiledPattern();
        }
        return delegate.getPattern();
    }

    @Override
    public boolean isSystem() {
        return delegate.isSystem();
    }

    @Override
    public boolean isPublic() {
        return delegate.isPublic();
    }

    @Override
    public void setPublic(boolean b) {
        delegate.setPublic(b);
    }

    @Override
    public boolean isDynamic() {
        return delegate.isDynamic();
    }

    @Override
    public boolean isTrace() {
        return delegate.isTrace();
    }

    @Override
    public boolean isDebug() {
        return delegate.isDebug();
    }

    @Override
    public String getLabel() {
        return delegate.getLabel();
    }

    @Override
    public String getModality() {
        return delegate.getModality();
    }

    @Override
    public List<Expr> getExpList() {
        List<?> in = delegate.getExpList();
        List<Expr> out = new ArrayList<>(in.size());
        for (Object o : in) {
            out.add(wrapInterpreterSubexpr(o));
        }
        return out;
    }

    @Override
    public Expr getExp(int i) {
        return wrapInterpreterSubexpr(delegate.getExp(i));
    }

    @Override
    public void setExp(int i, Expr e) {
        if (e instanceof AstBackedExpr ab) {
            delegate.setExp(i, ab.delegate);
        } else {
            throw new IllegalArgumentException("Expr must be AstBackedExpr");
        }
    }

    @Override
    public Expr getArg() {
        return wrapInterpreterSubexpr(delegate.getArg());
    }

    @Override
    public void setArg(Expr exp) {
        if (exp instanceof AstBackedExpr ab) {
            delegate.setArg(ab.delegate);
        } else {
            throw new IllegalArgumentException("Expr must be AstBackedExpr");
        }
    }

    @Override
    public DatatypeValue getValue() {
        return NextDatatypeValueAdapter.ofNullable(delegate.getValue());
    }

    @Override
    public DatatypeValue getDatatypeValue() {
        return NextDatatypeValueAdapter.ofNullable(delegate.getDatatypeValue());
    }

    @Override
    public int type() {
        return delegate.type();
    }

    @Override
    public int subtype() {
        return delegate.subtype();
    }

    @Override
    public void setSubtype(int n) {
        delegate.setSubtype(n);
    }

    @Override
    public int oper() {
        return delegate.oper();
    }

    @Override
    public boolean match(int oper) {
        return delegate.match(oper);
    }

    @Override
    public void setOper(int n) {
        delegate.setOper(n);
    }

    @Override
    public boolean isAggregate() {
        return delegate.isAggregate();
    }

    @Override
    public boolean isRecAggregate() {
        return delegate.isRecAggregate();
    }

    @Override
    public boolean isExist() {
        return delegate.isExist();
    }

    @Override
    public boolean isRecExist() {
        return delegate.isRecExist();
    }

    @Override
    public boolean isVariable() {
        return delegate.isVariable();
    }

    @Override
    public boolean isConstant() {
        return delegate.isConstant();
    }

    @Override
    public boolean isFuncall() {
        return delegate.isFuncall();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isDistinct() {
        return delegate.isDistinct();
    }

    @Override
    public int arity() {
        return delegate.arity();
    }

    @Override
    public int getIndex() {
        return delegate.getIndex();
    }

    @Override
    public void setIndex(int index) {
        delegate.setIndex(index);
    }

    @Override
    public Expr getDefine() {
        return wrapInterpreterSubexpr(delegate.getDefine());
    }

    @Override
    public void setDefine(Expr exp) {
        if (exp instanceof AstBackedExpr ab) {
            delegate.setDefine(ab.delegate);
        } else {
            throw new IllegalArgumentException("Expr must be AstBackedExpr");
        }
    }

    @Override
    public Expr getFunction() {
        return wrapInterpreterSubexpr(delegate.getFunction());
    }

    @Override
    public Expr getBody() {
        return wrapInterpreterSubexpr(delegate.getBody());
    }

    @Override
    public Expr getVariable() {
        return wrapInterpreterSubexpr(delegate.getVariable());
    }

    @Override
    public Expr getDefinition() {
        return wrapInterpreterSubexpr(delegate.getDefinition());
    }

    @Override
    public boolean hasMetadata(String name) {
        return delegate.hasMetadata(name);
    }

    @Override
    public IDatatype evalWE(Evaluator eval, BindingContext b, Environment env, Producer p) {
        if (!(eval instanceof Computer computer)) {
            throw new QueryEvaluationException("Evaluator must implement Computer for triple.parser Expression evaluation");
        }
        Binding binding = bindingFrom(b);
        return TripleParserEvalSupport.evalWE(delegate, computer, binding, env, p);
    }

    private static Binding bindingFrom(BindingContext b) {
        if (b instanceof BindingAdapter(Binding delegate1)) {
            return delegate1;
        }
        if (b instanceof Binding binding) {
            return binding;
        }
        throw new QueryEvaluationException("BindingContext must be BindingAdapter or Binding");
    }
}
