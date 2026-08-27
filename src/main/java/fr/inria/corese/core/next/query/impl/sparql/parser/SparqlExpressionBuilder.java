package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.function.Supplier;

/**
 * Expression and constraint building helpers extracted from {@link SparqlAstBuilder}.
 * <p>
 * The only runtime dependency on {@link SparqlAstBuilder} state is the captured
 * EXISTS/NOT EXISTS group-graph-pattern, accessed through the {@code existsPatternSupplier}
 * callback injected at construction time.
 */
public final class SparqlExpressionBuilder {

    private final SparqlTermBuilder terms;
    private final Supplier<GroupGraphPatternAst> existsPatternSupplier;

    public SparqlExpressionBuilder(SparqlTermBuilder terms,
                                   Supplier<GroupGraphPatternAst> existsPatternSupplier) {
        this.terms = terms;
        this.existsPatternSupplier = existsPatternSupplier;
    }

    // --- Constraint / function-call factories ---

    /**
     * Creates the right AST node for a built-in SPARQL operator or function.
     */
    public ConstraintAst createConstraint(ASTConstants.Constraint constraint, List<TermAst> args) {
        if (constraint instanceof ASTConstants.OPERATOR op) {
            return createOperatorConstraint(op, args);
        }
        if (constraint instanceof ASTConstants.FUNCTION_CALL fn) {
            return createFunctionCallConstraint(fn, args);
        }
        throw new QueryEvaluationException(
                "Unexpected constraint (" + constraint + ") with arguments: " + args);
    }

    private ConstraintAst createOperatorConstraint(ASTConstants.OPERATOR op, List<TermAst> args) {
        return switch (op) {
            case NOT -> new BooleanNotAst(args);
            case PLUS -> new UnaryPlusAst(args);
            case MINUS -> new UnaryMinusAst(args);
            case OR -> new OrAst(args);
            case AND -> new AndAst(args);
            case EQ -> new EqualsAst(args);
            case NE -> new DifferentAst(args);
            case LT -> new LowerThanAst(args);
            case LE -> new LowerOrEqualThanAst(args);
            case GT -> new GreaterThanAst(args);
            case GE -> new GreaterOrEqualThanAst(args);
            case MUL -> new MultiplyAst(args);
            case DIV -> new DivideAst(args);
            case ADD -> new AddAst(args);
            case SUB -> new SubtractAst(args);
        };
    }

    private ConstraintAst createFunctionCallConstraint(ASTConstants.FUNCTION_CALL fn, List<TermAst> args) {
        return switch (fn) {
            case BOUND -> new BoundAst(args);
            case IS_IRI -> new IsIriAst(args);
            case IS_BLANK -> new IsBlankAst(args);
            case IS_LITERAL -> new IsLiteralAst(args);
            case STR -> new StrAst(args);
            case UCASE -> new UcaseAst(args);
            case LCASE -> new LcaseAst(args);
            case LANG -> new LangAst(args);
            case STRDT -> new StrDtAst(args);
            case STRLANG -> new StrLangAst(args);
            case DATATYPE -> new DatatypeAst(args);
            case IRI -> new IriFunctionAst(args);
            case BNODE -> new BnodeAst(args);
            case SAMETERM -> new SameTermAst(args);
            case LANGMATCHES -> new LangMatchesAst(args);
            case CONTAINS -> new ContainsAst(args);
            case STRSTARTS -> new StrStartsAst(args);
            case STRENDS -> new StrEndsAst(args);
            case SUBSTR -> new SubstrAst(args);
            case CONCAT -> new ConcatAst(args);
            case STRBEFORE -> new StrBeforeAst(args);
            case STRAFTER -> new StrAfterAst(args);
            case REPLACE -> new ReplaceAst(args);
            case REGEX -> {
                if (args.size() == 2) {
                    yield new BinaryRegexAst(args);
                } else if (args.size() == 3) {
                    yield new TrinaryRegexAst(args);
                } else {
                    throw new QueryEvaluationException("Unexpected number of arguments (3) for REGEX keyword");
                }
            }
        };
    }

    public ConstraintAst createFunCall(IriAst functionName, List<TermAst> args) {
        return new FunctionCallAst(functionName, args);
    }

    public AggregateAst createAggregate(
            AggregateFunction function, boolean distinct, TermAst expression, String groupConcatSeparator) {
        return new AggregateAst(function, distinct, expression, groupConcatSeparator);
    }

