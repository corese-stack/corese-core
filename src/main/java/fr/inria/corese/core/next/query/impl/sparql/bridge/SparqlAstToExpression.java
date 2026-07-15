package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.util.StringUtils;
import fr.inria.corese.core.sparql.datatype.RDF;
import fr.inria.corese.core.sparql.triple.parser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts Corese-next {@link TermAst} nodes (including {@link ConstraintAst}) into
 * {@link Expression} trees for the SPARQL interpreter, consumable from KGRAM “next” via
 * {@link fr.inria.corese.core.next.query.kgram.api.core.Filter} / {@link AstBackedExpr}.
 *
 */
public final class SparqlAstToExpression {

    private static final Map<Class<?>, String> BINARY_OPERATORS =
            Map.ofEntries(
                    Map.entry(AndAst.class, KeywordHolder.SEAND),
                    Map.entry(OrAst.class, KeywordHolder.SEOR),
                    Map.entry(EqualsAst.class, "="),
                    Map.entry(DifferentAst.class, "!="),
                    Map.entry(LowerThanAst.class, "<"),
                    Map.entry(LowerOrEqualThanAst.class, "<="),
                    Map.entry(GreaterThanAst.class, ">"),
                    Map.entry(GreaterOrEqualThanAst.class, ">="),
                    Map.entry(AddAst.class, "+"),
                    Map.entry(SubtractAst.class, "-"),
                    Map.entry(MultiplyAst.class, "*"),
                    Map.entry(DivideAst.class, "/"));

    private static final Map<Class<?>, String> UNARY_OPERATORS =
            Map.ofEntries(
                    Map.entry(UnaryPlusAst.class, "+"),
                    Map.entry(UnaryMinusAst.class, "-"),
                    Map.entry(BooleanNotAst.class, "!"));

    private static final Map<Class<?>, String> UNARY_FUNCTIONS =
            Map.ofEntries(
                    Map.entry(BoundAst.class, Processor.BOUND),
                    Map.entry(IsIriAst.class, "isIRI"),
                    Map.entry(IsBlankAst.class, "isBlank"),
                    Map.entry(IsLiteralAst.class, "isLiteral"),
                    Map.entry(StrAst.class, "str"),
                    Map.entry(LangAst.class, "lang"),
                    Map.entry(DatatypeAst.class, "datatype"),
                    Map.entry(IriFunctionAst.class, "iri"),
                    Map.entry(LcaseAst.class, "lcase"),
                    Map.entry(UcaseAst.class, "ucase"),
                    Map.entry(EncodeForUriAst.class, "encode_for_uri"),
                    Map.entry(Md5Ast.class, "md5"),
                    Map.entry(Sha1Ast.class, "sha1"),
                    Map.entry(Sha256Ast.class, "sha256"),
                    Map.entry(Sha384Ast.class, "sha384"),
                    Map.entry(Sha512Ast.class, "sha512"));

    private static final Map<Class<?>, String> BINARY_FUNCTIONS =
            Map.ofEntries(
                    Map.entry(SameTermAst.class, "sameTerm"),
                    Map.entry(LangMatchesAst.class, "langMatches"),
                    Map.entry(StrStartsAst.class, "strstarts"),
                    Map.entry(StrEndsAst.class, "strends"),
                    Map.entry(ContainsAst.class, "contains"),
                    Map.entry(StrBeforeAst.class, "strbefore"),
                    Map.entry(StrAfterAst.class, "strafter"),
                    Map.entry(StrLangAst.class, "strlang"),
                    Map.entry(StrDtAst.class, Processor.STRDT));

    private SparqlAstToExpression() {
    }

    /**
     * Converts any {@link TermAst} (variable, literal, IRI, or constraint expression) to {@link Expression}.
     */
    public static Expression convert(TermAst term) {
        return switch (term) {
            case VarAst(String name) -> Variable.create(name);
            case LiteralAst(String lexical, String lang, String datatype) -> literalToConstant(lexical, lang, datatype);
            case IriAst(String raw) -> iriToConstant(raw);
            case ConstraintAst c -> constraintToExpression(c);
            default -> throw new IllegalStateException("Unhandled TermAst: " + term.getClass());
        };
    }

