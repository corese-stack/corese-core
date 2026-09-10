package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
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
            case StrAst unary -> str(context.required(unary.argument()), context);
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
        String pattern = context.simpleString(expression.getPattern()).getLabel();
        return compilePattern(pattern, "").matcher(value).find();
    }

    static boolean regex(TrinaryRegexAst expression, NativeEvaluationContext context) {
        String value = context.stringLiteral(expression.getString()).getLabel();
        String pattern = context.simpleString(expression.getPattern()).getLabel();
        String flags = context.simpleString(expression.getFlags()).getLabel();
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
        if (separator < 0) return context.values().createLiteral("");
        String result = operands.left().substring(0, separator);
        return stringLike(operands.source(), result, context);
    }

    private static DatatypeValue after(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        StringOperands operands = stringOperands(expression, context);
        int separator = operands.left().indexOf(operands.right());
        if (separator < 0) return context.values().createLiteral("");
        String result = operands.left().substring(separator + operands.right().length());
        return stringLike(operands.source(), result, context);
    }

    private static DatatypeValue stringWithLanguage(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        String label = context.simpleString(expression.getLeftArgument()).getLabel();
        String language = context.simpleString(expression.getRightArgument()).getLabel();
        return context.values().createLiteral(label, language);
    }

    private static DatatypeValue stringWithDatatype(
            BinaryConstraintAst expression,
            NativeEvaluationContext context) {
        String label = context.simpleString(expression.getLeftArgument()).getLabel();
        DatatypeValue datatype = context.required(expression.getRightArgument());
        if (!datatype.isIRI()) {
            throw new QueryTypeErrorException("STRDT expects an IRI as its second argument");
        }
        return context.values().createLiteral(label, context.values().createIRI(datatype.stringValue()));
    }

    private static DatatypeValue str(DatatypeValue value, NativeEvaluationContext context) {
        if (!value.isIRI() && !(value instanceof Literal)) {
            throw new QueryTypeErrorException("STR expects an IRI or a literal");
        }
        return context.values().createLiteral(value.stringValue());
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
        String pattern = context.simpleString(expression.getPattern()).getLabel();
        String replacement = context.simpleString(expression.getReplacement()).getLabel();
        String flags = expression.hasFlags()
                ? context.simpleString(expression.getFlags()).getLabel()
                : "";
        Pattern compiled = compilePattern(pattern, flags);
        if (compiled.matcher("").find()) {
            throw new QueryTypeErrorException("REPLACE pattern must not match an empty string");
        }
        try {
            String result = compiled.matcher(source.getLabel()).replaceAll(replacement);
            return stringLike(source, result, context);
        } catch (IndexOutOfBoundsException invalidGroup) {
            throw new QueryTypeErrorException("Invalid replacement group", invalidGroup);
        }
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
        double firstPosition = Math.max(1, xpathRound(start));
        double endPosition = length == null ? Double.POSITIVE_INFINITY : xpathRound(start) + xpathRound(length);
        if (Double.isNaN(firstPosition) || Double.isNaN(endPosition)) return "";
        int count = value.codePointCount(0, value.length());
        double exclusivePosition = Math.min(count + 1.0, endPosition);
        if (exclusivePosition <= firstPosition || firstPosition > count) return "";
        int from = value.offsetByCodePoints(0, (int) firstPosition - 1);
        int to = value.offsetByCodePoints(0, (int) exclusivePosition - 1);
        return value.substring(from, to);
    }

    private static double xpathRound(double value) {
        if (!Double.isFinite(value) || value == 0) return value;
        double floor = Math.floor(value);
        return value - floor >= 0.5 ? floor + 1 : floor;
    }

    private static DatatypeValue digest(
            String algorithm,
            TermAst expression,
            NativeEvaluationContext context) {
        String value = context.simpleString(expression).getLabel();
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return context.values().createLiteral(HexFormat.of().formatHex(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new QueryEvaluationException("Digest algorithm unavailable: " + algorithm, exception);
        }
    }

    private static Pattern compilePattern(String expression, String flags) {
        int options = regexOptions(flags);
        return Pattern.compile(flags.contains("x") ? removePatternWhitespace(expression) : expression, options);
    }

    private static String removePatternWhitespace(String expression) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        int brackets = 0;
        for (int index = 0; index < expression.length(); index++) {
            char character = expression.charAt(index);
            if (!escaped) {
                if (character == '[') brackets++;
                if (character == ']') brackets--;
                if (brackets == 0 && " \t\r\n".indexOf(character) >= 0) continue;
            }
            result.append(character);
            escaped = !escaped && character == '\\';
        }
        return result.toString();
    }

    private static int regexOptions(String flags) {
        if (!flags.chars().allMatch(flag -> "imsx".indexOf(flag) >= 0)) {
            throw new QueryTypeErrorException("Invalid regular expression flags");
        }
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
        return options;
    }

    private static String encodeForUri(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
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
            throw new QueryTypeErrorException("String arguments have incompatible language tags");
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