    // --- ANTLR expression converters ---

    public TermAst termFromExpression(SparqlParser.ExpressionContext ctx) {
        if (ctx.conditionalOrExpression() != null) {
            return termFromConditionalOr(ctx.conditionalOrExpression());
        } else {
            throw new QueryEvaluationException("No conditional OR found");
        }
    }

    public TermAst termFromConditionalOr(SparqlParser.ConditionalOrExpressionContext ctx) {
        if (ctx.conditionalAndExpression() != null && !ctx.conditionalAndExpression().isEmpty()) {
            if (ctx.conditionalAndExpression().size() > 1) {
                List<TermAst> args = ctx.conditionalAndExpression().stream()
                        .map(this::termFromConditionalAnd).toList();
                return createConstraint(ASTConstants.OPERATOR.OR, args);
            } else {
                return termFromConditionalAnd(ctx.conditionalAndExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No conditional AND  found");
        }
    }

    public TermAst termFromConditionalAnd(SparqlParser.ConditionalAndExpressionContext ctx) {
        if (ctx.valueLogical() != null && !ctx.valueLogical().isEmpty()) {
            if (ctx.valueLogical().size() > 1) {
                List<TermAst> args = ctx.valueLogical().stream().map(this::termFromValueLogical).toList();
                return createConstraint(ASTConstants.OPERATOR.AND, args);
            } else {
                return termFromValueLogical(ctx.valueLogical().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No logical value found");
        }
    }

    public TermAst termFromValueLogical(SparqlParser.ValueLogicalContext ctx) {
        if (ctx.relationalExpression() != null) {
            return termFromRelational(ctx.relationalExpression());
        } else {
            throw new QueryEvaluationException("No relational termFromExpression found");
        }
    }

    public TermAst termFromRelational(SparqlParser.RelationalExpressionContext ctx) {
        if (ctx.numericExpression() == null || ctx.numericExpression().isEmpty()) {
            throw new QueryEvaluationException("No numeric termFromExpression found");
        }
        TermAst lhs = termFromNumeric(ctx.numericExpression().getFirst());

        if (ctx.IN() != null) {
            List<TermAst> candidates = termFromExpressionList(ctx.expressionList());
            if (ctx.NOT() != null) {
                return new NotInAst(lhs, candidates);
            }
            return new InAst(lhs, candidates);
        }

        if (ctx.numericExpression().size() > 1) {
            ASTConstants.OPERATOR op;
            if (ctx.EQUAL() != null) {
                op = ASTConstants.OPERATOR.EQ;
            } else if (ctx.NOT_EQUAL() != null) {
                op = ASTConstants.OPERATOR.NE;
            } else if (ctx.LESS() != null) {
                op = ASTConstants.OPERATOR.LT;
            } else if (ctx.LESS_OR_EQUAL() != null) {
                op = ASTConstants.OPERATOR.LE;
            } else if (ctx.GREATER() != null) {
                op = ASTConstants.OPERATOR.GT;
            } else if (ctx.GREATER_OR_EQUAL() != null) {
                op = ASTConstants.OPERATOR.GE;
            } else {
                throw new QueryEvaluationException("Unexpected operator in relational termFromExpression");
            }
            List<TermAst> args = ctx.numericExpression().stream().map(this::termFromNumeric).toList();
            return createConstraint(op, args);
        }

        return lhs;
    }

    /**
     * Right-hand side of {@code IN} / {@code NOT IN}: {@code NIL} ({@code ()}) or parenthesized expression list.
     */
    public List<TermAst> termFromExpressionList(SparqlParser.ExpressionListContext ctx) {
        if (ctx == null) {
            throw new QueryEvaluationException("expressionList missing for IN / NOT IN");
        }
        if (ctx.NIL() != null) {
            return List.of();
        }
        if (ctx.expression() != null && !ctx.expression().isEmpty()) {
            return ctx.expression().stream().map(this::termFromExpression).toList();
        }
        return List.of();
    }

    public TermAst termFromNumeric(SparqlParser.NumericExpressionContext ctx) {
        if (ctx.additiveExpression() != null) {
            return termFromAdditive(ctx.additiveExpression());
        } else {
            throw new QueryEvaluationException("No additive termFromExpression found");
        }
    }

    public TermAst termFromAdditive(SparqlParser.AdditiveExpressionContext ctx) {
        if (ctx.multiplicativeExpression() == null || ctx.multiplicativeExpression().isEmpty()) {
            throw new QueryEvaluationException("No multiplicative termFromExpression found");
        }
        if (ctx.multiplicativeExpression().size() == 1
                && ctx.numericLiteralNegative().isEmpty()
                && ctx.numericLiteralPositive().isEmpty()) {
            return termFromMultiplicative(ctx.multiplicativeExpression().getFirst());
        }

        TermAst leftHand = termFromMultiplicative(ctx.multiplicativeExpression().getFirst());
        ASTConstants.OPERATOR op = ASTConstants.OPERATOR.ADD;
        for (int i = 1; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                op = parseAdditiveOperator(child);
            } else {
                TermAst rightHand = extractAdditiveOperand(child, ctx.getText());
                leftHand = createConstraint(op, List.of(leftHand, rightHand));
            }
        }
        return leftHand;
    }

    private static ASTConstants.OPERATOR parseAdditiveOperator(ParseTree child) {
        if ("+".equals(child.getText())) {
            return ASTConstants.OPERATOR.ADD;
        }
        if ("-".equals(child.getText())) {
            return ASTConstants.OPERATOR.SUB;
        }
        throw new QueryEvaluationException(
                "Unexpected operator in additive termFromExpression " + child.getText());
    }

    private TermAst extractAdditiveOperand(ParseTree child, String contextText) {
        return switch (child) {
            case SparqlParser.MultiplicativeExpressionContext c ->
                    termFromMultiplicative(c);
            case SparqlParser.NumericLiteralPositiveContext c ->
                    terms.termFromNumericLiteralPositive(c);
            case SparqlParser.NumericLiteralNegativeContext c ->
                    terms.termFromNumericLiteralNegative(c);
            case null, default ->
                    throw new QueryEvaluationException(
                            "Unexpected left hand termFromExpression in additive termFromExpression "
                                    + contextText
                                    + " "
                                    + (child != null ? child.getText() : "null")
                    );
        };
    }

    public TermAst termFromMultiplicative(SparqlParser.MultiplicativeExpressionContext ctx) {
        if (ctx.unaryExpression() == null || ctx.unaryExpression().isEmpty()) {
            throw new QueryEvaluationException("No unary termFromExpression found");
        }
        if (ctx.unaryExpression().size() == 1) {
            return termFromUnary(ctx.unaryExpression().getFirst());
        }

        TermAst head = termFromUnary(ctx.unaryExpression().getFirst());
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            ASTConstants.OPERATOR op = parseMultiplicativeOperator(ctx.getChild(i));
            ParseTree rightHandContext = ctx.getChild(i + 1);
            if (op == null || !(rightHandContext instanceof SparqlParser.UnaryExpressionContext uec)) {
                throw new QuerySyntaxException(
                        "Unexpected operator or right hand content in " + ctx.getText());
            }
            TermAst rightHand = termFromUnary(uec);
            head = createConstraint(op, List.of(head, rightHand));
        }
        return head;
    }

    private static ASTConstants.OPERATOR parseMultiplicativeOperator(ParseTree tree) {
        if (tree instanceof TerminalNode terminalNode) {
            if ("*".equals(terminalNode.getText())) {
                return ASTConstants.OPERATOR.MUL;
            }
            if ("/".equals(terminalNode.getText())) {
                return ASTConstants.OPERATOR.DIV;
            }
        }
        return null;
    }

    public TermAst termFromUnary(SparqlParser.UnaryExpressionContext ctx) {
        ASTConstants.Constraint op = null;
        if (ctx.PLUS() != null) {
            op = ASTConstants.OPERATOR.PLUS;
        } else if (ctx.MINUS_SIGN() != null) {
            op = ASTConstants.OPERATOR.MINUS;
        } else if (ctx.EXCLAMATION() != null) {
            op = ASTConstants.OPERATOR.NOT;
        }
        if (op != null) {
            return createConstraint(op, List.of(termFromPrimary(ctx.primaryExpression())));
        } else {
            return termFromPrimary(ctx.primaryExpression());
        }
    }

    public TermAst termFromBrackettedExpression(SparqlParser.BrackettedExpressionContext ctx) {
        return termFromExpression(ctx.expression());
    }

    public TermAst termFromRegex(SparqlParser.RegexExpressionContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.REGEX, args);
        } else {
            throw new QueryEvaluationException("Unexpected arguments for REGEX call");
        }
    }

