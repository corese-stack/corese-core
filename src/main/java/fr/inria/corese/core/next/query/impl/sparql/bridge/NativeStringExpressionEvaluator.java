package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ConcatAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EncodeForUriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LangAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LcaseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LiteralExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Md5Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ReplaceAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha1Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha256Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha384Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha512Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrAfterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrBeforeAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrDtAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrLangAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrUuidAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SubstrAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TrinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TzAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UcaseAst;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Evaluates SPARQL expressions that produce string literals. */
final class NativeStringExpressionEvaluator {

    private NativeStringExpressionEvaluator() {
    }

    static DatatypeValue evaluate(
            LiteralExpressionAst expression,
            NativeEvaluationContext context) {
        return switch (expression) {
            case StrAst unary -> context.values().createLiteral(
                    context.required(unary.argument()).stringValue());
            case StrAfterAst binary -> after(binary, context);
            case StrBeforeAst binary -> before(binary, context);
            case StrLangAst binary -> stringWithLanguage(binary, context);
            case StrDtAst binary -> stringWithDatatype(binary, context);
            case LangAst unary -> language(unary.argument(), context);
            case LcaseAst unary -> changeCase(unary.argument(), context, false);
            case UcaseAst unary -> changeCase(unary.argument(), context, true);
            case EncodeForUriAst unary -> context.values().createLiteral(
                    encodeForUri(context.stringLiteral(unary.argument()).getLabel()));
            case ConcatAst concat -> concat(concat.arguments(), context);
            case ReplaceAst replace -> replace(replace, context);
            case SubstrAst substring -> substring(substring, context);
            case Md5Ast unary -> digest("MD5", unary.argument(), context);
            case Sha1Ast unary -> digest("SHA-1", unary.argument(), context);
            case Sha256Ast unary -> digest("SHA-256", unary.argument(), context);
            case Sha384Ast unary -> digest("SHA-384", unary.argument(), context);
            case Sha512Ast unary -> digest("SHA-512", unary.argument(), context);
            case StrUuidAst ignored -> context.values().createLiteral(UUID.randomUUID().toString());
            case TzAst unary -> context.values().createLiteral(
                    NativeTemporalExpressionEvaluator.timezoneLabel(
                            NativeTemporalExpressionEvaluator.calendar(unary.argument(), context)));
            default -> throw unsupported(expression);
        };
    }

    static boolean startsWith(BinaryConstraintAst expression, NativeEvaluationContext context) {
        StringOperands operands = stringOperands(expression, context);
        return operands.left().startsWith(operands.right());
    }

    static boolean endsWith(BinaryConstraintAst expression, NativeEvaluationContext context) {
        StringOperands operands = stringOperands(expression, context);
        return operands.left().endsWith(operands.right());
    }

    static boolean contains(BinaryConstraintAst expression, NativeEvaluationContext context) {
        StringOperands operands = stringOperands(expression, context);
        return operands.left().contains(operands.right());
    }

    static boolean regex(BinaryRegexAst expression, NativeEvaluationContext context) {
        String value = context.stringLiteral(expression.getString()).getLabel();
        String pattern = context.stringLiteral(expression.getPattern()).getLabel();
        return compilePattern(pattern, "").matcher(value).find();
    }

    static boolean regex(TrinaryRegexAst expression, NativeEvaluationContext context) {
        String value = context.stringLiteral(expression.getString()).getLabel();
        String pattern = context.stringLiteral(expression.getPattern()).getLabel();
        String flags = context.stringLiteral(expression.getFlags()).getLabel();
        return compilePattern(pattern, flags).matcher(value).find();
    }

    private static StringOperands stringOperands(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        Literal left = context.stringLiteral(expression.getLeftArgument());
        Literal right = context.stringLiteral(expression.getRightArgument());
        ensureCompatibleArguments(left, right);
        return new StringOperands(left.getLabel(), right.getLabel(), left);
    }

    private static DatatypeValue before(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        StringOperands operands = stringOperands(expression, context);
        int separator = operands.left().indexOf(operands.right());
        String result = separator < 0 ? "" : operands.left().substring(0, separator);
        return stringLike(operands.source(), result, context);
    }

    private static DatatypeValue after(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        StringOperands operands = stringOperands(expression, context);
        int separator = operands.left().indexOf(operands.right());
        String result = separator < 0
                ? ""
                : operands.left().substring(separator + operands.right().length());
        return stringLike(operands.source(), result, context);
    }

    private static DatatypeValue stringWithLanguage(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        String label = context.stringLiteral(expression.getLeftArgument()).getLabel();
        String language = context.stringLiteral(expression.getRightArgument()).getLabel();
        return context.values().createLiteral(label, language);
    }

    private static DatatypeValue stringWithDatatype(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        String label = context.stringLiteral(expression.getLeftArgument()).getLabel();
        DatatypeValue datatype = context.required(expression.getRightArgument());
        if (!datatype.isIRI()) {
            throw new QueryEvaluationException("STRDT expects an IRI as its second argument");
        }
        return context.values().createLiteral(label, context.values().createIRI(datatype.stringValue()));
    }

    private static DatatypeValue language(TermAst expression, NativeEvaluationContext context) {
        Literal literal = context.literal(expression);
        return context.values().createLiteral(literal.getLanguage().orElse(""));
    }

