package fr.inria.corese.core.next.query.impl.sparql.bridge;


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

/** Immutable KGRAM expression backed directly by a Corese-next AST term. */
public final class AstBackedExpr implements Expr {

    private final TermAst source;
    private final WhereCompiler whereCompiler;
    private final NextFilterFromAst filterView;
    private int index = ExprType.UNBOUND;
    private int subtype = ExprType.GLOBAL;
    private int operator;
    private boolean publicExpression;

    public AstBackedExpr(TermAst source) {
        this(source, null);
    }

    AstBackedExpr(TermAst source, WhereCompiler whereCompiler) {
        this.source = Objects.requireNonNull(source, "source");
        this.whereCompiler = whereCompiler;
        this.operator = operator(source);
        this.filterView = new NextFilterFromAst(this);
    }

    public Optional<TermAst> sourceAst() {
        return Optional.of(source);
    }

    @Override
    public Filter getFilter() {
        return filterView;
    }

    @Override
    public Object getPattern() {
        return switch (source) {
            case ExistsAst(var pattern) ->
                    whereCompiler == null ? null : whereCompiler.compile(pattern);
            default -> null;
        };
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return publicExpression;
    }

    @Override
    public void setPublic(boolean value) {
        publicExpression = value;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public boolean isTrace() {
        return false;
    }

    @Override
    public boolean isDebug() {
        return false;
    }

    @Override
    public String getLabel() {
        return source.getName();
    }

    @Override
    public String getModality() {
        return null;
    }

    @Override
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
    public void setExp(int childIndex, Expr expression) {
        throw new UnsupportedOperationException("Corese-next AST expressions are immutable");
    }

    @Override
    public Expr getArg() {
        return getExpList().isEmpty() ? null : getExpList().getFirst();
    }

    @Override
    public void setArg(Expr expression) {
        throw new UnsupportedOperationException("Corese-next AST expressions are immutable");
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
    public DatatypeValue getDatatypeValue() {
        return getValue();
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
        return subtype;
    }

    @Override
    public void setSubtype(int value) {
        subtype = value;
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
    public void setOper(int value) {
        operator = value;
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
    public boolean isExist() {
        return source instanceof ExistsAst || source instanceof NotExistsAst;
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
    public boolean isFuncall() {
        return source instanceof FunctionCallAst;
    }

    @Override
    public boolean isBound() {
        return source instanceof BoundAst;
    }

    @Override
    public boolean isDistinct() {
        return source instanceof AggregateAst aggregate && aggregate.distinct();
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
    public Expr getDefine() {
        return null;
    }

    @Override
    public void setDefine(Expr expression) {
        throw new UnsupportedOperationException("Corese-next AST expressions are immutable");
    }

    @Override
    public Expr getFunction() {
        return null;
    }

    @Override
    public Expr getBody() {
        return null;
    }

    @Override
    public Expr getVariable() {
        return isVariable() ? this : null;
    }

    @Override
    public Expr getDefinition() {
        return null;
    }

    @Override
    public boolean hasMetadata(String name) {
        return false;
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
