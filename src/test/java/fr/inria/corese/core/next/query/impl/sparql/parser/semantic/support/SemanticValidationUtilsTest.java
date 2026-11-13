package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.*;
import static org.junit.jupiter.api.Assertions.*;



class SemanticValidationUtilsTest {

    @Test
    void checkTermIsPotentialBooleanTest() {
        assertTrue(isPotentialBooleanCompatible(new LiteralAst("true", null, null)));
        assertTrue(isPotentialBooleanCompatible(new LiteralAst("potato", null, XSD.xsdBoolean.getIRI().stringValue())));
        assertTrue(isPotentialBooleanCompatible(new AndAst(List.of(new LiteralAst("true", null, null), new LiteralAst("true", null, null)))));
        assertTrue(isPotentialBooleanCompatible(new VarAst("test")));
        assertFalse(isPotentialBooleanCompatible(new IriAst("<http://ns.inria.de/test>")));
        assertFalse(isPotentialBooleanCompatible(new NowAst()));
    }

    @Test
    void isPotentialNumericTest() {
        assertTrue(isPotentialNumeric(new LiteralAst("1", null, null)));
        assertTrue(isPotentialNumeric(new LiteralAst("abc", null, XSD.xsdDouble.getIRI().stringValue())));
        assertTrue(isPotentialNumeric(new VarAst("test")));
        assertFalse(isPotentialNumeric(new IriAst("<http://ns.inria.de/test>")));
        assertFalse(isPotentialNumeric(new NowAst()));
        assertTrue(isPotentialNumeric(new AddAst(List.of(new LiteralAst("1", null, null), new LiteralAst("2", null, null)))));
    }

    @Test
    void isUnknownTypeTest() {
        assertTrue(isUnknownType(new VarAst("test")));
        assertFalse(isUnknownType(new LiteralAst("true", null, null)));
        assertFalse(isUnknownType(new NowAst()));
    }

    @Test
    void isNumericTest() {
        assertTrue(isNumeric("1"));
        assertFalse(isNumeric("Potato"));
    }

    @Test
    void isPotentialIriTest() {
        assertTrue(isPotentialIri(new IriAst("<http://ns.inria.de/test>")));
        assertTrue(isPotentialIri(new DatatypeAst(List.of(new LiteralAst("1", null, null)))));
        assertFalse(isPotentialIri(new VarAst("test")));
        assertFalse(isPotentialIri(new LiteralAst("1", null, null)));
        assertFalse(isPotentialIri(new NowAst()));
    }

    @Test
    void isPotentialStringLiteralTest() {
        assertTrue(isPotentialStringLiteral(new LiteralAst("test", null, null)));
        assertTrue(isPotentialStringLiteral(new LiteralAst("test", "en", null)));
        assertTrue(isPotentialStringLiteral(new LiteralAst("test", null, RDF.langString.getIRI().stringValue())));
        assertTrue(isPotentialStringLiteral(new LiteralAst("test", null, XSD.xsdString.getIRI().stringValue())));
        assertTrue(isPotentialStringLiteral(new VarAst("test")));
        assertTrue(isPotentialStringLiteral(new ConcatAst(List.of())));
        assertFalse(isPotentialStringLiteral(new LiteralAst("test", null, XSD.xsdDouble.getIRI().stringValue())));
        assertFalse(isPotentialStringLiteral(new DatatypeAst(List.of(new LiteralAst("1", null, null)))));
        assertFalse(isPotentialStringLiteral(new IriAst("<http://ns.inria.de/test>")));
        assertFalse(isPotentialStringLiteral(new NowAst()));
    }

    @Test
    void hasIncompatibleComparisonTest() {
        LiteralAst strLiteral = new LiteralAst("text", null, null);
        LiteralAst numLiteral = new LiteralAst("1", null, XSD.xsdInteger.getIRI().stringValue());
        IriAst iri = new IriAst("<http://example.org/>");

        // string vs numeric → incompatible
        assertTrue(hasIncompatibleComparison(new GreaterThanAst(List.of(strLiteral, numLiteral))));
        // IRI operands are excluded — handled by OperandArgumentIRITypeValidationRule
        assertFalse(hasIncompatibleComparison(new GreaterThanAst(List.of(iri, numLiteral))));
        // same type → compatible
        assertFalse(hasIncompatibleComparison(new LowerThanAst(List.of(numLiteral, numLiteral))));
        // variable → unknown type, no incompatibility detected statically
        assertFalse(hasIncompatibleComparison(new GreaterThanAst(List.of(new VarAst("x"), numLiteral))));
    }

    @Test
    void visitExpressionTermsTest() {
        // Collects all terms in a simple expression tree
        LiteralAst left = new LiteralAst("1", null, null);
        LiteralAst right = new LiteralAst("2", null, null);
        GreaterThanAst gt = new GreaterThanAst(List.of(left, right));

        List<TermAst> visited = new ArrayList<>();
        visitExpressionTerms(gt, visited::add);

        assertTrue(visited.contains(gt));
        assertTrue(visited.contains(left));
        assertTrue(visited.contains(right));
        assertEquals(3, visited.size());
    }

    @Test
    void isSuspiciousEBVCoercionTest() {
        // Suspicious: numeric literals
        assertTrue(isSuspiciousEBVCoercion(new LiteralAst("42", null, XSD.xsdInteger.getIRI().stringValue())));
        assertTrue(isSuspiciousEBVCoercion(new LiteralAst("3.14", null, XSD.xsdDecimal.getIRI().stringValue())));
        assertTrue(isSuspiciousEBVCoercion(new LiteralAst("1.0", null, XSD.xsdDouble.getIRI().stringValue())));
        // Boolean literals have a valid EBV; constant-condition diagnostics handle them separately.
        LiteralAst trueLiteral = new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
        LiteralAst falseLiteral = new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
        assertFalse(isSuspiciousEBVCoercion(trueLiteral));
        assertFalse(isSuspiciousEBVCoercion(falseLiteral));
        assertTrue(isConstantBooleanCondition(trueLiteral));
        assertTrue(isConstantBooleanCondition(falseLiteral));
        // Suspicious: plain string literal
        assertTrue(isSuspiciousEBVCoercion(new LiteralAst("text", null, null)));
        assertTrue(isSuspiciousEBVCoercion(new LiteralAst("text", null, XSD.xsdString.getIRI().stringValue())));
        // A bare variable may hold a non-boolean value, so flag its implicit EBV use.
        assertTrue(isSuspiciousEBVCoercion(new VarAst("x")));
        // Not suspicious: IRI, function call
        assertFalse(isSuspiciousEBVCoercion(new IriAst("<http://example.org/>")));
        assertFalse(isSuspiciousEBVCoercion(new NowAst()));
    }
}