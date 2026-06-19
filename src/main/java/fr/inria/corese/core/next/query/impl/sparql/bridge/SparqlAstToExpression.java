package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.util.StringUtils;
import fr.inria.corese.core.sparql.datatype.RDF;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.parser.Processor;
import fr.inria.corese.core.sparql.triple.parser.Term;
import fr.inria.corese.core.sparql.triple.parser.Variable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts Corese-next {@link TermAst} nodes (including {@link ConstraintAst}) into
 * {@link Expression} trees for the SPARQL interpreter, consumable from KGRAM “next” via
 * {@link fr.inria.corese.core.next.query.kgram.api.core.Filter} / {@link AstBackedExpr}.
 *
 */
public final class SparqlAstToExpression {

    private SparqlAstToExpression() {
    }

    /**
     * Converts any {@link TermAst} (variable, literal, IRI, or constraint expression) to {@link Expression}.
     */
    public static Expression convert(TermAst term) {
        return switch (term) {
            case VarAst v -> Variable.create(v.name());
            case LiteralAst l -> literalToConstant(l);
            case IriAst i -> iriToConstant(i);
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
        AstBackedExpr expr = new AstBackedExpr(exprTree, Optional.of(filterExpression));
        return expr.getFilter();
    }

    private static Constant literalToConstant(LiteralAst l) {
        if (l.lang() != null && !l.lang().isEmpty()) {
            return Constant.create(unquoteLexical(l.lexical()), RDF.rdflangString, l.lang());
        }
        if (l.datatype() != null && !l.datatype().isEmpty()) {
            return Constant.create(unquoteLexical(l.lexical()), normalizeDatatypeIri(l.datatype()), null);
        }
        return Constant.createString(unquoteLexical(l.lexical()));
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

    private static Constant iriToConstant(IriAst i) {
        String raw = StringUtils.trimChevronIRIs(i.raw());
        if (raw.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return Constant.createBlank(raw.substring(IOConstants.BLANK_NODE_PREFIX.length()));
        }
        return Constant.createResource(raw);
    }

    private static Expression constraintToExpression(ConstraintAst constraint) {
        return switch (constraint) {
            case AndAst andAst ->
                    Term.create(KeywordHolder.SEAND, convert(andAst.getLeftArgument()), convert(andAst.getRightArgument()));
            case OrAst orAst ->
                    Term.create(KeywordHolder.SEOR, convert(orAst.getLeftArgument()), convert(orAst.getRightArgument()));
            case EqualsAst equalsAst ->
                    Term.create("=", convert(equalsAst.getLeftArgument()), convert(equalsAst.getRightArgument()));
            case DifferentAst differentAst ->
                    Term.create("!=", convert(differentAst.getLeftArgument()), convert(differentAst.getRightArgument()));
            case LowerThanAst lowerThanAst ->
                    Term.create("<", convert(lowerThanAst.getLeftArgument()), convert(lowerThanAst.getRightArgument()));
            case LowerOrEqualThanAst lowerOrEqualThanAst ->
                    Term.create("<=", convert(lowerOrEqualThanAst.getLeftArgument()), convert(lowerOrEqualThanAst.getRightArgument()));
            case GreaterThanAst greaterThanAst ->
                    Term.create(">", convert(greaterThanAst.getLeftArgument()), convert(greaterThanAst.getRightArgument()));
            case GreaterOrEqualThanAst greaterOrEqualThanAst ->
                    Term.create(">=", convert(greaterOrEqualThanAst.getLeftArgument()), convert(greaterOrEqualThanAst.getRightArgument()));
            case AddAst addAst ->
                    Term.create("+", convert(addAst.getLeftArgument()), convert(addAst.getRightArgument()));
            case SubtractAst subtractAst ->
                    Term.create("-", convert(subtractAst.getLeftArgument()), convert(subtractAst.getRightArgument()));
            case MultiplyAst multiplyAst ->
                    Term.create("*", convert(multiplyAst.getLeftArgument()), convert(multiplyAst.getRightArgument()));
            case DivideAst divideAst ->
                    Term.create("/", convert(divideAst.getLeftArgument()), convert(divideAst.getRightArgument()));
            case UnaryPlusAst unaryPlusAst ->
                    Term.create("+", convert(unaryPlusAst.argument()));
            case UnaryMinusAst unaryMinusAst ->
                    Term.create("-", convert(unaryMinusAst.argument()));
            case BooleanNotAst booleanNotAst ->
                    Term.create("!", convert(booleanNotAst.argument()));
            case BoundAst boundAst ->
                    functionTerm(Processor.BOUND, convert(boundAst.argument()));
            case IsIriAst isIriAst ->
                    functionTerm("isIRI", convert(isIriAst.argument()));
            case IsBlankAst isBlankAst ->
                    functionTerm("isBlank", convert(isBlankAst.argument()));
            case IsLiteralAst isLiteralAst ->
                    functionTerm("isLiteral", convert(isLiteralAst.argument()));
            case StrAst strAst ->
                    functionTerm("str", convert(strAst.argument()));
            case LangAst langAst ->
                    functionTerm("lang", convert(langAst.argument()));
            case DatatypeAst datatypeAst ->
                    functionTerm("datatype", convert(datatypeAst.argument()));
            case SameTermAst sameTermAst ->
                    functionTerm("sameTerm", convert(sameTermAst.getLeftArgument()), convert(sameTermAst.getRightArgument()));
            case LangMatchesAst langMatchesAst ->
                    functionTerm("langMatches", convert(langMatchesAst.getLeftArgument()), convert(langMatchesAst.getRightArgument()));
            case BinaryRegexAst binaryRegexAst ->
                    regexTerm(convert(binaryRegexAst.getString()), convert(binaryRegexAst.getPattern()));
            case TrinaryRegexAst trinaryRegexAst ->
                    regexTerm(convert(trinaryRegexAst.getString()), convert(trinaryRegexAst.getPattern()), convert(trinaryRegexAst.getFlags()));
            case FunctionCallAst callAst ->
                    functionCallAst(callAst);
            case ConcatAst concatAst ->
                    variadicTerm("concat", concatAst.arguments());
            case CoalesceAst coalesceAst ->
                    variadicTerm(Processor.COALESCE, coalesceAst.arguments());
            case IfAst ifAst ->
                    Term.function(Processor.IF, convert(ifAst.condition()), convert(ifAst.thenExpr()), convert(ifAst.elseExpr()));
            case ReplaceAst replaceAst ->
                    replaceToTerm(replaceAst);
            case SubstrAst substrAst ->
                    substrToTerm(substrAst);
            case BnodeAst bnodeAst ->
                    bnodeToTerm(bnodeAst);
            case StrStartsAst strStartsAst ->
                    functionTerm("strstarts", convert(strStartsAst.getLeftArgument()), convert(strStartsAst.getRightArgument()));
            case StrEndsAst strEndsAst ->
                    functionTerm("strends", convert(strEndsAst.getLeftArgument()), convert(strEndsAst.getRightArgument()));
            case ContainsAst containsAst ->
                    functionTerm("contains", convert(containsAst.getLeftArgument()), convert(containsAst.getRightArgument()));
            case StrBeforeAst strBeforeAst ->
                    functionTerm("strbefore", convert(strBeforeAst.getLeftArgument()), convert(strBeforeAst.getRightArgument()));
            case StrAfterAst strAfterAst ->
                    functionTerm("strafter", convert(strAfterAst.getLeftArgument()), convert(strAfterAst.getRightArgument()));
            case StrLangAst strLangAst ->
                    functionTerm("strlang", convert(strLangAst.getLeftArgument()), convert(strLangAst.getRightArgument()));
            case StrDtAst strDtAst ->
                    functionTerm(Processor.STRDT, convert(strDtAst.getLeftArgument()), convert(strDtAst.getRightArgument()));
            case IriFunctionAst iriFunctionAst ->
                    functionTerm("iri", convert(iriFunctionAst.argument()));
            case LcaseAst lcaseAst ->
                    functionTerm("lcase", convert(lcaseAst.argument()));
            case UcaseAst ucaseAst ->
                    functionTerm("ucase", convert(ucaseAst.argument()));
            case EncodeForUriAst encodeForUriAst ->
                    functionTerm("encode_for_uri", convert(encodeForUriAst.argument()));
            case Md5Ast md5Ast ->
                    functionTerm("md5", convert(md5Ast.argument()));
            case Sha1Ast sha1Ast ->
                    functionTerm("sha1", convert(sha1Ast.argument()));
            case Sha256Ast sha256Ast ->
                    functionTerm("sha256", convert(sha256Ast.argument()));
            case Sha384Ast sha384Ast ->
                    functionTerm("sha384", convert(sha384Ast.argument()));
            case Sha512Ast sha512Ast ->
                    functionTerm("sha512", convert(sha512Ast.argument()));
            case ExistsAst existsAst ->
                    throw new UnsupportedOperationException(
                            "EXISTS { ... } conversion requires GroupGraphPatternAst → Exp (see CoreseAstQueryBuilder)");
            case NotExistsAst notExistsAst ->
                    throw new UnsupportedOperationException(
                            "NOT EXISTS { ... } conversion requires GroupGraphPatternAst → Exp (see CoreseAstQueryBuilder)");
            default ->
                    throw new UnsupportedOperationException(
                            "Unsupported constraint AST: " + constraint.getClass().getName());
        };
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
