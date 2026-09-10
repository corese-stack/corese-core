package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import javax.xml.datatype.DatatypeConstants;

import static fr.inria.corese.core.next.query.impl.sparql.bridge.NativeNumericValues.DECIMAL;
import static fr.inria.corese.core.next.query.impl.sparql.bridge.NativeNumericValues.INTEGER;
import static fr.inria.corese.core.next.query.impl.sparql.bridge.NativeNumericValues.floatingLexical;
import static fr.inria.corese.core.next.query.impl.sparql.bridge.NativeNumericValues.lexical;
import static fr.inria.corese.core.next.query.impl.sparql.bridge.NativeNumericValues.whitespaceCollapsed;

/** XML Schema constructors supported by SPARQL 1.1, without extension dispatch. */
final class NativeCastExpressionEvaluator {
    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";
    private static final String STRING = "string";
    private static final Set<String> TARGETS = Set.of(STRING, "integer", "decimal", "double", "float", "boolean", "dateTime");

    private NativeCastExpressionEvaluator() {}

    static DatatypeValue evaluate(FunctionCallAst function, NativeEvaluationContext context) {
        String iri = context.constant(function.functionName()).stringValue();
        if (!iri.startsWith(XSD) || !TARGETS.contains(iri.substring(XSD.length()))) {
            throw new UnsupportedQueryFeatureException("Extension function evaluation is not supported yet: " + iri);
        }
        if (function.arguments().size() != 1) {
            throw new QueryTypeErrorException("An XML Schema constructor requires exactly one argument");
        }
        DatatypeValue value = context.required(function.arguments().getFirst());
        String target = iri.substring(XSD.length());
        if (target.equals(STRING) && value.isIRI()) {
            return context.values().createLiteral(value.stringValue());
        }
        if (!(value instanceof Literal literal) || literal.getLanguage().isPresent()) {
            throw new QueryTypeErrorException("XML Schema constructors require an untagged literal");
        }
        if (literal.isNumber()) literal = NativeNumericValues.normalized(literal);
        return switch (target) {
            case STRING -> context.values().createLiteral(stringValue(literal));
            case "integer" -> context.values().createLiteral(integerValue(literal));
            case "decimal" -> context.values().createLiteral(decimalValue(literal));
            case "double" -> context.values().createLiteral(floatingValue(literal));
            case "float" -> context.values().createLiteral((float) floatingValue(literal));
            case "boolean" -> context.values().createLiteral(booleanValue(literal));
            case "dateTime" -> dateTime(literal, context);
            default -> throw new IllegalStateException("Unrecognized validated constructor: " + target);
        };
    }

    @SuppressWarnings("java:S2111") // XPath casts truncate the exact binary value, not its decimal display string.
    private static BigInteger integerValue(Literal value) {
        if (value.getCoreDatatype() == XSDDatatype.STRING) {
            return new BigInteger(lexical(value, INTEGER));
        }
        if (value.getCoreDatatype() == XSDDatatype.DOUBLE || value.getCoreDatatype() == XSDDatatype.FLOAT) {
            double number = floatingValue(value);
            if (!Double.isFinite(number)) {
                throw new QueryTypeErrorException("NaN and infinity cannot be cast to integer");
            }
            // Truncate the binary value itself, not its rounded decimal display string.
            return new BigDecimal(number).toBigInteger();
        }
        return decimalValue(value).toBigInteger();
    }

