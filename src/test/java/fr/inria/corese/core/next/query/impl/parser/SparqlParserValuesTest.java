package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserValuesTest extends AbstractSparqlParserFeatureTest {

    private static final Logger logger = LoggerFactory.getLogger(SparqlParserValuesTest.class);

    @Test
    public void inlineSyntaxTest() {
        SparqlParser parser = newParserDefault();
        String inlineValueTest = """
                SELECT ?var {
                    VALUES ?var { "test" <http://ns.inria.fr/test> }
                }
                """;
        QueryAst ast = parser.parse(inlineValueTest);
        assertNotNull(ast);
        ValuesAst valuesAst = ast.valuesClause();
        assertNotNull(valuesAst);
        assertEquals(2, valuesAst.mappings().size());
        assertEquals(1, valuesAst.mappings().getFirst().values().keySet().size());
        assertEquals(1, valuesAst.mappings().getLast().values().keySet().size());
        VarAst varKey = new VarAst("var");
        assertTrue(valuesAst.mappings().getFirst().values().containsKey(varKey));
        assertInstanceOf(LiteralAst.class, valuesAst.mappings().getFirst().values().get(varKey));
        LiteralAst literalAst = (LiteralAst) valuesAst.mappings().getFirst().values().get(varKey);
        assertEquals("\"test\"", literalAst.lexical());
        assertInstanceOf(IriAst.class, valuesAst.mappings().getLast().values().get(varKey));
        IriAst iriAst = (IriAst) valuesAst.mappings().getLast().values().get(varKey);
        assertEquals("<http://ns.inria.fr/test>", iriAst.raw());
    }

    @Test
    public void fullSyntaxTest() {
        SparqlParser parser = newParserDefault();
        String inlineValueTest = """
                SELECT ?var1 ?var2 {
                    VALUES (?var1 ?var2) { ("test1" <http://ns.inria.fr/test1>) ("test2" <http://ns.inria.fr/test2>) }
                }
                """;
        QueryAst ast = parser.parse(inlineValueTest);
        assertNotNull(ast);
        ValuesAst valuesAst = ast.valuesClause();
        assertNotNull(valuesAst);
        assertEquals(2, valuesAst.mappings().size());
        assertEquals(2, valuesAst.mappings().getFirst().values().keySet().size());
        assertEquals(2, valuesAst.mappings().getLast().values().keySet().size());
        VarAst var1Key = new VarAst("var1");
        VarAst var2Key = new VarAst("var2");
        assertTrue(valuesAst.mappings().getFirst().values().containsKey(var1Key));
        assertTrue(valuesAst.mappings().getFirst().values().containsKey(var2Key));
        assertTrue(valuesAst.mappings().getLast().values().containsKey(var1Key));
        assertTrue(valuesAst.mappings().getLast().values().containsKey(var2Key));
        assertEquals(2, valuesAst.mappings().getFirst().values().size());
        assertEquals(2, valuesAst.mappings().getLast().values().size());

        ValueMappingAst valueMappingAst1 = valuesAst.mappings().getFirst();
        assertInstanceOf(LiteralAst.class, valueMappingAst1.values().get(var1Key));
        LiteralAst literalAst1 = (LiteralAst) valueMappingAst1.values().get(var1Key);
        assertEquals("\"test1\"", literalAst1.lexical());
        assertInstanceOf(IriAst.class, valueMappingAst1.values().get(var2Key));
        IriAst iriAst1 = (IriAst) valueMappingAst1.values().get(var2Key);
        assertEquals("<http://ns.inria.fr/test1>", iriAst1.raw());

        ValueMappingAst valueMappingAst2 = valuesAst.mappings().getLast();
        assertInstanceOf(LiteralAst.class, valueMappingAst2.values().get(var1Key));
        LiteralAst literalAst2 = (LiteralAst) valueMappingAst2.values().get(var1Key);
        assertEquals("\"test2\"", literalAst2.lexical());
        assertInstanceOf(IriAst.class, valueMappingAst2.values().get(var2Key));
        IriAst iriAst2 = (IriAst) valueMappingAst2.values().get(var2Key);
        assertEquals("<http://ns.inria.fr/test2>", iriAst2.raw());
    }

    @Test
    public void multipleValuesTest() {
        SparqlParser parser = newParserDefault();
        String inlineValueTest = """
                SELECT ?var1 ?var2 {
                    VALUES ?var1 { "test1" }
                }
               VALUES (?var2 ?var3) { ("test2" <http://ns.inria.fr/test2>) }
               """;
        QueryAst ast = parser.parse(inlineValueTest);
        assertNotNull(ast);
        ValuesAst valuesAst = ast.valuesClause();
        assertNotNull(valuesAst);
        assertEquals(2, valuesAst.mappings().size());
        assertEquals(1, valuesAst.mappings().getFirst().values().keySet().size());
        assertEquals(2, valuesAst.mappings().getLast().values().keySet().size());
        VarAst var1Key = new VarAst("var1");
        VarAst var2Key = new VarAst("var2");
        VarAst var3Key = new VarAst("var3");
        assertTrue(valuesAst.mappings().getFirst().values().containsKey(var1Key));
        assertTrue(valuesAst.mappings().getLast().values().containsKey(var2Key));
        assertTrue(valuesAst.mappings().getLast().values().containsKey(var3Key));

        ValueMappingAst valueMappingAst1 = valuesAst.mappings().getFirst();
        assertInstanceOf(LiteralAst.class, valueMappingAst1.values().get(var1Key));
        LiteralAst literalAst1 = (LiteralAst) valueMappingAst1.values().get(var1Key);
        assertEquals("\"test1\"", literalAst1.lexical());

        ValueMappingAst valueMappingAst2 = valuesAst.mappings().getLast();
        assertInstanceOf(LiteralAst.class, valueMappingAst2.values().get(var2Key));
        LiteralAst literalAst2 = (LiteralAst) valueMappingAst2.values().get(var2Key);
        assertEquals("\"test2\"", literalAst2.lexical());
        assertInstanceOf(IriAst.class, valueMappingAst2.values().get(var3Key));
        IriAst iriAst2 = (IriAst) valueMappingAst2.values().get(var3Key);
        assertEquals("<http://ns.inria.fr/test2>", iriAst2.raw());
    }

    @Test
    public void nilValueSyntaxTest() {
        SparqlParser parser = newParserDefault();
        String inlineValueTest = """
                SELECT ?var {
                    VALUES ?var { "test" UNDEF }
                }
                """;
        QueryAst ast = parser.parse(inlineValueTest);
        assertNotNull(ast);
        ValuesAst valuesAst = ast.valuesClause();
        assertNotNull(valuesAst);
        assertEquals(2, valuesAst.mappings().size());
        assertEquals(1, valuesAst.mappings().getFirst().values().keySet().size());
        assertEquals(1, valuesAst.mappings().getLast().values().keySet().size());
        VarAst varKey = new VarAst("var");
        assertTrue(valuesAst.mappings().getFirst().values().containsKey(varKey));
        assertInstanceOf(LiteralAst.class, valuesAst.mappings().getFirst().values().get(varKey));
        LiteralAst literalAst = (LiteralAst) valuesAst.mappings().getFirst().values().get(varKey);
        assertEquals("\"test\"", literalAst.lexical());
        assertNull(valuesAst.mappings().getLast().values().get(varKey));
    }

    @Test
    public void nilVarNilValueSyntaxTest() {
        SparqlParser parser = newParserDefault();
        String inlineValueTest = """
                SELECT ?var {
                    ?var ?var ?var .
                    VALUES () { () }
                }
                """;
        QueryAst ast = parser.parse(inlineValueTest);
        assertNotNull(ast);
        ValuesAst valuesAst = ast.valuesClause();
        assertNotNull(valuesAst);
        assertEquals(0, valuesAst.mappings().size());
    }

    @Test
    public void nilVarSomeValueSyntaxExceptionTest() {
        SparqlParser parser = newParserDefault();
        String inlineValueTest = """
                SELECT ?var {
                    ?var ?var ?var .
                    VALUES () { ( "test" ) }
                }
                """;
        QueryAst ast = parser.parse(inlineValueTest);
        assertNotNull(ast);
        ValuesAst valuesAst = ast.valuesClause();
        assertNotNull(valuesAst);
        assertEquals(0, valuesAst.mappings().size());
    }
}
