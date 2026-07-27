package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Objects;
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
        switch (constraint) {
            case ASTConstants.OPERATOR.NOT -> { return new BooleanNotAst(args); }
            case ASTConstants.OPERATOR.PLUS -> { return new UnaryPlusAst(args); }
            case ASTConstants.OPERATOR.MINUS -> { return new UnaryMinusAst(args); }
            case ASTConstants.FUNCTION_CALL.BOUND -> { return new BoundAst(args); }
            case ASTConstants.FUNCTION_CALL.IS_IRI -> { return new IsIriAst(args); }
            case ASTConstants.FUNCTION_CALL.IS_BLANK -> { return new IsBlankAst(args); }
            case ASTConstants.FUNCTION_CALL.IS_LITERAL -> { return new IsLiteralAst(args); }
            case ASTConstants.FUNCTION_CALL.STR -> { return new StrAst(args); }
            case ASTConstants.FUNCTION_CALL.UCASE -> { return new UcaseAst(args); }
            case ASTConstants.FUNCTION_CALL.LCASE -> { return new LcaseAst(args); }
            case ASTConstants.FUNCTION_CALL.LANG -> { return new LangAst(args); }
            case ASTConstants.FUNCTION_CALL.STRDT -> { return new StrDtAst(args); }
            case ASTConstants.FUNCTION_CALL.STRLANG -> { return new StrLangAst(args); }
            case ASTConstants.FUNCTION_CALL.DATATYPE -> { return new DatatypeAst(args); }
            case ASTConstants.FUNCTION_CALL.IRI -> { return new IriFunctionAst(args); }
            case ASTConstants.FUNCTION_CALL.BNODE -> { return new BnodeAst(args); }
            case ASTConstants.OPERATOR.OR -> { return new OrAst(args); }
            case ASTConstants.OPERATOR.AND -> { return new AndAst(args); }
            case ASTConstants.OPERATOR.EQ -> { return new EqualsAst(args); }
            case ASTConstants.OPERATOR.NE -> { return new DifferentAst(args); }
            case ASTConstants.OPERATOR.LT -> { return new LowerThanAst(args); }
            case ASTConstants.OPERATOR.LE -> { return new LowerOrEqualThanAst(args); }
            case ASTConstants.OPERATOR.GT -> { return new GreaterThanAst(args); }
            case ASTConstants.OPERATOR.GE -> { return new GreaterOrEqualThanAst(args); }
            case ASTConstants.OPERATOR.MUL -> { return new MultiplyAst(args); }
            case ASTConstants.OPERATOR.DIV -> { return new DivideAst(args); }
            case ASTConstants.OPERATOR.ADD -> { return new AddAst(args); }
            case ASTConstants.OPERATOR.SUB -> { return new SubtractAst(args); }
            case ASTConstants.FUNCTION_CALL.SAMETERM -> { return new SameTermAst(args); }
            case ASTConstants.FUNCTION_CALL.LANGMATCHES -> { return new LangMatchesAst(args); }
            case ASTConstants.FUNCTION_CALL.CONTAINS -> { return new ContainsAst(args); }
            case ASTConstants.FUNCTION_CALL.STRSTARTS -> { return new StrStartsAst(args); }
            case ASTConstants.FUNCTION_CALL.STRENDS -> { return new StrEndsAst(args); }
            case ASTConstants.FUNCTION_CALL.SUBSTR -> { return new SubstrAst(args); }
            case ASTConstants.FUNCTION_CALL.CONCAT -> { return new ConcatAst(args); }
            case ASTConstants.FUNCTION_CALL.STRBEFORE -> { return new StrBeforeAst(args); }
            case ASTConstants.FUNCTION_CALL.STRAFTER -> { return new StrAfterAst(args); }
            case ASTConstants.FUNCTION_CALL.REPLACE -> { return new ReplaceAst(args); }
            case ASTConstants.FUNCTION_CALL.REGEX -> {
                if (args.size() == 2) {
                    return new BinaryRegexAst(args);
                } else if (args.size() == 3) {
                    return new TrinaryRegexAst(args);
                } else {
                    throw new QueryEvaluationException("Unexpected number of arguments (3) for REGEX keyword");
                }
            }
            default ->
                    throw new QueryEvaluationException(
                            "Unexpected number of arguments (" + args.size() + ") for " + constraint);
        }
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
        if (ctx.multiplicativeExpression() != null && !ctx.multiplicativeExpression().isEmpty()) {
            if (ctx.multiplicativeExpression().size() > 1
                    || !ctx.numericLiteralNegative().isEmpty()
                    || !ctx.numericLiteralPositive().isEmpty()) {
                TermAst leftHand = termFromMultiplicative(ctx.multiplicativeExpression().getFirst());
                ASTConstants.OPERATOR op = ASTConstants.OPERATOR.ADD;
                for (int i = 1; i < ctx.getChildCount(); i++) {
                    ParseTree child = ctx.getChild(i);
                    if (child instanceof TerminalNode) {
                        if (Objects.equals(child.getText(), "+")) {
                            op = ASTConstants.OPERATOR.ADD;
                        } else if (Objects.equals(child.getText(), "-")) {
                            op = ASTConstants.OPERATOR.SUB;
                        } else {
                            throw new QueryEvaluationException(
                                    "Unexpected operator in additive termFromExpression " + child.getText());
                        }
                    } else {
                        TermAst rightHand = switch (child) {
                            case SparqlParser.MultiplicativeExpressionContext c ->
                                    termFromMultiplicative(c);
                            case SparqlParser.NumericLiteralPositiveContext c ->
                                    (ExprAst) terms.termFromNumericLiteralPositive(c);
                            case SparqlParser.NumericLiteralNegativeContext c ->
                                    (ExprAst) terms.termFromNumericLiteralNegative(c);
                            case null, default ->
                                    throw new QueryEvaluationException(
                                            "Unexpected left hand termFromExpression in additive termFromExpression "
                                                    + ctx.getText()
                                                    + " "
                                                    + (child != null ? child.getText() : "null")
                                    );
                        };
                        leftHand = createConstraint(op, List.of(leftHand, rightHand));
                    }
                }
                return leftHand;
            } else {
                return termFromMultiplicative(ctx.multiplicativeExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No multiplicative termFromExpression found");
        }
    }

    public TermAst termFromMultiplicative(SparqlParser.MultiplicativeExpressionContext ctx) {
        if (ctx.unaryExpression() != null && !ctx.unaryExpression().isEmpty()) {
            if (ctx.unaryExpression().size() > 1) {
                TermAst head = termFromUnary(ctx.unaryExpression().getFirst());
                TermAst rightHand = null;
                ASTConstants.OPERATOR op = null;
                for (int i = 1; i < ctx.getChildCount(); i += 2) {
                    ParseTree operatorContext = ctx.getChild(i);
                    ParseTree rightHandContext = ctx.getChild(i + 1);

                    if (rightHandContext instanceof SparqlParser.UnaryExpressionContext uec) {
                        rightHand = termFromUnary(uec);
                    }

                    if (operatorContext instanceof TerminalNode terminalNode) {
                        if (Objects.equals(terminalNode.getText(), "*")) {
                            op = ASTConstants.OPERATOR.MUL;
                        } else if (Objects.equals(terminalNode.getText(), "/")) {
                            op = ASTConstants.OPERATOR.DIV;
                        }
                    }
                    if (op != null && rightHand != null) {
                        head = createConstraint(op, List.of(head, rightHand));
                    } else {
                        throw new QuerySyntaxException(
                                "Unexpected operator or right hand content in " + ctx.getText());
                    }
                }
                return head;
            } else {
                return termFromUnary(ctx.unaryExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No unary termFromExpression found");
        }
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
        } else if (ctx.strReplaceExpression() != null) {
            return termFromReplace(ctx.strReplaceExpression());
        } else if (ctx.BOUND() != null) {
            return createConstraint(ASTConstants.FUNCTION_CALL.BOUND,
                    List.of(terms.var(ctx.var_().getText())));
        } else if (ctx.BNODE() != null) {
            List<TermAst> args = ctx.expression() == null
                    ? List.of()
                    : ctx.expression().stream().map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.BNODE, args);
        } else if (ctx.IF() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return new IfAst(args.get(0), args.get(1), args.get(2));
        } else if (ctx.RAND() != null) {
            return new RandAst();
        } else if (ctx.UUID() != null) {
            return new UuidAst();
        } else if (ctx.STRUUID() != null) {
            return new StrUuidAst();
        } else if (ctx.CONCAT() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.CONCAT, args);
        } else if (ctx.COALESCE() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            return new CoalesceAst(args);
        } else if (ctx.subStringExpression() != null) {
            List<TermAst> args = ctx.subStringExpression().expression().stream()
                    .map(this::termFromExpression).toList();
            return createConstraint(ASTConstants.FUNCTION_CALL.SUBSTR, args);
        } else if (ctx.NOW() != null) {
            return new NowAst();
        } else if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(this::termFromExpression).toList();
            if (ctx.STR() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STR, args);
            } else if (ctx.UCASE() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.UCASE, args);
            } else if (ctx.LCASE() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.LCASE, args);
            } else if (ctx.ENCODE_FOR_URI() != null) {
                return new EncodeForUriAst(args);
            } else if (ctx.LANG() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.LANG, args);
            } else if (ctx.LANGMATCHES() != null) {
                return new LangMatchesAst(args);
            } else if (ctx.CONTAINS() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.CONTAINS, args);
            } else if (ctx.STRSTARTS() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STRSTARTS, args);
            } else if (ctx.STRENDS() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STRENDS, args);
            } else if (ctx.STRDT() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STRDT, args);
            } else if (ctx.STRLANG() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STRLANG, args);
            } else if (ctx.STRBEFORE() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STRBEFORE, args);
            } else if (ctx.STRAFTER() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.STRAFTER, args);
            } else if (ctx.YEAR() != null) {
                return new YearAst(args);
            } else if (ctx.MONTH() != null) {
                return new MonthAst(args);
            } else if (ctx.DAY() != null) {
                return new DayAst(args);
            } else if (ctx.HOURS() != null) {
                return new HoursAst(args);
            } else if (ctx.MINUTES() != null) {
                return new MinutesAst(args);
            } else if (ctx.SECONDS() != null) {
                return new SecondsAst(args);
            } else if (ctx.TIMEZONE() != null) {
                return new TimezoneAst(args);
            } else if (ctx.TZ() != null) {
                return new TzAst(args);
            } else if (ctx.DATATYPE() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.DATATYPE, args);
            } else if (ctx.IRI() != null || ctx.URI() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.IRI, args);
            } else if (ctx.SAME_TERM() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.SAMETERM, args);
            } else if (ctx.IS_URI() != null || ctx.IS_IRI() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.IS_IRI, args);
            } else if (ctx.IS_BLANK() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.IS_BLANK, args);
            } else if (ctx.IS_LITERAL() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.IS_LITERAL, args);
            } else if (ctx.MD5() != null) {
                return new Md5Ast(args);
            } else if (ctx.SHA1() != null) {
                return new Sha1Ast(args);
            } else if (ctx.SHA256() != null) {
                return new Sha256Ast(args);
            } else if (ctx.SHA384() != null) {
                return new Sha384Ast(args);
            } else if (ctx.SHA512() != null) {
                return new Sha512Ast(args);
            } else if (ctx.ABS() != null) {
                return new AbsAst(args);
            } else if (ctx.CEIL() != null) {
                return new CeilAst(args);
            } else if (ctx.FLOOR() != null) {
                return new FloorAst(args);
            } else if (ctx.ROUND() != null) {
                return new RoundAst(args);
            } else if (ctx.BOUND() != null) {
                return createConstraint(ASTConstants.FUNCTION_CALL.BOUND,
                        List.of(terms.var(ctx.var_().getText())));
            } else if (ctx.regexExpression() != null) {
                return termFromRegex(ctx.regexExpression());
            } else if (ctx.STRLEN() != null) {
                return new StrLenAst(args.getFirst());
            } else {
                throw new QueryEvaluationException(
                        "Unexpected function for a  BuiltInCall for token " + ctx.getText());
            }
        } else {
            throw new QueryEvaluationException(
                    "Unable to resolve BuiltInCall for token " + ctx.getText());
        }
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
        } else if (ctx.builtInCall() != null) {
            return termFromBuiltInCall(ctx.builtInCall());
        } else if (ctx.iriRefOrFunction() != null) {
            return termFromIriRefOrFunction(ctx.iriRefOrFunction());
        } else if (ctx.rdfLiteral() != null) {
            return terms.termFromRdfLiteral(ctx.rdfLiteral());
        } else if (ctx.numericLiteral() != null) {
            return terms.termFromNumericLiteral(ctx.numericLiteral());
        } else if (ctx.booleanLiteral() != null) {
            return terms.termFromBooleanLiteral(ctx.booleanLiteral());
        } else if (ctx.var_() != null) {
            return terms.termFromVar(ctx.var_());
        } else {
            throw new QueryEvaluationException("Unexpected content of bracketed termFromExpression");
        }
    }
}
