package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopy;
import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopyable;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VariableScopeAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable KGRAM expression backed directly by a Corese-next AST term. */
public final class AstBackedExpr implements Expr, PlanCopyable<AstBackedExpr> {

    private final TermAst source;
    private final WhereCompiler whereCompiler;
    /**
     * Snapshot of the enclosing group's in-scope variables, captured at filter
     * creation time so that {@link AstBackedFilter#getVariables()} reports the
     * correct scheduling dependencies even after {@code WhereCompiler} has moved
     * to a different scope (e.g., after returning from a MINUS body).
     */
    private final Set<String> capturedInScope;
    private final AstBackedFilter filterView;
    private int index = ExprType.UNBOUND;
    private final int operator;

    public AstBackedExpr(TermAst source) {
        this(source, null);
    }

    AstBackedExpr(TermAst source, WhereCompiler whereCompiler) {
        this.source = Objects.requireNonNull(source, "source");
        this.whereCompiler = whereCompiler;
        this.capturedInScope = (whereCompiler != null)
                ? Set.copyOf(whereCompiler.inScopeVariables())
                : Set.of();
        this.operator = operator(source);
        this.filterView = new AstBackedFilter(this);
    }

    // A filter points to its compiler, whose pattern cache can point back to this filter.
    @SuppressWarnings("this-escape") // The registered shell is only linked by the copy operation, never evaluated.
    private AstBackedExpr(AstBackedExpr original, PlanCopy copies) {
        source = original.source;
        capturedInScope = original.capturedInScope;
        operator = original.operator;
        index = original.index;
        filterView = new AstBackedFilter(this);
        copies.remember(original, this);
        whereCompiler = copies.copy(original.whereCompiler);
    }

    /** Copies expression indexes and compiler caches while sharing immutable syntax. */
    @Override
    public AstBackedExpr copyPlan(PlanCopy copies) {
        return new AstBackedExpr(this, copies);
    }

    public Optional<TermAst> sourceAst() {
        return Optional.of(source);
    }

    WhereCompiler whereCompiler() {
        return whereCompiler;
    }

    /** Returns the in-scope variables as they were when this expression was compiled. */
    Set<String> capturedInScope() {
        return capturedInScope;
    }

    @Override
    public Filter getFilter() {
        return filterView;
    }

    @Override
    public Exp getPattern() {
        return switch (source) {
            case ExistsAst(var pattern) ->
                    whereCompiler == null ? null : whereCompiler.compile(pattern);
            default -> null;
        };
    }

    @Override
    public String getLabel() {
        return source.getName();
    }

    @Override
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<Expr> getExpList() {
        List<Expr> expressions = new ArrayList<>();
        for (TermAst child : children(source)) {
            expressions.add(new AstBackedExpr(child, whereCompiler));
        }
        return List.copyOf(expressions);
    }

    @Override
    public Expr getExp(int childIndex) {
        return getExpList().get(childIndex);
    }

    @Override
    public DatatypeValue getValue() {
        if (source instanceof IriAst || source instanceof LiteralAst) {
            SparqlTermResolver resolver = whereCompiler == null
                    ? new SparqlTermResolver(null)
                    : whereCompiler.termResolver();
            return resolver.toNode(source).getDatatypeValue();
        }
        return null;
    }

    @Override
    public int type() {
        return switch (source) {
            case VarAst ignored -> ExprType.VARIABLE;
            case IriAst ignored -> ExprType.CONSTANT;
            case LiteralAst ignored -> ExprType.CONSTANT;
            case BooleanExpressionAst ignored -> ExprType.BOOLEAN;
            case ConstraintAst ignored -> ExprType.FUNCTION;
        };
    }

    @Override
    public int subtype() {
        return ExprType.GLOBAL;
    }

    @Override
    public int oper() {
        return operator;
    }

    @Override
    public boolean match(int value) {
        return operator == value;
    }

    @Override
    public boolean isAggregate() {
        return source instanceof AggregateAst;
    }

    @Override
    public boolean isRecAggregate() {
        return new VariableScopeAnalyzer().containsAggregate(source);
    }

    @Override
    public boolean isRecExist() {
        return contains(ExistsAst.class) || contains(NotExistsAst.class);
    }

    @Override
    public boolean isVariable() {
        return source instanceof VarAst;
    }

    @Override
    public boolean isConstant() {
        return source instanceof IriAst || source instanceof LiteralAst;
    }

    @Override
    public int arity() {
        return children(source).size();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int value) {
        index = value;
    }

    @Override
    public DatatypeValue evalWE(
            Evaluator evaluator,
            BindingContext bindings,
            Environment environment,
            Producer producer) {
        return NativeExpressionEvaluator.evaluate(source, evaluator, environment, producer, whereCompiler);
    }

