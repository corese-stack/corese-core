package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CoreseTermAdapter}.
 */
class CoreseTermAdapterTest {

    @Nested
    @DisplayName("toNode")
    class ToNodeTest {

        @Test
        @DisplayName("VarAst produces variable node with same label")
        void varAstToVariable() {
            VarAst v = new VarAst("s");
            Node n = CoreseTermAdapter.toNode(v);
            assertNotNull(n);
            assertTrue(n.isVariable());
            assertFalse(n.isConstant());
            assertEquals("s", n.getLabel());
        }

        @Test
        @DisplayName("IriAst produces constant node with raw IRI as label")
        void iriAstToResource() {
            IriAst i = new IriAst("http://example.org/");
            Node n = CoreseTermAdapter.toNode(i);
            assertNotNull(n);
            assertTrue(n.isConstant());
            assertFalse(n.isVariable());
            assertEquals("http://example.org/", n.getLabel());
        }

        @Test
        @DisplayName("LiteralAst lexical only produces string literal node")
        void literalLexicalOnly() {
            LiteralAst l = new LiteralAst("hello", null, null);
            Node n = CoreseTermAdapter.toNode(l);
            assertNotNull(n);
            assertTrue(n.isConstant());
            assertEquals("hello", n.getLabel());
        }

        @Test
        @DisplayName("LiteralAst with language tag")
        void literalWithLang() {
            LiteralAst l = new LiteralAst("\"salut\"", "fr", null);
            Node n = CoreseTermAdapter.toNode(l);
            assertNotNull(n);
            assertTrue(n.isConstant());
        }

        @Test
        @DisplayName("LiteralAst with datatype")
        void literalWithDatatype() {
            LiteralAst l = new LiteralAst("\"42\"", null, "xsd:integer");
            Node n = CoreseTermAdapter.toNode(l);
            assertNotNull(n);
            assertTrue(n.isConstant());
        }

        @Test
        @DisplayName("throws when term is null")
        void nullTermThrows() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> CoreseTermAdapter.toNode(null));
            assertTrue(e.getMessage().contains("null"));
        }
    }
}