    public TermAst termFromReplace(SparqlParser.StrReplaceExpressionContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.REPLACE, args);
        } else {
            throw new QueryEvaluationException("Unexpected arguments for REPLACE call");
        }
    }

    /**
     * Converts a {@code builtInCall} parse-tree node to the corresponding
     * {@link TermAst} / {@link ConstraintAst}.
     *
     * @param ctx the ANTLR parse-tree node for a {@code builtInCall} rule
     * @return the AST node representing the built-in call
     * @throws QueryEvaluationException if the function keyword is unknown or the
     *                                  EXISTS/NOT EXISTS pattern was not captured
     */
    public TermAst termFromBuiltInCall(SparqlParser.BuiltInCallContext ctx) {
        TermAst patternOrAgg = termFromPatternOrAggregateBuiltInCall(ctx);
        if (patternOrAgg != null) {
            return patternOrAgg;
        }
        TermAst condOrCtor = termFromConditionOrConstructorBuiltInCall(ctx);
        if (condOrCtor != null) {
            return condOrCtor;
        }
        TermAst zeroArg = termFromZeroArgBuiltInCall(ctx);
        if (zeroArg != null) {
            return zeroArg;
        }
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            TermAst dtOrNum = termFromDateTimeOrNumericBuiltInCall(ctx, args);
            if (dtOrNum != null) {
                return dtOrNum;
            }
            TermAst strCall = termFromStringBuiltInCall(ctx, args);
            if (strCall != null) {
                return strCall;
            }
            TermAst typeOrHash = termFromTypeOrHashBuiltInCall(ctx, args);
            if (typeOrHash != null) {
                return typeOrHash;
            }
            throw new QueryEvaluationException(
                    "Unexpected function for a  BuiltInCall for token " + ctx.getText());
        }
        throw new QueryEvaluationException(
                "Unable to resolve BuiltInCall for token " + ctx.getText());
    }

    private TermAst termFromPatternOrAggregateBuiltInCall(SparqlParser.BuiltInCallContext ctx) {
        if (ctx.aggregate() != null) {
            return termFromAggregate(ctx.aggregate());
        }
        if (ctx.existsFunc() != null) {
            GroupGraphPatternAst existsPattern = existsPatternSupplier.get();
            if (existsPattern == null) {
                throw new QueryEvaluationException(
                        "EXISTS { ... } inner pattern was not captured; check listener order");
            }
            return new ExistsAst(existsPattern);
        }
        if (ctx.notExistsFunc() != null) {
            GroupGraphPatternAst notExistsPattern = existsPatternSupplier.get();
            if (notExistsPattern == null) {
                throw new QueryEvaluationException(
                        "NOT EXISTS { ... } inner pattern was not captured; check listener order");
            }
            return new NotExistsAst(notExistsPattern);
        }
        if (ctx.regexExpression() != null) {
            return termFromRegex(ctx.regexExpression());
        }
        if (ctx.strReplaceExpression() != null) {
            return termFromReplace(ctx.strReplaceExpression());
        }
        return null;
    }

    private TermAst termFromConditionOrConstructorBuiltInCall(SparqlParser.BuiltInCallContext ctx) {
        if (ctx.BOUND() != null) {
            return createConstraint(ASTConstants.FUNCTION_CALL.BOUND,
                    List.of(terms.variable(ctx.var_().getText())));
        }
        if (ctx.BNODE() != null) {
            List<TermAst> args = ctx.expression() == null
                    ? List.of()
                    : ctx.expression().stream().map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.BNODE, args);
        }
        if (ctx.IF() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return new IfAst(args.get(0), args.get(1), args.get(2));
        }
        if (ctx.CONCAT() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.CONCAT, args);
        }
        if (ctx.COALESCE() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return new CoalesceAst(args);
        }
        if (ctx.subStringExpression() != null) {
            List<TermAst> args = ctx.subStringExpression().expression().stream()
                    .map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.SUBSTR, args);
        }
        return null;
    }

    private static TermAst termFromZeroArgBuiltInCall(SparqlParser.BuiltInCallContext ctx) {
        if (ctx.RAND() != null) {
            return new RandAst();
        }
        if (ctx.UUID() != null) {
            return new UuidAst();
        }
        if (ctx.STRUUID() != null) {
            return new StrUuidAst();
        }
        if (ctx.NOW() != null) {
            return new NowAst();
        }
        return null;
    }

    private static TermAst termFromDateTimeOrNumericBuiltInCall(
            SparqlParser.BuiltInCallContext ctx, List<TermAst> args) {
        if (ctx.YEAR() != null) return new YearAst(args);
        if (ctx.MONTH() != null) return new MonthAst(args);
        if (ctx.DAY() != null) return new DayAst(args);
        if (ctx.HOURS() != null) return new HoursAst(args);
        if (ctx.MINUTES() != null) return new MinutesAst(args);
        if (ctx.SECONDS() != null) return new SecondsAst(args);
        if (ctx.TIMEZONE() != null) return new TimezoneAst(args);
        if (ctx.TZ() != null) return new TzAst(args);
        if (ctx.ABS() != null) return new AbsAst(args);
        if (ctx.CEIL() != null) return new CeilAst(args);
        if (ctx.FLOOR() != null) return new FloorAst(args);
        if (ctx.ROUND() != null) return new RoundAst(args);
        if (ctx.STRLEN() != null) return new StrLenAst(args.getFirst());
        return null;
    }

    private TermAst termFromStringBuiltInCall(
            SparqlParser.BuiltInCallContext ctx, List<TermAst> args) {
        if (ctx.STR() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STR, args);
        if (ctx.UCASE() != null) return createConstraint(ASTConstants.FUNCTION_CALL.UCASE, args);
        if (ctx.LCASE() != null) return createConstraint(ASTConstants.FUNCTION_CALL.LCASE, args);
        if (ctx.ENCODE_FOR_URI() != null) return new EncodeForUriAst(args);
        if (ctx.LANG() != null) return createConstraint(ASTConstants.FUNCTION_CALL.LANG, args);
        if (ctx.LANGMATCHES() != null) return new LangMatchesAst(args);
        if (ctx.CONTAINS() != null) return createConstraint(ASTConstants.FUNCTION_CALL.CONTAINS, args);
        if (ctx.STRSTARTS() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STRSTARTS, args);
        if (ctx.STRENDS() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STRENDS, args);
        if (ctx.STRDT() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STRDT, args);
        if (ctx.STRLANG() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STRLANG, args);
        if (ctx.STRBEFORE() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STRBEFORE, args);
        if (ctx.STRAFTER() != null) return createConstraint(ASTConstants.FUNCTION_CALL.STRAFTER, args);
        return null;
    }

    private TermAst termFromTypeOrHashBuiltInCall(
            SparqlParser.BuiltInCallContext ctx, List<TermAst> args) {
        if (ctx.DATATYPE() != null) return createConstraint(ASTConstants.FUNCTION_CALL.DATATYPE, args);
        if (ctx.IRI() != null || ctx.URI() != null) return createConstraint(ASTConstants.FUNCTION_CALL.IRI, args);
        if (ctx.SAME_TERM() != null) return createConstraint(ASTConstants.FUNCTION_CALL.SAMETERM, args);
        if (ctx.IS_URI() != null || ctx.IS_IRI() != null) return createConstraint(ASTConstants.FUNCTION_CALL.IS_IRI, args);
        if (ctx.IS_BLANK() != null) return createConstraint(ASTConstants.FUNCTION_CALL.IS_BLANK, args);
        if (ctx.IS_LITERAL() != null) return createConstraint(ASTConstants.FUNCTION_CALL.IS_LITERAL, args);
        if (ctx.MD5() != null) return new Md5Ast(args);
        if (ctx.SHA1() != null) return new Sha1Ast(args);
        if (ctx.SHA256() != null) return new Sha256Ast(args);
        if (ctx.SHA384() != null) return new Sha384Ast(args);
        if (ctx.SHA512() != null) return new Sha512Ast(args);
        return null;
    }

    public TermAst termFromConstraint(SparqlParser.ConstraintContext ctx) {
        if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        } else if (ctx.functionCall() != null) {
            IriAst functionTermAst = new IriAst(ctx.functionCall().iriRef().getText());
            List<TermAst> args = ctx.functionCall().argList().expression().stream()
                    .map(this::termFromExpression).toList();
            return new FunctionCallAst(functionTermAst, args);
        } else if (ctx.brackettedExpression() != null
                && ctx.brackettedExpression().expression() != null) {
            return termFromBrackettedExpression(ctx.brackettedExpression());
        } else {
            throw new QueryEvaluationException("No createFunCall found in filter");
        }
    }

    public TermAst termFromGroupCondition(SparqlParser.GroupConditionContext ctx) {
        if (ctx.expression() != null) {
            return termFromExpression(ctx.expression());
        }
        if (ctx.var_() != null) {
            return terms.termFromVar(ctx.var_());
        }
        if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        }
        if (ctx.functionCall() != null) {
            SparqlParser.FunctionCallContext fc = ctx.functionCall();
            IriAst functionName = new IriAst(fc.iriRef().getText());
            if (fc.argList() == null) {
                return createFunCall(functionName, List.of());
            }
            if (fc.argList().NIL() != null) {
                return createFunCall(functionName, List.of());
            }
            List<TermAst> args = fc.argList().expression().stream()
                    .map(this::termFromExpression).toList();
            return createFunCall(functionName, args);
        }
        throw new QueryEvaluationException("Unsupported group condition: " + ctx.getText());
    }

    public TermAst termFromIriRefOrFunction(SparqlParser.IriRefOrFunctionContext ctx) {
        if (ctx.iriRef() != null && ctx.argList() == null) {
            return terms.termFromIriRef(ctx.iriRef());
        } else if (ctx.iriRef() != null && ctx.argList() != null) {
            List<TermAst> args = termListFromArgList(ctx.argList());
            IriAst iriRef = (IriAst) terms.termFromIriRef(ctx.iriRef());
            return createFunCall(iriRef, args);
        } else {
            throw new QueryEvaluationException("Unexpected element in IRI ref or function");
        }
    }

    public List<TermAst> termListFromArgList(SparqlParser.ArgListContext ctx) {
        return ctx.expression().stream().map(this::termFromExpression).toList();
    }

    public TermAst termFromAggregate(SparqlParser.AggregateContext ctx) {
        boolean distinct = ctx.DISTINCT() != null;

        if (ctx.COUNT() != null) {
            TermAst expression = null;
            if (ctx.STAR() == null && ctx.expression() != null && !ctx.expression().isEmpty()) {
                expression = termFromExpression(ctx.expression());
            }
            return createAggregate(AggregateFunction.COUNT, distinct, expression, null);
        }
        if (ctx.SUM() != null) {
            return createAggregate(AggregateFunction.SUM, distinct,
                    termFromExpression(ctx.expression()), null);
        }
        if (ctx.AVG() != null) {
            return createAggregate(AggregateFunction.AVG, distinct,
                    termFromExpression(ctx.expression()), null);
        }
        if (ctx.MIN() != null) {
            return createAggregate(AggregateFunction.MIN, distinct,
                    termFromExpression(ctx.expression()), null);
        }
        if (ctx.MAX() != null) {
            return createAggregate(AggregateFunction.MAX, distinct,
                    termFromExpression(ctx.expression()), null);
        }
        if (ctx.SAMPLE() != null) {
            return createAggregate(AggregateFunction.SAMPLE, distinct,
                    termFromExpression(ctx.expression()), null);
        }
        if (ctx.GROUP_CONCAT() != null) {
            String sep = ctx.string_() != null ? ctx.string_().getText() : null;
            return createAggregate(AggregateFunction.GROUP_CONCAT, distinct,
                    termFromExpression(ctx.expression()), sep);
        }
        throw new QueryEvaluationException("Unsupported aggregate: " + ctx.getText());
    }

    public TermAst termFromPrimary(SparqlParser.PrimaryExpressionContext ctx) {
        if (ctx.brackettedExpression() != null) {
            return termFromBrackettedExpression(ctx.brackettedExpression());
        }
        if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        }
        if (ctx.iriRefOrFunction() != null) {
            return termFromIriRefOrFunction(ctx.iriRefOrFunction());
        }
        if (ctx.rdfLiteral() != null) {
            return terms.termFromRdfLiteral(ctx.rdfLiteral());
        }
        if (ctx.numericLiteral() != null) {
            return terms.termFromNumericLiteral(ctx.numericLiteral());
        }
        if (ctx.booleanLiteral() != null) {
            return terms.termFromBooleanLiteral(ctx.booleanLiteral());
        }
        if (ctx.var_() != null) {
            return terms.termFromVar(ctx.var_());
        }
        throw new QueryEvaluationException("Unexpected content of bracketed termFromExpression");
    }
}