    boolean contains(Class<? extends TermAst> type) {
        if (type.isInstance(source)) {
            return true;
        }
        for (TermAst child : children(source)) {
            if (new AstBackedExpr(child, whereCompiler).contains(type)) {
                return true;
            }
        }
        return false;
    }

    private static int operator(TermAst term) {
        if (term instanceof VarAst) {
            return ExprType.VARIABLE;
        }
        if (term instanceof IriAst || term instanceof LiteralAst) {
            return ExprType.CONSTANT;
        }
        if (term instanceof FunctionCallAst call) {
            return functionOperator(call);
        }
        return switch (term) {
            case AndAst ignored -> ExprType.AND;
            case OrAst ignored -> ExprType.OR;
            case BooleanNotAst ignored -> ExprType.NOT;
            case NotExistsAst ignored -> ExprType.NOT;
            case EqualsAst ignored -> ExprType.EQ;
            case DifferentAst ignored -> ExprType.NE;
            case LowerThanAst ignored -> ExprType.LT;
            case LowerOrEqualThanAst ignored -> ExprType.LE;
            case GreaterThanAst ignored -> ExprType.GT;
            case GreaterOrEqualThanAst ignored -> ExprType.GE;
            case AddAst ignored -> ExprType.PLUS;
            case UnaryPlusAst ignored -> ExprType.PLUS;
            case SubtractAst ignored -> ExprType.MINUS;
            case UnaryMinusAst ignored -> ExprType.MINUS;
            case MultiplyAst ignored -> ExprType.MULT;
            case BoundAst ignored -> ExprType.BOUND;
            case SameTermAst ignored -> ExprType.SAMETERM;
            case LangAst ignored -> ExprType.LANG;
            case DatatypeAst ignored -> ExprType.DATATYPE;
            case BinaryRegexAst ignored -> ExprType.REGEX;
            case TrinaryRegexAst ignored -> ExprType.REGEX;
            case ExistsAst ignored -> ExprType.EXIST;
            case BnodeAst ignored -> ExprType.BNODE;
            case CoalesceAst ignored -> ExprType.COALESCE;
            case IfAst ignored -> ExprType.IF;
            case StrLenAst ignored -> ExprType.STRLEN;
            case ContainsAst ignored -> ExprType.CONTAINS;
            case ConcatAst ignored -> ExprType.CONCAT;
            case IriFunctionAst ignored -> ExprType.URI;
            default -> ExprType.UNDEF;
        };
    }

    static List<TermAst> children(TermAst term) {
        if (term instanceof UnaryConstraintAst unary) {
            return List.of(unary.argument());
        }
        if (term instanceof BinaryConstraintAst binary) {
            return List.of(binary.getLeftArgument(), binary.getRightArgument());
        }
        if (term instanceof UnlimitedArgumentsFunctionAst unlimited) {
            return unlimited.arguments();
        }
        return switch (term) {
            case AggregateAst aggregate -> aggregate.expression() == null
                    ? List.of() : List.of(aggregate.expression());
            case FunctionCallAst call -> call.arguments();
            case BnodeAst bnode -> bnode.getLabel() == null ? List.of() : List.of(bnode.getLabel());
            case TrinaryRegexAst regex -> List.of(regex.getString(), regex.getPattern(), regex.getFlags());
            case SubstrAst substring -> substring.getLength() == null
                    ? List.of(substring.getString(), substring.getStart())
                    : List.of(substring.getString(), substring.getStart(), substring.getLength());
            case ReplaceAst replace -> replace.hasFlags()
                    ? List.of(replace.getString(), replace.getPattern(), replace.getReplacement(), replace.getFlags())
                    : List.of(replace.getString(), replace.getPattern(), replace.getReplacement());
            case IfAst(var condition, var thenExpr, var elseExpr) -> List.of(condition, thenExpr, elseExpr);
            case InAst(var left, var candidates) -> prepend(left, candidates);
            case NotInAst(var left, var candidates) -> prepend(left, candidates);
            case NotExistsAst(var pattern) -> List.of(new ExistsAst(pattern));
            default -> List.of();
        };
    }

    private static int functionOperator(FunctionCallAst call) {
        if (call.functionName() instanceof IriAst(String raw)) {
            String name = raw.startsWith("<") && raw.endsWith(">")
                    ? raw.substring(1, raw.length() - 1)
                    : raw;
            if (name.equals("unnest") || name.endsWith("/unnest") || name.endsWith("#unnest")) {
                return ExprType.UNNEST;
            }
        }
        return ExprType.UNDEF;
    }

    private static List<TermAst> prepend(TermAst first, List<TermAst> remaining) {
        List<TermAst> terms = new ArrayList<>(remaining.size() + 1);
        terms.add(first);
        terms.addAll(remaining);
        return List.copyOf(terms);
    }
}