    private static BigDecimal decimalValue(Literal value) {
        if (value.getCoreDatatype() == XSDDatatype.STRING) {
            return new BigDecimal(lexical(value, DECIMAL));
        }
        if (value.getCoreDatatype() == XSDDatatype.BOOLEAN) {
            return booleanValue(value) ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        requireNumeric(value);
        if (value.getCoreDatatype() == XSDDatatype.FLOAT || value.getCoreDatatype() == XSDDatatype.DOUBLE) {
            double number = value.getCoreDatatype() == XSDDatatype.FLOAT ? value.floatValue() : value.doubleValue();
            if (!Double.isFinite(number)) {
                throw new QueryTypeErrorException("NaN and infinity cannot be cast to decimal or integer");
            }
            return BigDecimal.valueOf(number);
        }
        return value.decimalValue();
    }

    private static double floatingValue(Literal value) {
        if (value.getCoreDatatype() == XSDDatatype.STRING) {
            return switch (floatingLexical(value)) {
                case "INF" -> Double.POSITIVE_INFINITY;
                case "-INF" -> Double.NEGATIVE_INFINITY;
                case "NaN" -> Double.NaN;
                default -> Double.parseDouble(whitespaceCollapsed(value.getLabel()));
            };
        }
        if (value.getCoreDatatype() == XSDDatatype.BOOLEAN) {
            return booleanValue(value) ? 1.0 : 0.0;
        }
        requireNumeric(value);
        return value.getCoreDatatype() == XSDDatatype.FLOAT ? value.floatValue() : value.doubleValue();
    }

    private static boolean booleanValue(Literal value) {
        if (value.getCoreDatatype() == XSDDatatype.STRING || value.getCoreDatatype() == XSDDatatype.BOOLEAN) {
            return switch (whitespaceCollapsed(value.getLabel())) {
                case "true", "1" -> true;
                case "false", "0" -> false;
                default -> throw new QueryTypeErrorException("Invalid xsd:boolean lexical form");
            };
        }
        requireNumeric(value);
        return NativeNumericValues.booleanValue(value);
    }

    private static String stringValue(Literal value) {
        if (value.getCoreDatatype() == XSDDatatype.BOOLEAN) {
            return Boolean.toString(booleanValue(value));
        }
        if (value.isNumber()) {
            NativeNumericValues.validate(value);
            return numericString(value);
        }
        return value.getLabel();
    }

    private static String numericString(Literal value) {
        if (value.getCoreDatatype() != XSDDatatype.FLOAT && value.getCoreDatatype() != XSDDatatype.DOUBLE) {
            return value.decimalValue().stripTrailingZeros().toPlainString();
        }
        double number = value.getCoreDatatype() == XSDDatatype.FLOAT ? value.floatValue() : value.doubleValue();
        if (Double.isNaN(number)) return "NaN";
        if (Double.isInfinite(number)) return number < 0 ? "-INF" : "INF";
        return floatingString(value, number);
    }

    private static String floatingString(Literal literal, double number) {
        if (number == 0) return Double.doubleToRawLongBits(number) < 0 ? "-0" : "0";
        BigDecimal decimal = new BigDecimal(literal.getCoreDatatype() == XSDDatatype.FLOAT
                ? Float.toString((float) number) : Double.toString(number)).stripTrailingZeros();
        double magnitude = Math.abs(number);
        if (magnitude >= 0.000001 && magnitude < 1000000) return decimal.toPlainString();
        int exponent = decimal.precision() - decimal.scale() - 1;
        BigDecimal mantissa = decimal.movePointLeft(exponent);
        if (mantissa.scale() < 1) mantissa = mantissa.setScale(1);
        return mantissa.toPlainString() + "E" + exponent;
    }

    private static DatatypeValue dateTime(Literal value, NativeEvaluationContext context) {
        if (value.getCoreDatatype() != XSDDatatype.STRING && value.getCoreDatatype() != XSDDatatype.DATETIME) {
            throw new QueryTypeErrorException("Cannot cast this datatype to xsd:dateTime");
        }
        Literal result = context.values().createLiteral(whitespaceCollapsed(value.getLabel()), XSDDatatype.DATETIME.getIRI());
        if (!DatatypeConstants.DATETIME.equals(result.calendarValue().getXMLSchemaType())) {
            throw new QueryTypeErrorException("Invalid xsd:dateTime lexical form");
        }
        return result;
    }

    private static void requireNumeric(Literal value) {
        NativeNumericValues.validate(value);
    }
}
