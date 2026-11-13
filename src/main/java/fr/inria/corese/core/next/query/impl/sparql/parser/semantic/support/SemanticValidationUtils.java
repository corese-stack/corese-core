package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.Set;
import java.util.function.Consumer;

public class SemanticValidationUtils {

    private static final String BOOLEAN_DATATYPE = XSD.xsdBoolean.getIRI().stringValue();
    private static final Set<String> STRING_DATATYPE = Set.of(
            XSD.xsdString.getIRI().stringValue(),
            RDF.langString.getIRI().stringValue());

    /**
     * Valid numeric datatypes as defined in <a href="https://www.w3.org/TR/sparql11-query/#operandDataTypes">...</a>
     */
    private static final Set<String> NUMERIC_DATATYPES = Set.of(
            XSD.xsdInteger.getIRI().stringValue(),
            XSD.xsdNonNegativeInteger.getIRI().stringValue(),
            XSD.xsdNonPositiveInteger.getIRI().stringValue(),
            XSD.xsdPositiveInteger.getIRI().stringValue(),
            XSD.xsdNegativeInteger.getIRI().stringValue(),
            XSD.xsdInt.getIRI().stringValue(),
            XSD.xsdUnsignedInt.getIRI().stringValue(),
            XSD.xsdLong.getIRI().stringValue(),
            XSD.xsdUnsignedLong.getIRI().stringValue(),
            XSD.xsdDecimal.getIRI().stringValue(),
            XSD.xsdShort.getIRI().stringValue(),
            XSD.xsdUnsignedShort.getIRI().stringValue(),
            XSD.xsdByte.getIRI().stringValue(),
            XSD.xsdUnsignedByte.getIRI().stringValue(),
            XSD.xsdFloat.getIRI().stringValue(),
            XSD.xsdDouble.getIRI().stringValue());

    private SemanticValidationUtils() {
    }

    public static boolean isBooleanCompatible(LiteralAst literalAst) {
        if (literalAst.lang() != null && !literalAst.lang().isBlank()) {
            return true;
        }

        String datatype = literalAst.datatype();
        if (datatype == null) {
            return true;
        }

        return BOOLEAN_DATATYPE.equals(datatype)
                || STRING_DATATYPE.contains(datatype)
                || NUMERIC_DATATYPES.contains(datatype);
    }

    /**
     * Checks whether the given term is statically compatible with SPARQL
     * effective boolean value evaluation.
     * Literal datatype and lexical errors are resolved at runtime so logical
     * operators can apply the SPARQL error truth tables.
     *
     * @param termAst operand to inspect
     * @return whether the operand can be evaluated for its effective boolean value
     */
    public static boolean isPotentialBooleanCompatible(TermAst termAst) {
        if (termAst instanceof BooleanExpressionAst
                || termAst instanceof NumericExpressionAst
                || termAst instanceof VarAst
                || termAst instanceof FunctionCallAst
                || termAst instanceof UnlimitedArgumentsFunctionAst) {
            return true;
        }

        if (termAst instanceof LiteralAst) {
            return true;
        }

        if (termAst instanceof IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr)) {
            return isPotentialBooleanCompatible(condition)
                    && isPotentialBooleanCompatible(thenExpr)
                    && isPotentialBooleanCompatible(elseExpr);
        }

        if (termAst instanceof LiteralExpressionAst literalExpressionAst) {
            return !(literalExpressionAst instanceof XsdDateTimeExpressionAst
                    || literalExpressionAst instanceof XsdDayTimeDurationExpressionAst);
        }

