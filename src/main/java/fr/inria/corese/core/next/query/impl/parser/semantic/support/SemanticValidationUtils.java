package fr.inria.corese.core.next.query.impl.parser.semantic.support;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.List;

public class SemanticValidationUtils {

    /**
     * Valid numeric datatypes as defined in https://www.w3.org/TR/sparql11-query/#operandDataTypes
     */
    private static final List<String> validNumericDatatypes = List.of(
            XSD.xsdInteger.getIRI().stringValue(),
            XSD.xsdDecimal.getIRI().stringValue(),
            XSD.xsdFloat.getIRI().stringValue(),
            XSD.xsdDouble.getIRI().stringValue(),
            XSD.xsdNonPositiveInteger.getIRI().stringValue(),
            XSD.xsdNegativeInteger.getIRI().stringValue(),
            XSD.xsdLong.getIRI().stringValue(),
            XSD.xsdInt.getIRI().stringValue(),
            XSD.xsdShort.getIRI().stringValue(),
            XSD.xsdByte.getIRI().stringValue(),
            XSD.xsdNonNegativeInteger.getIRI().stringValue(),
            XSD.xsdUnsignedLong.getIRI().stringValue(),
            XSD.xsdUnsignedInt.getIRI().stringValue(),
            XSD.xsdUnsignedShort.getIRI().stringValue(),
            XSD.xsdUnsignedByte.getIRI().stringValue(),
            XSD.xsdPositiveInteger.getIRI().stringValue()
    );

    /**
     * Check that the given term is either boolean expression, literal expression (that may return a boolean result) or a variable
     */
    public static boolean checkTermIsPotentialBoolean(TermAst termAst) {
        if (checkTermIsUnknownType(termAst)) {
            return true;
        }
        if (termAst instanceof BooleanExpressionAst) {
            return true;
        }
        if (termAst instanceof LiteralExpressionAst literalExpressionAst
                && !(literalExpressionAst instanceof XsdDateTimeExpressionAst
                || literalExpressionAst instanceof XsdDayTimeDurationExpressionAst
                || literalExpressionAst instanceof NumericExpressionAst
        )) { // Is a literal expression that could be a boolean, we cannot know
            return true;
        }
        if (termAst instanceof IfAst ifAst
                && checkTermIsPotentialBoolean(ifAst.thenExpr())
                && checkTermIsPotentialBoolean(ifAst.elseExpr())) { // Is a IF that returns potential booleans
            return true;
        }
        if (termAst instanceof LiteralAst(
                String lexical, String lang, String datatype
        )) {// is a literal that is a typed as a boolean or the string representation of one
            if (datatype != null) {
                return datatype.equals(XSD.xsdBoolean.getIRI().stringValue());
            } else {
                return lexical.trim().equalsIgnoreCase("true") || lexical.trim().equalsIgnoreCase("false");
            }
        }

        return false;
    }

    public static boolean checkTermIsPotentialNumeric(TermAst termAst) {
        if (checkTermIsUnknownType(termAst)) {
            return true;
        }
        if (termAst instanceof IfAst ifAst
                && checkTermIsPotentialNumeric(ifAst.thenExpr())
                && checkTermIsPotentialNumeric(ifAst.elseExpr())) { // Is a IF that returns potential numerics
            return true;
        }
        if (termAst instanceof NumericExpressionAst) { // Is a literal expression that could be a numeric, we cannot know
            return true;
        }
        if (termAst instanceof LiteralAst(String lexical, String lang, String datatype)) {
            if(datatype != null) {
                return validNumericDatatypes.contains(datatype); // Datatype is an URI of an standard numeric datatype
            } else {
                return checkStringIsNumeric(lexical); // The string value can be parsed to a numeric value
            }
        }

        return false;
    }

    /**
     * Check if the given term type cannot be determined at query parsing, it will be checked at query resolution.
     *
     */
    public static boolean checkTermIsUnknownType(TermAst termAst) {
        if (termAst instanceof VarAst) { // Is a variable that can be resolved to a boolean
            return true;
        }
        if (termAst instanceof FunctionCallAst) { // Is a function call that could return a boolean, we cannot know
            return true;
        }
        if(termAst instanceof SimpleLiteralExpressionAst) { // Could be a string un-typed but still representing a known type. will be filtered at resolution
            return true;
        }

        return false;
    }

    public static boolean checkStringIsNumeric(String lexical) {
        if (lexical == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(lexical);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