    private static DatatypeValue changeCase(
            TermAst expression,
            NativeEvaluationContext context,
            boolean upperCase) {
        Literal source = context.stringLiteral(expression);
        String result = upperCase
                ? source.getLabel().toUpperCase(Locale.ROOT)
                : source.getLabel().toLowerCase(Locale.ROOT);
        return stringLike(source, result, context);
    }

    private static DatatypeValue concat(List<TermAst> arguments, NativeEvaluationContext context) {
        StringBuilder result = new StringBuilder();
        String commonLanguage = null;
        boolean preserveLanguage = !arguments.isEmpty();
        for (TermAst argument : arguments) {
            Literal literal = context.stringLiteral(argument);
            result.append(literal.getLabel());
            String language = literal.getLanguage().orElse(null);
            if (commonLanguage == null) {
                commonLanguage = language;
            } else if (language == null || !commonLanguage.equalsIgnoreCase(language)) {
                preserveLanguage = false;
            }
            if (language == null) {
                preserveLanguage = false;
            }
        }
        return preserveLanguage
                ? context.values().createLiteral(result.toString(), commonLanguage)
                : context.values().createLiteral(result.toString());
    }

    private static DatatypeValue replace(ReplaceAst expression, NativeEvaluationContext context) {
        Literal source = context.stringLiteral(expression.getString());
        String pattern = context.stringLiteral(expression.getPattern()).getLabel();
        String replacement = context.stringLiteral(expression.getReplacement()).getLabel();
        String flags = expression.hasFlags()
                ? context.stringLiteral(expression.getFlags()).getLabel()
                : "";
        String result = compilePattern(pattern, flags)
                .matcher(source.getLabel())
                .replaceAll(replacement);
        return stringLike(source, result, context);
    }

    private static DatatypeValue substring(SubstrAst expression, NativeEvaluationContext context) {
        Literal source = context.stringLiteral(expression.getString());
        double start = NativeNumericExpressionEvaluator.numericDouble(
                context.required(expression.getStart()));
        Double length = expression.getLength() == null
                ? null
                : NativeNumericExpressionEvaluator.numericDouble(
                        context.required(expression.getLength()));
        String result = codePointSubstring(source.getLabel(), start, length);
        return stringLike(source, result, context);
    }

    private static String codePointSubstring(String value, double start, Double length) {
        if (Double.isNaN(start) || start == Double.POSITIVE_INFINITY
                || (length != null && Double.isNaN(length))) {
            return "";
        }
        int codePointCount = value.codePointCount(0, value.length());
        long roundedStart = xpathRound(start);
        long firstPosition = Math.max(1L, roundedStart);
        long endPosition = length == null
                ? codePointCount + 1L
                : saturatedAdd(roundedStart, xpathRound(length));
        long exclusivePosition = Math.min(codePointCount + 1L, endPosition);
        if (exclusivePosition <= firstPosition || firstPosition > codePointCount) {
            return "";
        }
        int from = value.offsetByCodePoints(0, Math.toIntExact(firstPosition - 1L));
        int to = value.offsetByCodePoints(0, Math.toIntExact(exclusivePosition - 1L));
        return value.substring(from, to);
    }

    private static long xpathRound(double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            return Long.MIN_VALUE;
        }
        if (value == Double.POSITIVE_INFINITY) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(value + 0.5d);
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return right < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }

    private static DatatypeValue digest(
            String algorithm,
            TermAst expression,
            NativeEvaluationContext context) {
        String value = context.stringLiteral(expression).getLabel();
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return context.values().createLiteral(HexFormat.of().formatHex(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new QueryEvaluationException("Digest algorithm unavailable: " + algorithm, exception);
        }
    }

    private static Pattern compilePattern(String expression, String flags) {
        return Pattern.compile(expression, regexOptions(flags));
    }

    private static int regexOptions(String flags) {
        int options = 0;
        if (flags.contains("i")) {
            options |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        if (flags.contains("m")) {
            options |= Pattern.MULTILINE;
        }
        if (flags.contains("s")) {
            options |= Pattern.DOTALL;
        }
        if (flags.contains("x")) {
            options |= Pattern.COMMENTS;
        }
        return options;
    }

    private static String encodeForUri(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    private static DatatypeValue stringLike(
            Literal source,
            String value,
            NativeEvaluationContext context) {
        return source.getLanguage()
                .<DatatypeValue>map(language -> context.values().createLiteral(value, language))
                .orElseGet(() -> context.values().createLiteral(value));
    }

    private static void ensureCompatibleArguments(Literal left, Literal right) {
        String leftLanguage = left.getLanguage().orElse(null);
        String rightLanguage = right.getLanguage().orElse(null);
        boolean compatible = rightLanguage == null
                || leftLanguage != null && leftLanguage.equalsIgnoreCase(rightLanguage);
        if (!compatible) {
            throw new QueryEvaluationException("String arguments have incompatible language tags");
        }
    }

    private static UnsupportedQueryFeatureException unsupported(
            LiteralExpressionAst expression) {
        return new UnsupportedQueryFeatureException(
                "String expression is not supported yet: " + expression.getClass().getSimpleName());
    }

    private record StringOperands(String left, String right, Literal source) {
    }
}