        return false;
    }

    /**
     * Check that the given term is either numeric expression, literal expression typed by a standard numeric datatype, a literal that can be parsed to a numeric or a variable
     */
    public static boolean isPotentialNumeric(TermAst termAst) {
        if (isUnknownType(termAst)) {
            return true;
        }
        if (termAst instanceof IfAst ifAst
                && isPotentialNumeric(ifAst.thenExpr())
                && isPotentialNumeric(ifAst.elseExpr())) { // Is an IF that returns potential numerics
            return true;
        }
        if (termAst instanceof NumericExpressionAst) { // Is a literal expression that could be a numeric, we cannot know
            return true;
        }
        if (termAst instanceof LiteralAst(String lexical, String lang, String datatype)) {
            if(datatype != null) {
                return NUMERIC_DATATYPES.contains(datatype); // Datatype is an URI of an standard numeric datatype
            } else {
                return isNumeric(lexical); // The string value can be parsed to a numeric value
            }
        }

        return false;
    }

    /**
     * Check if the given term type cannot be determined at query parsing, it will be checked at query resolution.
     *
     */
    public static boolean isUnknownType(TermAst termAst) {
        return termAst instanceof VarAst
                || termAst instanceof FunctionCallAst;
    }

    /**
     * Tries to parse the string as a Double
     */
    public static boolean isNumeric(String lexical) {
        if (lexical == null) {
            return false;
        }
        try {
            Double.parseDouble(lexical);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Check if the term is either a variable or an IRI or an expression that can be resolved to an IRI
     */
    public static boolean isPotentialIri(TermAst termAst) {
        if(termAst instanceof IriAst) { // Is an IRI
            return true;
        }
        // Is a function that can be resolved to an IRI
        return termAst instanceof IriExpressionAst;
    }

    /**
     * Check if the term is a literal that can be considered as a string argument as defined in the SPARQL 1.1 recommendation
     */
    public static boolean isStringLiteral(TermAst termAst) {
        if(termAst instanceof LiteralAst(String lexical, String lang, String datatype) && lexical != null) {
            if(lang != null) {
                return true;
            }
            if(datatype == null) {
                return true;
            }
            if(STRING_DATATYPE.contains(datatype)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Check if the term is either a variable or a literal that can be considered as a string argument as defined in the SPARQL 1.1 recommendation
     */
    public static boolean isPotentialStringLiteral(TermAst termAst) {
        return isUnknownType(termAst) || isStringLiteral(termAst) || termAst instanceof SimpleLiteralExpressionAst;
    }

    /**
     * Returns {@code true} when the term is structurally valid in a FILTER or HAVING
     * but its use as an effective boolean value (EBV) is likely a mistake.
     *
     * <p>Suspicious patterns:</p>
     * <ul>
     *   <li>Numeric literal — EBV reduces to non-zero check; user probably meant a comparison.</li>
     *   <li>Plain or {@code xsd:string} literal — EBV reduces to non-empty check.</li>
     *   <li>String-producing built-in (e.g. {@code STR}, {@code CONCAT}, {@code LCASE}) —
     *       the EBV of a string is non-empty; user probably meant a boolean predicate.</li>
     *   <li>Numeric-producing built-in (e.g. {@code ABS}, {@code STRLEN}, {@code ROUND}) —
     *       EBV reduces to non-zero check; user probably meant an explicit comparison.</li>
     * </ul>
     *
     * <p>Boolean literals ({@code true}/{@code false}) are <em>not</em> flagged here — they have
     * a well-defined EBV and are reported separately as a "Constant condition" by
     * {@link #isConstantBooleanCondition(TermAst)}.</p>
     *
     * @param termAst the FILTER/HAVING operand to inspect
     * @return {@code true} if the term triggers a suspicious EBV coercion warning
     */
    public static boolean isSuspiciousEBVCoercion(TermAst termAst) {
        if (termAst instanceof LiteralAst literalAst) {
            // Boolean literals have a well-defined EBV — handled as "Constant condition", not coercion
            return !BOOLEAN_DATATYPE.equals(literalAst.datatype());
        }
        if (termAst instanceof SimpleLiteralExpressionAst) {
            return true;
        }
        if (termAst instanceof NumericExpressionAst) {
            return true;
        }
        if (termAst instanceof VarAst) {
            return true;
        }
        if (termAst instanceof IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr)) {
            return isSuspiciousEBVCoercion(condition)
                    || isSuspiciousEBVCoercion(thenExpr)
                    || isSuspiciousEBVCoercion(elseExpr);
        }
        return false;
    }

    /**
     * Visits expression terms without descending into graph patterns embedded in EXISTS.
     * Those patterns are visited separately, where their own FILTER locations are available.
     */
    public static void visitExpressionTerms(TermAst termAst, Consumer<TermAst> visitor) {
        if (termAst == null || visitor == null) {
            return;
        }
        visitor.accept(termAst);
        if (termAst instanceof ExistsAst || termAst instanceof NotExistsAst) {
            return;
        }
        if (termAst instanceof UnaryConstraintAst unaryConstraintAst) {
            visitExpressionTerms(unaryConstraintAst.argument(), visitor);
        } else if (termAst instanceof BinaryConstraintAst binaryConstraintAst) {
            visitExpressionTerms(binaryConstraintAst.getLeftArgument(), visitor);
            visitExpressionTerms(binaryConstraintAst.getRightArgument(), visitor);
        } else if (termAst instanceof UnlimitedArgumentsFunctionAst unlimitedArgumentsFunctionAst) {
            unlimitedArgumentsFunctionAst.arguments()
                    .forEach(argument -> visitExpressionTerms(argument, visitor));
        } else if (termAst instanceof FunctionCallAst functionCallAst) {
            functionCallAst.arguments().forEach(argument -> visitExpressionTerms(argument, visitor));
        } else if (termAst instanceof IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr)) {
            visitExpressionTerms(condition, visitor);
            visitExpressionTerms(thenExpr, visitor);
            visitExpressionTerms(elseExpr, visitor);
        } else if (termAst instanceof AggregateAst aggregateAst) {
            visitExpressionTerms(aggregateAst.expression(), visitor);
        } else if (termAst instanceof InAst(TermAst left, java.util.List<TermAst> candidates)) {
            visitExpressionTerms(left, visitor);
            candidates.forEach(candidate -> visitExpressionTerms(candidate, visitor));
        } else if (termAst instanceof NotInAst(TermAst left, java.util.List<TermAst> candidates)) {
            visitExpressionTerms(left, visitor);
            candidates.forEach(candidate -> visitExpressionTerms(candidate, visitor));
        }
    }

    /**
     * Identifies boolean conditions whose value can be determined from constants alone.
     */
    public static boolean isConstantBooleanCondition(TermAst termAst) {
        if (termAst instanceof LiteralAst literalAst) {
            return BOOLEAN_DATATYPE.equals(literalAst.datatype());
        }
        return termAst instanceof BooleanExpressionAst && isConstantExpression(termAst);
    }

    /**
     * Checks for ordering comparisons between statically known, incompatible value types.
     */
    public static boolean hasIncompatibleComparison(TermAst termAst) {
        if (termAst instanceof GreaterThanAst
                || termAst instanceof GreaterOrEqualThanAst
                || termAst instanceof LowerThanAst
                || termAst instanceof LowerOrEqualThanAst) {
            BinaryConstraintAst comparison = (BinaryConstraintAst) termAst;
            ValueType left = valueType(comparison.getLeftArgument());
            ValueType right = valueType(comparison.getRightArgument());
            if (left != null && right != null && left != right
                        && left != ValueType.IRI && right != ValueType.IRI) {
                return true;
            }
        }
        if (termAst instanceof UnaryConstraintAst unaryConstraintAst) {
            return hasIncompatibleComparison(unaryConstraintAst.argument());
        }
        if (termAst instanceof BinaryConstraintAst binaryConstraintAst) {
            return hasIncompatibleComparison(binaryConstraintAst.getLeftArgument())
                    || hasIncompatibleComparison(binaryConstraintAst.getRightArgument());
        }
        if (termAst instanceof UnlimitedArgumentsFunctionAst unlimitedArgumentsFunctionAst) {
            return unlimitedArgumentsFunctionAst.arguments().stream()
                    .anyMatch(SemanticValidationUtils::hasIncompatibleComparison);
        }
        if (termAst instanceof IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr)) {
            return hasIncompatibleComparison(condition)
                    || hasIncompatibleComparison(thenExpr)
                    || hasIncompatibleComparison(elseExpr);
        }
        return false;
    }

    private static boolean isConstantExpression(TermAst termAst) {
        if (termAst instanceof LiteralAst || termAst instanceof IriAst) {
            return true;
        }
        if (termAst instanceof VarAst || termAst instanceof FunctionCallAst || termAst instanceof IriExpressionAst) {
            return false;
        }
        if (termAst instanceof UnaryConstraintAst unaryConstraintAst) {
            return isConstantExpression(unaryConstraintAst.argument());
        }
        if (termAst instanceof BinaryConstraintAst binaryConstraintAst) {
            return isConstantExpression(binaryConstraintAst.getLeftArgument())
                    && isConstantExpression(binaryConstraintAst.getRightArgument());
        }
        if (termAst instanceof UnlimitedArgumentsFunctionAst unlimitedArgumentsFunctionAst) {
            return unlimitedArgumentsFunctionAst.arguments().stream()
                    .allMatch(SemanticValidationUtils::isConstantExpression);
        }
        if (termAst instanceof IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr)) {
            return isConstantExpression(condition)
                    && isConstantExpression(thenExpr)
                    && isConstantExpression(elseExpr);
        }
        return false;
    }

    private static ValueType valueType(TermAst termAst) {
        if (termAst instanceof LiteralAst(String lexical, String lang, String datatype)) {
            if ((datatype != null && NUMERIC_DATATYPES.contains(datatype))
                    || (datatype == null && lang == null && isNumeric(lexical))) {
                return ValueType.NUMERIC;
            }
            if (BOOLEAN_DATATYPE.equals(datatype)) {
                return ValueType.BOOLEAN;
            }
            if (lang != null || datatype == null || STRING_DATATYPE.contains(datatype)) {
                return ValueType.STRING;
            }
        }
        if (termAst instanceof NumericExpressionAst) {
            return ValueType.NUMERIC;
        }
        if (termAst instanceof SimpleLiteralExpressionAst) {
            return ValueType.STRING;
        }
        if (termAst instanceof IriAst || termAst instanceof IriExpressionAst) {
            return ValueType.IRI;
        }
        if (termAst instanceof XsdDateTimeExpressionAst) {
            return ValueType.DATETIME;
        }
        if (termAst instanceof XsdDayTimeDurationExpressionAst) {
            return ValueType.DURATION;
        }
        if (termAst instanceof BooleanExpressionAst) {
            return ValueType.BOOLEAN;
        }
        if (termAst instanceof IfAst ifAst) {
            ValueType thenType = valueType(ifAst.thenExpr());
            return thenType == valueType(ifAst.elseExpr()) ? thenType : null;
        }
        return null;
    }

    private enum ValueType {
        NUMERIC,
        STRING,
        BOOLEAN,
        IRI,
        DATETIME,
        DURATION
    }
}
