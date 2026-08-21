package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.Test;

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
}