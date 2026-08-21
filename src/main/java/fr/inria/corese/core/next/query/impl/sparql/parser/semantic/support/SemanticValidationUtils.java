package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.Set;

public class SemanticValidationUtils {

    private static final String BOOLEAN_DATATYPE = XSD.xsdBoolean.getIRI().stringValue();
    private static final Set<String> STRING_DATATYPE = Set.of(
            XSD.xsdString.getIRI().stringValue(),
            RDF.langString.getIRI().stringValue());

    /**
     * Valid numeric datatypes as defined in https://www.w3.org/TR/sparql11-query/#operandDataTypes
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
     */
    public static boolean isPotentialBooleanCompatible(TermAst termAst) {
        if (termAst instanceof BooleanExpressionAst
                || termAst instanceof NumericExpressionAst
                || termAst instanceof VarAst
                || termAst instanceof FunctionCallAst
                || termAst instanceof UnlimitedArgumentsFunctionAst) {
            return true;
        }

        if (termAst instanceof LiteralAst literalAst) {
            return isBooleanCompatible(literalAst);
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
}