    /**
     * Converts the operator of a SPARQL {@code FILTER} clause ({@link FilterAst}) the same way as
     * {@link #toNextFilter(TermAst)}.
     *
     * <p>Prefer {@link CoreseAstQueryBuilder#toNextFilter(FilterAst)} at call sites that build queries.
     */
    public static Filter toNextFilter(FilterAst filterClause) {
        Objects.requireNonNull(filterClause, "filterClause");
        return toNextFilter(filterClause.operator());
    }

    /**
     * Converts a filter {@link TermAst} to an {@link Expression}, then wraps it as a
     * {@link Filter} with {@link Filter#coreseNextSource()} set to {@code filterExpression}.
     *
     * <p>Prefer {@link CoreseAstQueryBuilder#toNextFilter(TermAst)} at call sites that build queries; this
     * method is the shared implementation. For a full {@link FilterAst} node, use {@link #toNextFilter(FilterAst)}.
     */
    public static Filter toNextFilter(TermAst filterExpression) {
        Expression exprTree = convert(filterExpression);
        initializeExpList(exprTree);
        AstBackedExpr expr = new AstBackedExpr(exprTree, Optional.of(filterExpression));
        return expr.getFilter();
    }

    /**
     * Initializes the {@code Expr} list ({@code lExp}) of every {@link Term} in the tree.
     */
    private static void initializeExpList(Expression expression) {
        if (expression instanceof Term term) {
            for (Expression arg : term.getArgs()) {
                initializeExpList(arg);
            }
            term.setExpList(new ArrayList<>(term.getArgs()));
        }
    }

    private static Constant literalToConstant(String lexical, String lang, String datatype) {
        if (lang != null && !lang.isEmpty()) {
            return Constant.create(unquoteLexical(lexical), RDF.rdflangString, lang);
        }
        if (datatype != null && !datatype.isEmpty()) {
            return Constant.create(unquoteLexical(lexical), normalizeDatatypeIri(datatype), null);
        }
        return Constant.createString(unquoteLexical(lexical));
    }

    private static String unquoteLexical(String lexical) {
        if (lexical.length() >= 2 && lexical.startsWith("\"")) {
            if (lexical.endsWith("\"")) {
                return lexical.substring(1, lexical.length() - 1);
            }
            int langIdx = lexical.lastIndexOf('"');
            if (langIdx > 0) {
                return lexical.substring(1, langIdx);
            }
        }
        return lexical;
    }

    private static String normalizeDatatypeIri(String dt) {
        String d =  StringUtils.trimChevronIRIs(dt);
        return fr.inria.corese.core.sparql.triple.parser.NSManager.nsm().toNamespace(d);
    }

    private static Constant iriToConstant(String rawIri) {
        String raw = StringUtils.trimChevronIRIs(rawIri);
        if (raw.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return Constant.createBlank(raw.substring(IOConstants.BLANK_NODE_PREFIX.length()));
        }
        return Constant.createResource(raw);
    }

    private static Expression constraintToExpression(ConstraintAst constraint) {
        String binaryOperator = BINARY_OPERATORS.get(constraint.getClass());
        if (binaryOperator != null && constraint instanceof BinaryConstraintAst binary) {
            return binaryOperatorTerm(binaryOperator, binary);
        }

        String unaryOperator = UNARY_OPERATORS.get(constraint.getClass());
        if (unaryOperator != null && constraint instanceof UnaryConstraintAst unary) {
            return Term.create(unaryOperator, convert(unary.argument()));
        }

        String unaryFunction = UNARY_FUNCTIONS.get(constraint.getClass());
        if (unaryFunction != null && constraint instanceof UnaryConstraintAst unary) {
            return functionTerm(unaryFunction, convert(unary.argument()));
        }

        String binaryFunction = BINARY_FUNCTIONS.get(constraint.getClass());
        if (binaryFunction != null && constraint instanceof BinaryConstraintAst binary) {
            return binaryFunctionTerm(binaryFunction, binary);
        }

        return specialConstraintToExpression(constraint);
    }

