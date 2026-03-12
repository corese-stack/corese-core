package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParserBaseListener;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for SPARQL parsing features implemented as ANTLR listeners.
 *
 * @see SparqlParserBaseListener
 */
public abstract class AbstractSparqlFeature extends SparqlParserBaseListener {

    private final SparqlAstBuilder builder;

    protected AbstractSparqlFeature(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    protected SparqlAstBuilder builder() {
        return this.builder;
    }

    /**
     * Creates the right createFunCall AST according to keyword and argument list
     *
     * @param constraint keyword
     * @param args       arguments list
     * @return An ConstraintAst
     */
    protected ConstraintAst createConstraint(ASTConstants.Constraint constraint, List<TermAst> args) {
        switch (args.size()) {
            case 1 -> {
                switch (constraint) {
                    case ASTConstants.OPERATOR.BOOLEAN_NOT -> {
                        return new BooleanNotAst(args.getFirst());
                    }
                    case ASTConstants.OPERATOR.PLUS -> {
                        return new UnaryPlusAst(args.getFirst());
                    }
                    case ASTConstants.OPERATOR.MINUS -> {
                        return new UnaryMinusAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.BOUND -> {
                        return new BoundAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.ISIRI -> {
                        return new IsIriAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.ISBLANK -> {
                        return new IsBlankAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.ISLITERAL -> {
                        return new IsLiteralAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.STR -> {
                        return new StrAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.LANG -> {
                        return new LangAst(args.getFirst());
                    }
                    case ASTConstants.FUNCTION_CALL.DATATYPE -> {
                        return new DatatypeAst(args.getFirst());
                    }
                    default ->
                            throw new QueryEvaluationException("Unexpected number of arguments (1) for createFunCall " + constraint);
                }
            }
            case 2 -> {
                switch (constraint) {
                    case ASTConstants.OPERATOR.OR -> {
                        return new OrAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.AND -> {
                        return new AndAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.EQUALS -> {
                        return new EqualsAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.DIFFERENT -> {
                        return new DifferentAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.LOWER -> {
                        return new LowerThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.LOWER_EQUAL -> {
                        return new LowerOrEqualThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.GREATER -> {
                        return new GreaterThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.GREATER_EQUAL -> {
                        return new GreaterOrEqualThanAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.TIMES -> {
                        return new MultiplyAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.DIVIDE -> {
                        return new DivideAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.PLUS -> {
                        return new AddAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.OPERATOR.MINUS -> {
                        return new SubtractAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.FUNCTION_CALL.SAMETERM -> {
                        return new SameTermAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.FUNCTION_CALL.LANGMATCHES -> {
                        return new LangMatchesAst(args.getFirst(), args.getLast());
                    }
                    case ASTConstants.FUNCTION_CALL.REGEX -> {
                        return new BinaryRegexAst(args.getFirst(), args.getLast());
                    }
                    default ->
                            throw new QueryEvaluationException("Unexpected number of arguments (2) for " + constraint + " keyword");
                }
            }
            case 3 -> {
                if (Objects.requireNonNull(constraint) == ASTConstants.FUNCTION_CALL.REGEX) {
                    return new TrinaryRegexAst(args.getFirst(), args.get(1), args.getLast());
                }
                throw new QueryEvaluationException("Unexpected number of arguments (3) for " + constraint + " keyword");
            }
            default ->
                    throw new QueryEvaluationException("Unexpected number of arguments (" + args.size() + ") for " + constraint);
        }
    }

    protected ConstraintAst createFunCall(IriAst functionName, List<TermAst> args) {
        return new FunctionCallAst(functionName, args);
    }

    // ---- term helpers ----

    protected TermAst termFromVerb(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VerbContext ctx) {
        if (ctx.A() != null) return builder().iri("a");
        return termFromVarOrIriRef(ctx.varOrIRIref());
    }

    protected TermAst termFromVarOrTerm(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VarOrTermContext ctx) {
        if (ctx.var_() != null) return termFromVar(ctx.var_());
        return termFromGraphTerm(ctx.graphTerm());
    }

    protected TermAst termFromVarOrIriRef(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VarOrIRIrefContext ctx) {
        if(ctx.var_() != null) {
            return termFromVar(ctx.var_());
        }
        return termFromIriRef(ctx.iriRef());
    }

    protected TermAst termFromGraphTerm(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GraphTermContext ctx) {
        if (ctx.iriRef() != null) {
            return termFromIriRef(ctx.iriRef());
        }
        if (ctx.rdfLiteral() != null) {
            return termFromRdfLiteral(ctx.rdfLiteral());
        }
        if (ctx.numericLiteral() != null) {
            return termFromNumericLiteral(ctx.numericLiteral());
        }
        if (ctx.booleanLiteral() != null) {
            return termFromBooleanLiteral(ctx.booleanLiteral());
        }
        if (ctx.blankNode() != null) {
            return termFromBlankNode(ctx.blankNode());
        }
        if (ctx.NIL() != null) {
            return builder().iri("()");
        } // NIL = () in SPARQL
        return builder().iri(ctx.getText());
    }

    protected List<TermAst> termListFromObjectList(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ObjectListContext ctx) {
        List<TermAst> out = new ArrayList<>();
        for (var obj : ctx.object_()) {
            out.add(termFromObject(obj));
        }
        return out;
    }

    protected TermAst termFromObject(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.Object_Context ctx) {
        // object_ : graphNode
        return termFromGraphNode(ctx.graphNode());
    }

    protected TermAst termFromGraphNode(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GraphNodeContext ctx) {
        if (ctx.varOrTerm() != null) return termFromVarOrTerm(ctx.varOrTerm());
        if (ctx.triplesNode() != null) {
            // MVP: pas encore supporté ( [ ... ] ou ( ... ) )
            return builder().iri(ctx.triplesNode().getText());
        }

        return builder().iri(ctx.getText());
    }

    protected TermAst termFromRdfLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.RdfLiteralContext ctx) {
        // rdfLiteral : string_ ( LANGTAG | '^^' iriRef )?

        String lexical = ctx.string_().getText();
        String lang = null;
        String datatype = null;

        if (ctx.LANGTAG() != null) {
            String t = ctx.LANGTAG().getText(); // ex: "@fr"
            lang = t.startsWith("@") ? t.substring(1) : t;
        } else if (ctx.DOUBLE_CARET() != null && ctx.iriRef() != null) {
            datatype = ctx.iriRef().getText(); // ex: xsd:integer ou <iri>
        }
        return builder().literal(lexical, lang, datatype);
    }

    protected ExprAst expressionFromConstraint(SparqlParser.ConstraintContext ctx) {
        if (ctx.builtInCall() != null) {
            return expressionFromBuiltInCall(ctx.builtInCall());
        } else if (ctx.functionCall() != null) {
            IriAst functionTermAst = new IriAst(ctx.functionCall().iriRef().getText());
            List<TermAst> args = ctx.functionCall().argList().expression().stream().map(arg -> (TermAst) expression(arg)).toList();
            return new FunctionCallAst(functionTermAst, args);
        } else if (ctx.brackettedExpression() != null && ctx.brackettedExpression().expression() != null) {
            return expressionFromBrackettedExpression(ctx.brackettedExpression());
        } else {
            throw new QueryEvaluationException("No createFunCall found in filter");
        }
    }

    protected ExprAst expressionFromBuiltInCall(SparqlParser.BuiltInCallContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(arg -> (TermAst) expression(arg)).toList();
            if (ctx.STR() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.STR, args);
            } else if (ctx.LANG() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.LANG, args);
            } else if (ctx.LANGMATCHES() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.LANGMATCHES, args);
            } else if (ctx.DATATYPE() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.DATATYPE, args);
            } else if (ctx.SAME_TERM() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.SAMETERM, args);
            } else if (ctx.IS_URI() != null || ctx.IS_IRI() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.ISIRI, args);
            } else if (ctx.IS_BLANK() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.ISBLANK, args);
            } else if (ctx.IS_LITERAL() != null) {
                return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.ISLITERAL, args);
            } else {
                throw new QueryEvaluationException("Unexpected function for a  BuiltInCall for token " + ctx.getText());
            }
        } else if (ctx.BOUND() != null) {
            return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.BOUND, List.of(builder().var(ctx.var_().getText())));
        } else if (ctx.regexExpression() != null) {
            return (ExprAst) ExpressionFromRegex(ctx.regexExpression());
        } else {
            throw new QueryEvaluationException("Unable to resolve BuiltInCall for token " + ctx.getText());
        }
    }

    protected ExprAst expression(SparqlParser.ExpressionContext ctx) {
        if (ctx.conditionalOrExpression() != null) {
            return this.expressionFromConditionalOr(ctx.conditionalOrExpression());
        } else {
            throw new QueryEvaluationException("No conditional OR found");
        }
    }

    protected ExprAst expressionFromConditionalOr(SparqlParser.ConditionalOrExpressionContext ctx) {
        if (ctx.conditionalAndExpression() != null && !ctx.conditionalAndExpression().isEmpty()) {
            if (ctx.conditionalAndExpression().size() > 1) {
                List<TermAst> args = ctx.conditionalAndExpression().stream().map(arg -> (TermAst) expressionFromConditionalAnd(arg)).toList();
                return (ExprAst) createConstraint(ASTConstants.OPERATOR.OR, args);
            } else {
                return expressionFromConditionalAnd(ctx.conditionalAndExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No conditional AND  found");
        }
    }

    protected ExprAst expressionFromConditionalAnd(SparqlParser.ConditionalAndExpressionContext ctx) {
        if (ctx.valueLogical() != null && !ctx.valueLogical().isEmpty()) {
            if (ctx.valueLogical().size() > 1) {
                List<TermAst> args = ctx.valueLogical().stream().map(arg -> (TermAst) expressionFromValueLogical(arg)).toList();
                return (ExprAst) createConstraint(ASTConstants.OPERATOR.AND, args);
            } else {
                return expressionFromValueLogical(ctx.valueLogical().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No logical value found");
        }
    }

    protected ExprAst expressionFromValueLogical(SparqlParser.ValueLogicalContext ctx) {
        if (ctx.relationalExpression() != null) {
            return this.expressionFromRelational(ctx.relationalExpression());
        } else {
            throw new QueryEvaluationException("No relational expression found");
        }
    }

    protected ExprAst expressionFromRelational(SparqlParser.RelationalExpressionContext ctx) {
        if (ctx.numericExpression() != null && !ctx.numericExpression().isEmpty()) {
            if (ctx.numericExpression().size() > 1) {
                ASTConstants.OPERATOR op;
                if (ctx.EQUAL() != null) {
                    op = ASTConstants.OPERATOR.EQUALS;
                } else if (ctx.NOT_EQUAL() != null) {
                    op = ASTConstants.OPERATOR.DIFFERENT;
                } else if (ctx.LESS() != null) {
                    op = ASTConstants.OPERATOR.LOWER;
                } else if (ctx.LESS_OR_EQUAL() != null) {
                    op = ASTConstants.OPERATOR.LOWER_EQUAL;
                } else if (ctx.GREATER() != null) {
                    op = ASTConstants.OPERATOR.GREATER;
                } else if (ctx.GREATER_OR_EQUAL() != null) {
                    op = ASTConstants.OPERATOR.GREATER_EQUAL;
                } else {
                    throw new QueryEvaluationException("Unexpected operator in relational expression");
                }
                List<TermAst> args = ctx.numericExpression().stream().map(arg -> (TermAst) expressionFromNumeric(arg)).toList();
                return (ExprAst) createConstraint(op, args);
            } else {
                return expressionFromNumeric(ctx.numericExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No numeric expression found");
        }
    }

    protected ExprAst expressionFromNumeric(SparqlParser.NumericExpressionContext ctx) {
        if (ctx.additiveExpression() != null) {
            return this.expressionFromAdditive(ctx.additiveExpression());
        } else {
            throw new QueryEvaluationException("No additive expression found");
        }
    }

    protected ExprAst expressionFromAdditive(SparqlParser.AdditiveExpressionContext ctx) {
        if(ctx.multiplicativeExpression() != null && ! ctx.multiplicativeExpression().isEmpty()) {
            if (ctx.multiplicativeExpression().size() > 1
                    || ! ctx.numericLiteralNegative().isEmpty()
                    || ! ctx.numericLiteralPositive().isEmpty()) {
                ExprAst leftHand = expressionFromMultiplicative(ctx.multiplicativeExpression().getFirst());
                for(int i = 1; i < ctx.getChildCount() ; i++) {
                    ParseTree numericChild = ctx.getChild(i);
                    ExprAst rightHand = switch (numericChild) {
                        case SparqlParser.MultiplicativeExpressionContext multiplicativeExpressionContext ->
                                expressionFromMultiplicative(multiplicativeExpressionContext);
                        case SparqlParser.NumericLiteralPositiveContext numericLiteralPositiveContext ->
                                (ExprAst) termFromNumericLiteralPositive(numericLiteralPositiveContext);
                        case SparqlParser.NumericLiteralNegativeContext numericLiteralNegativeContext ->
                                (ExprAst) termFromNumericLiteralNegative(numericLiteralNegativeContext);
                        case null, default ->
                                throw new QueryEvaluationException("Unexpected left hand expression in additive expression");
                    };
                    ASTConstants.OPERATOR op;
                    if (ctx.getToken(SparqlParser.PLUS, i) != null) {
                        op = ASTConstants.OPERATOR.PLUS;
                    } else if (ctx.getToken(SparqlParser.MINUS, i) != null) {
                        op = ASTConstants.OPERATOR.MINUS;
                    } else {
                        throw new QueryEvaluationException("Unexpected operator in additive expression");
                    }
                    leftHand = (ExprAst) createConstraint(op, List.of(leftHand, rightHand));
                }
                return leftHand;
            } else {
                return expressionFromMultiplicative(ctx.multiplicativeExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No multiplicative expression found");
        }
    }

    protected ExprAst expressionFromMultiplicative(SparqlParser.MultiplicativeExpressionContext ctx) {
        if(ctx.unaryExpression() != null && ! ctx.unaryExpression().isEmpty()) {
            if (ctx.unaryExpression().size() > 1) {
                ExprAst head = expressionFromUnary(ctx.unaryExpression().getFirst());
                for(int i = 1; i < ctx.getChildCount() ; i++) {
                    ParseTree numericChild = ctx.getChild(i);
                    ExprAst leftHand = expressionFromUnary((SparqlParser.UnaryExpressionContext) numericChild);
                    ASTConstants.OPERATOR op;
                    if (ctx.getToken(SparqlParser.STAR, i) != null) {
                        op = ASTConstants.OPERATOR.TIMES;
                    } else if (ctx.getToken(SparqlParser.SLASH, i) != null) {
                        op = ASTConstants.OPERATOR.DIVIDE;
                    } else {
                        throw new QueryEvaluationException("Unexpected operator in multiplicative expression");
                    }
                    head = (ExprAst) createConstraint(op, List.of(head, leftHand));
                }
                return head;
            } else {
                return expressionFromUnary(ctx.unaryExpression().getFirst());
            }
        } else {
            throw new QueryEvaluationException("No unary expression found");
        }
    }

    protected ExprAst expressionFromUnary(SparqlParser.UnaryExpressionContext ctx) {
        ASTConstants.Constraint op = null;
        if(ctx.PLUS() != null) {
            op = ASTConstants.OPERATOR.PLUS;
        } else if (ctx.MINUS() != null) {
            op = ASTConstants.OPERATOR.MINUS;
        } else if(ctx.EXCLAMATION() != null) {
            op = ASTConstants.OPERATOR.BOOLEAN_NOT;
        }
        if(op != null) {
            return (ExprAst) createConstraint(op, List.of(termFromPrimary(ctx.primaryExpression())));
        } else {
            return (ExprAst) termFromPrimary(ctx.primaryExpression());
        }
    }

    protected TermAst termFromPrimary(SparqlParser.PrimaryExpressionContext ctx) {
        if(ctx.brackettedExpression() != null) {
            return expressionFromBrackettedExpression(ctx.brackettedExpression());
        } else if (ctx.builtInCall() != null) {
            return expressionFromBuiltInCall(ctx.builtInCall());
        } else if(ctx.iriRefOrFunction() != null) {
            return termFromIriRefOrFunction(ctx.iriRefOrFunction());
        } else if(ctx.rdfLiteral() != null) {
            return termFromRdfLiteral(ctx.rdfLiteral());
        } else if(ctx.numericLiteral() != null) {
            return termFromNumericLiteral(ctx.numericLiteral());
        } else if(ctx.booleanLiteral() != null) {
            return termFromBooleanLiteral(ctx.booleanLiteral());
        } else if(ctx.var_() != null) {
            return termFromVar(ctx.var_());
        } else {
            throw new QueryEvaluationException("Unexpected content of bracketed expression");
        }
    }

    protected TermAst termFromVar(SparqlParser.Var_Context ctx) {
        return builder().var(ctx.getText());
    }

    protected TermAst termFromBooleanLiteral(SparqlParser.BooleanLiteralContext ctx) {
        if(ctx.FALSE() != null) {
            return new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
        } else if(ctx.TRUE() != null) {
            return new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected value for boolean literal");
        }
    }

    protected ExprAst expressionFromBrackettedExpression(SparqlParser.BrackettedExpressionContext ctx) {
        return expression(ctx.expression());
    }

    protected TermAst termFromIriRefOrFunction(SparqlParser.IriRefOrFunctionContext ctx) {
        if(ctx.iriRef() != null && ctx.argList() == null) {
            return termFromIriRef(ctx.iriRef());
        } else if(ctx.iriRef() != null && ctx.argList() != null) {
            List<TermAst> args = termListFromArgList(ctx.argList());
            IriAst iriRef = (IriAst) termFromIriRef(ctx.iriRef());
            return new FunctionCallAst(iriRef, args);
        } else {
            throw new QueryEvaluationException("Unexpected element in IRI ref or function");
        }
    }

    protected List<TermAst> termListFromArgList(SparqlParser.ArgListContext ctx) {
        return ctx.expression().stream().map(arg -> (TermAst) expression(arg)).toList();
    }

    protected TermAst termFromIriRef(SparqlParser.IriRefContext ctx) {
        return builder.iri(ctx.getText());
    }

    protected TermAst termFromNumericLiteral(SparqlParser.NumericLiteralContext ctx) {
        if(ctx.numericLiteralUnsigned() != null) {
            return termFromNumericLiteralUnsigned(ctx.numericLiteralUnsigned());
        } else if(ctx.numericLiteralPositive() != null) {
            return termFromNumericLiteralPositive(ctx.numericLiteralPositive());
        } else if(ctx.numericLiteralNegative() != null) {
            return termFromNumericLiteralNegative(ctx.numericLiteralNegative());
        } else {
            throw new QueryEvaluationException("Unexpected content for numeric literal");
        }
    }

    private TermAst termFromNumericLiteralNegative(SparqlParser.NumericLiteralNegativeContext ctx) {
        if(ctx.INTEGER_NEGATIVE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdNegativeInteger.getIRI().stringValue());
        } else if(ctx.DECIMAL_NEGATIVE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_NEGATIVE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for negative numeric literal");
        }
    }

    private TermAst termFromNumericLiteralPositive(SparqlParser.NumericLiteralPositiveContext ctx) {
        if(ctx.INTEGER_POSITIVE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdPositiveInteger.getIRI().stringValue());
        } else if(ctx.DECIMAL_POSITIVE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_POSITIVE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    private TermAst termFromNumericLiteralUnsigned(SparqlParser.NumericLiteralUnsignedContext ctx) {
        if(ctx.INTEGER() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdUnsignedInt.getIRI().stringValue());
        } else if(ctx.DECIMAL() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE() != null) {
            return this.builder().literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    protected TermAst termFromBlankNode(SparqlParser.BlankNodeContext ctx) {
        return this.builder().iri(ctx.getText());
    }

    protected ExprAst ExpressionFromRegex(SparqlParser.RegexExpressionContext ctx) {
        if (ctx.expression() != null) {
            List<TermAst> args = ctx.expression().stream().map(arg -> (TermAst) expression(arg)).toList();
            return (ExprAst) this.createConstraint(ASTConstants.FUNCTION_CALL.REGEX, args);
        } else {
            throw new QueryEvaluationException("Unexpected arguments for REGEX call");
        }
    }

}
