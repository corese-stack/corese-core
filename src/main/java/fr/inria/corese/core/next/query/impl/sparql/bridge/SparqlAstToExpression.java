package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.sparql.datatype.RDF;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.parser.Processor;
import fr.inria.corese.core.sparql.triple.parser.Term;
import fr.inria.corese.core.sparql.triple.parser.Variable;

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
     * Converts a filter {@link TermAst} to an {@link Expression}, then wraps it as a
     * {@link Filter} with {@link Filter#coreseNextSource()} set to {@code filterExpression}.
     *
     * <p>Prefer {@link CoreseAstQueryBuilder#toNextFilter(TermAst)} at call sites that build queries; this
     * method is the shared implementation.
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
        String d = dt.trim();
        if (d.length() >= 2 && d.charAt(0) == '<' && d.charAt(d.length() - 1) == '>') {
            return d.substring(1, d.length() - 1);
        }
        return fr.inria.corese.core.sparql.triple.parser.NSManager.nsm().toNamespace(d);
    }

    private static Constant iriToConstant(IriAst i) {
        String raw = i.raw().trim();
        if (raw.startsWith("_:")) {
            return Constant.createBlank(raw.substring(2));
        }
        return Constant.createResource(raw);
    }

    private static Expression constraintToExpression(ConstraintAst c) {
        return switch (c) {
            case AndAst a -> Term.create(KeywordHolder.SEAND, convert(a.getLeftArgument()), convert(a.getRightArgument()));
            case OrAst o -> Term.create(KeywordHolder.SEOR, convert(o.getLeftArgument()), convert(o.getRightArgument()));
            case EqualsAst e -> Term.create("=", convert(e.getLeftArgument()), convert(e.getRightArgument()));
            case DifferentAst d -> Term.create("!=", convert(d.getLeftArgument()), convert(d.getRightArgument()));
            case LowerThanAst x -> Term.create("<", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case LowerOrEqualThanAst x -> Term.create("<=", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case GreaterThanAst x -> Term.create(">", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case GreaterOrEqualThanAst x -> Term.create(">=", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case AddAst x -> Term.create("+", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case SubtractAst x -> Term.create("-", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case MultiplyAst x -> Term.create("*", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case DivideAst x -> Term.create("/", convert(x.getLeftArgument()), convert(x.getRightArgument()));
            case UnaryPlusAst u -> Term.create("+", convert(u.getArgument()));
            case UnaryMinusAst u -> Term.create("-", convert(u.getArgument()));
            case BooleanNotAst u -> Term.create("!", convert(u.getArgument()));
            case BoundAst u -> functionTerm(Processor.BOUND, convert(u.getArgument()));
            case IsIriAst u -> functionTerm("isIRI", convert(u.getArgument()));
            case IsBlankAst u -> functionTerm("isBlank", convert(u.getArgument()));
            case IsLiteralAst u -> functionTerm("isLiteral", convert(u.getArgument()));
            case StrAst u -> functionTerm("str", convert(u.getArgument()));
            case LangAst u -> functionTerm("lang", convert(u.getArgument()));
            case DatatypeAst u -> functionTerm("datatype", convert(u.getArgument()));
            case SameTermAst b -> functionTerm("sameTerm", convert(b.getLeftArgument()), convert(b.getRightArgument()));
            case LangMatchesAst b -> functionTerm("langMatches", convert(b.getLeftArgument()), convert(b.getRightArgument()));
            case BinaryRegexAst r -> regexTerm(convert(r.getString()), convert(r.getPattern()));
            case TrinaryRegexAst r -> regexTerm(convert(r.getString()), convert(r.getPattern()), convert(r.getFlags()));
            case FunctionCallAst f -> functionCallAst(f);
            case ExistsAst ignored ->
                    throw new UnsupportedOperationException("EXISTS { ... } conversion requires GroupGraphPatternAst → Exp (see CoreseAstQueryBuilder)");
            case NotExistsAst ignored ->
                    throw new UnsupportedOperationException("NOT EXISTS { ... } conversion requires GroupGraphPatternAst → Exp (see CoreseAstQueryBuilder)");
            default ->
                    throw new UnsupportedOperationException("Unsupported constraint AST: " + c.getClass().getName());
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

    private static final class KeywordHolder {
        static final String SEAND = fr.inria.corese.core.sparql.triple.cst.Keyword.SEAND;
        static final String SEOR = fr.inria.corese.core.sparql.triple.cst.Keyword.SEOR;
    }
}
