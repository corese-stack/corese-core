package fr.inria.corese.core.next.query.impl.parser.semantic.support;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static fr.inria.corese.core.next.query.impl.parser.semantic.support.SemanticValidationUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class SemanticValidationUtilsTest {

    @Test
    void checkTermIsPotentialBooleanTest() {
        assertTrue(checkTermIsPotentialBoolean(new LiteralAst("true", null, null)));
        assertTrue(checkTermIsPotentialBoolean(new LiteralAst("potato", null, XSD.xsdBoolean.getIRI().stringValue())));
        assertTrue(checkTermIsPotentialBoolean(new AndAst(List.of(new LiteralAst("true", null, null), new LiteralAst("true", null, null)))));
        assertTrue(checkTermIsPotentialBoolean(new VarAst("test")));
        assertFalse(checkTermIsPotentialBoolean(new IriAst("<http://ns.inria.de/test>")));
        assertFalse(checkTermIsPotentialBoolean(new NowAst()));
    }

    @Test
    void checkTermIsPotentialNumericTest() {
        assertTrue(checkTermIsPotentialNumeric(new LiteralAst("1", null, null)));
        assertTrue(checkTermIsPotentialNumeric(new LiteralAst("abc", null, XSD.xsdDouble.getIRI().stringValue())));
        assertTrue(checkTermIsPotentialNumeric(new VarAst("test")));
        assertFalse(checkTermIsPotentialNumeric(new IriAst("<http://ns.inria.de/test>")));
        assertFalse(checkTermIsPotentialNumeric(new NowAst()));
        assertTrue(checkTermIsPotentialNumeric(new AddAst(List.of(new LiteralAst("1", null, null), new LiteralAst("2", null, null)))));
    }

    @Test
    void checkTermIsUnknownTypeTest() {
        assertTrue(checkTermIsUnknownType(new VarAst("test")));
        assertFalse(checkTermIsUnknownType(new LiteralAst("true", null, null)));
        assertFalse(checkTermIsUnknownType(new NowAst()));
    }

    @Test
    void checkStringIsNumericTest() {
        assertTrue(checkStringIsNumeric("1"));
        assertFalse(checkStringIsNumeric("Potato"));
    }

    @Test
    void checkTermIsPotentialIriTest() {
        assertTrue(checkTermIsPotentialIri(new IriAst("<http://ns.inria.de/test>")));
        assertTrue(checkTermIsPotentialIri(new DatatypeAst(List.of(new LiteralAst("1", null, null)))));
        assertFalse(checkTermIsPotentialIri(new VarAst("test")));
        assertFalse(checkTermIsPotentialIri(new LiteralAst("1", null, null)));
        assertFalse(checkTermIsPotentialIri(new NowAst()));
    }
}