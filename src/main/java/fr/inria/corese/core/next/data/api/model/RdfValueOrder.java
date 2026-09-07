package fr.inria.corese.core.next.data.api.model;

import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Triple;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Deterministic total order for RDF values used by query sorting and KGRAM
 * collections.
 *
 * <p>The primary order follows SPARQL 1.1: blank nodes, IRIs, then literals.
 * RDF 1.2 triple terms follow RDF 1.1 terms. Within a term family, comparison
 * is deterministic and value-aware for compatible numeric and boolean
 * literals, with datatype, language and lexical form as stable tie-breakers.</p>
 */
public final class RdfValueOrder implements Comparator<DatatypeValue> {

    /** Shared stateless comparator. */
    public static final RdfValueOrder INSTANCE = new RdfValueOrder();

    private RdfValueOrder() {
    }

    /** Compares two values, sorting {@code null} before every bound RDF term. */
    public static int compareValues(DatatypeValue left, DatatypeValue right) {
        return compareInternal(left, right);
    }

    @Override
    public int compare(DatatypeValue left, DatatypeValue right) {
        return compareInternal(left, right);
    }

    private static int compareInternal(DatatypeValue left, DatatypeValue right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }

        int kindComparison = Integer.compare(kindRank(left), kindRank(right));
        if (kindComparison != 0) {
            return kindComparison;
        }
        if (left instanceof Literal leftLiteral && right instanceof Literal rightLiteral) {
            return compareLiterals(leftLiteral, rightLiteral);
        }
        if (left instanceof Triple leftTriple && right instanceof Triple rightTriple) {
            int subject = compareValues(leftTriple.subject(), rightTriple.subject());
            if (subject != 0) {
                return subject;
            }
            int predicate = compareValues(leftTriple.predicate(), rightTriple.predicate());
            return predicate != 0
                    ? predicate
                    : compareValues(leftTriple.object(), rightTriple.object());
        }
        return left.stringValue().compareTo(right.stringValue());
    }

    private static int kindRank(DatatypeValue value) {
        if (value instanceof BNode) {
            return 0;
        }
        if (value instanceof IRI) {
            return 1;
        }
        if (value instanceof Literal) {
            return 2;
        }
        if (value instanceof Triple) {
            return 3;
        }
        return 4;
    }

    private static int compareLiterals(Literal left, Literal right) {
        int familyComparison = Integer.compare(literalFamily(left), literalFamily(right));
        if (familyComparison != 0) {
            return familyComparison;
        }

        int valueComparison = compareCompatibleValues(left, right);
        if (valueComparison != 0) {
            return valueComparison;
        }

        int datatypeComparison = datatype(left).compareTo(datatype(right));
        if (datatypeComparison != 0) {
            return datatypeComparison;
        }
        int languageComparison = left.getLanguage().orElse("")
                .compareToIgnoreCase(right.getLanguage().orElse(""));
        return languageComparison != 0
                ? languageComparison
                : left.getLabel().compareTo(right.getLabel());
    }

    private static int compareCompatibleValues(Literal left, Literal right) {
        if (isNumeric(left) && isNumeric(right)) {
            if (isFloatingPoint(left) || isFloatingPoint(right)) {
                return compareFloatingPoint(left.doubleValue(), right.doubleValue());
            }
            return decimalValue(left).compareTo(decimalValue(right));
        }
        if (isBoolean(left) && isBoolean(right)) {
            return Boolean.compare(left.booleanValue(), right.booleanValue());
        }
        return left.getLabel().compareTo(right.getLabel());
    }

    private static int compareFloatingPoint(double left, double right) {
        if (Double.isNaN(left)) {
            return Double.isNaN(right) ? 0 : -1;
        }
        if (Double.isNaN(right)) {
            return 1;
        }
        return Double.compare(left, right);
    }

    private static BigDecimal decimalValue(Literal literal) {
        if (isInteger(literal)) {
            return new BigDecimal(literal.integerValue());
        }
        return literal.decimalValue();
    }

    private static boolean isNumeric(Literal literal) {
        CoreDatatype datatype = literal.getCoreDatatype();
        return datatype instanceof XSDDatatype xsd && switch (xsd) {
            case BYTE, SHORT, INT, LONG, INTEGER,
                    UNSIGNED_BYTE, UNSIGNED_SHORT, UNSIGNED_INT, UNSIGNED_LONG,
                    POSITIVE_INTEGER, NEGATIVE_INTEGER, NON_NEGATIVE_INTEGER,
                    NON_POSITIVE_INTEGER, DECIMAL, FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

    private static boolean isInteger(Literal literal) {
        CoreDatatype datatype = literal.getCoreDatatype();
        return datatype instanceof XSDDatatype xsd && switch (xsd) {
            case BYTE, SHORT, INT, LONG, INTEGER,
                    UNSIGNED_BYTE, UNSIGNED_SHORT, UNSIGNED_INT, UNSIGNED_LONG,
                    POSITIVE_INTEGER, NEGATIVE_INTEGER, NON_NEGATIVE_INTEGER,
                    NON_POSITIVE_INTEGER -> true;
            default -> false;
        };
    }

    private static boolean isFloatingPoint(Literal literal) {
        return literal.getCoreDatatype() == XSDDatatype.FLOAT
                || literal.getCoreDatatype() == XSDDatatype.DOUBLE;
    }

    private static boolean isBoolean(Literal literal) {
        return literal.getCoreDatatype() == XSDDatatype.BOOLEAN;
    }

    private static int literalFamily(Literal literal) {
        if (isNumeric(literal)) {
            return 0;
        }
        if (isBoolean(literal)) {
            return 1;
        }
        if (literal.getCoreDatatype() == XSDDatatype.STRING) {
            return 2;
        }
        if (literal.getCoreDatatype() == RDFDatatype.LANGSTRING) {
            return 3;
        }
        return 4;
    }

    private static String datatype(Literal literal) {
        return literal.getDatatype() == null ? "" : literal.getDatatype().stringValue();
    }
}
