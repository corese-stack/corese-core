package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.Values;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import java.math.BigInteger;
import java.util.regex.Pattern;

/** Validates numeric RDF literals before using their XML Schema values. */
final class NativeNumericValues {
    static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    static final Pattern DECIMAL = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)");

    private NativeNumericValues() {}

    static String whitespaceCollapsed(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && xmlWhitespace(value.charAt(start))) start++;
        while (end > start && xmlWhitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean xmlWhitespace(char value) {
        return value == ' ' || value == '\t' || value == '\r' || value == '\n';
    }

    static String lexical(Literal literal, Pattern pattern) {
        String text = whitespaceCollapsed(literal.getLabel());
        if (!pattern.matcher(text).matches()) {
            throw new QueryTypeErrorException("Invalid XML Schema numeric lexical form");
        }
        return text;
    }

    static void validate(Literal literal) {
        if (!literal.isNumber() || !(literal.getCoreDatatype() instanceof XSDDatatype datatype)) {
            throw new QueryTypeErrorException("Expected an XML Schema numeric literal");
        }
        switch (datatype) {
            case FLOAT, DOUBLE -> floatingLexical(literal);
            case DECIMAL -> lexical(literal, DECIMAL);
            default -> validateInteger(new BigInteger(lexical(literal, INTEGER)), datatype);
        }
    }

    static Literal normalized(Literal literal) {
        validate(literal);
        String lexical = whitespaceCollapsed(literal.getLabel());
        return lexical.equals(literal.getLabel()) ? literal
                : Values.factory().createLiteral(lexical, literal.getDatatype());
    }

    static String floatingLexical(Literal literal) {
        String text = whitespaceCollapsed(literal.getLabel());
        validateFloatingLexical(text);
        return text;
    }

    private static void validateFloatingLexical(String text) {
        if (text.equals("INF") || text.equals("-INF") || text.equals("NaN")) return;
        int exponent = Math.max(text.indexOf('e'), text.indexOf('E'));
        String mantissa = exponent < 0 ? text : text.substring(0, exponent);
        boolean validExponent = exponent < 0 || INTEGER.matcher(text.substring(exponent + 1)).matches();
        if (!validExponent || !DECIMAL.matcher(mantissa).matches()) {
            throw new QueryTypeErrorException("Invalid XML Schema floating point lexical form");
        }
    }

    private static void validateInteger(BigInteger value, XSDDatatype datatype) {
        boolean valid = switch (datatype) {
            case BYTE -> value.bitLength() < 8;
            case SHORT -> value.bitLength() < 16;
            case INT -> value.bitLength() < 32;
            case LONG -> value.bitLength() < 64;
            case UNSIGNED_BYTE -> unsigned(value, 8);
            case UNSIGNED_SHORT -> unsigned(value, 16);
            case UNSIGNED_INT -> unsigned(value, 32);
            case UNSIGNED_LONG -> unsigned(value, 64);
            case POSITIVE_INTEGER -> value.signum() > 0;
            case NEGATIVE_INTEGER -> value.signum() < 0;
            case NON_NEGATIVE_INTEGER -> value.signum() >= 0;
            case NON_POSITIVE_INTEGER -> value.signum() <= 0;
            default -> true;
        };
        if (!valid) throw new QueryTypeErrorException("Integer value is outside its datatype value space");
    }

    private static boolean unsigned(BigInteger value, int bits) {
        return value.signum() >= 0 && value.bitLength() <= bits;
    }

    static boolean booleanValue(Literal literal) {
        literal = normalized(literal);
        if (literal.getCoreDatatype() == XSDDatatype.DOUBLE || literal.getCoreDatatype() == XSDDatatype.FLOAT) {
            double value = literal.getCoreDatatype() == XSDDatatype.FLOAT ? literal.floatValue() : literal.doubleValue();
            return value != 0 && !Double.isNaN(value);
        }
        return literal.decimalValue().signum() != 0;
    }
}