    private static Expression specialConstraintToExpression(ConstraintAst constraint) {
        return switch (constraint) {
            case BinaryRegexAst regex ->
                    regexTerm(convert(regex.getString()), convert(regex.getPattern()));
            case TrinaryRegexAst regex ->
                    regexTerm(convert(regex.getString()), convert(regex.getPattern()), convert(regex.getFlags()));
            case FunctionCallAst call ->
                    functionCallAst(call);
            case ConcatAst concat ->
                    variadicTerm("concat", concat.arguments());
            case CoalesceAst coalesce ->
                    variadicTerm(Processor.COALESCE, coalesce.arguments());
            case IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) ->
                    Term.function(Processor.IF, convert(condition), convert(thenExpr), convert(elseExpr));
            case ReplaceAst replace ->
                    replaceToTerm(replace);
            case SubstrAst substr ->
                    substrToTerm(substr);
            case BnodeAst bnode ->
                    bnodeToTerm(bnode);
            case ExistsAst ignored ->
                    throw new UnsupportedQueryFeatureException(
                            "EXISTS filters are not supported yet by the next pipeline");
            case NotExistsAst ignored ->
                    throw new UnsupportedQueryFeatureException(
                            "NOT EXISTS filters are not supported yet by the next pipeline");
            default ->
                    throw new UnsupportedQueryFeatureException(
                            "Filter expression is not supported yet by the next pipeline: "
                                    + constraint.getClass().getSimpleName());
        };
    }

    private static Term binaryOperatorTerm(String operator, BinaryConstraintAst binary) {
        return Term.create(operator, convert(binary.getLeftArgument()), convert(binary.getRightArgument()));
    }

    private static Term binaryFunctionTerm(String name, BinaryConstraintAst binary) {
        return functionTerm(name, convert(binary.getLeftArgument()), convert(binary.getRightArgument()));
    }

    private static Term functionTerm(String name, Expression arg) {
        Term t = Term.function(name);
        t.add(arg);
        return t;
    }

    private static Term functionTerm(String name, Expression a1, Expression a2) {
        Term t = Term.function(name);
        t.add(a1);
        t.add(a2);
        return t;
    }

    private static Term regexTerm(Expression s, Expression pattern) {
        Term t = Term.function("regex");
        t.add(s);
        t.add(pattern);
        return t;
    }

    private static Term regexTerm(Expression s, Expression pattern, Expression flags) {
        Term t = Term.function("regex");
        t.add(s);
        t.add(pattern);
        t.add(flags);
        return t;
    }

    private static Term functionCallAst(FunctionCallAst f) {
        String name = SparqlBuiltinFunctionNameResolver.fromFunctionTerm(f.functionName());
        Term t = Term.function(name);
        for (TermAst arg : f.arguments()) {
            t.add(convert(arg));
        }
        return t;
    }

    private static Term variadicTerm(String name, List<TermAst> args) {
        Term t = Term.function(name);
        for (TermAst arg : args) {
            t.add(convert(arg));
        }
        return t;
    }

    private static Term replaceToTerm(ReplaceAst r) {
        Term t =
                Term.function(
                        "replace",
                        convert(r.getString()),
                        convert(r.getPattern()),
                        convert(r.getReplacement()));
        if (r.hasFlags()) {
            t.add(convert(r.getFlags()));
        }
        return t;
    }

    private static Term substrToTerm(SubstrAst s) {
        if (s.getLength() != null) {
            return Term.function(
                    "substr",
                    convert(s.getString()),
                    convert(s.getStart()),
                    convert(s.getLength()));
        }
        return Term.function("substr", convert(s.getString()), convert(s.getStart()));
    }

    private static Term bnodeToTerm(BnodeAst b) {
        if (b.getLabel() == null) {
            return Term.function(Processor.BNODE);
        }
        return Term.function(Processor.BNODE, convert(b.getLabel()));
    }

    private static final class KeywordHolder {
        static final String SEAND = fr.inria.corese.core.sparql.triple.cst.Keyword.SEAND;
        static final String SEOR = fr.inria.corese.core.sparql.triple.cst.Keyword.SEOR;
    }
}
