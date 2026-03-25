package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserPrologueTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("Basic Ask with base")
    public void askWithBase() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                ASK {
                    ?s ?p ?o .
                }
                """;

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
    }

    @Test
    @DisplayName("Basic Construct with base")
    public void constructWithBase() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                CONSTRUCT {
                    ?o ?p ?s .
                }
                {
                    ?s ?p ?o .
                } LIMIT 10
                """;

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
    }

    @Test
    @DisplayName("Basic Select with base")
    public void describeWithBase() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                DESCRIBE ?s {
                    ?s ?p ?o .
                } LIMIT 10
                """;

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
    }

    @Test
    @DisplayName("Basic Select with base")
    public void selectWithBase() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                SELECT * {
                    ?s ?p ?o .
                } LIMIT 10
                """;

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
    }

    @Test
    @DisplayName("Basic Select with base and one prefix")
    public void selectWithBaseAndOnePrefix() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                PREFIX test: <https://ns.inria.fr/otherTest/#>
                SELECT * {
                    ?s ?p ?o .
                } LIMIT 10
                """;

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
        assertTrue(ast.prefixHandler().hasPrefix("test"));
        assertEquals("test", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest/#"));
        assertTrue(ast.prefixHandler().hasNamespace("https://ns.inria.fr/otherTest/#"));
        assertEquals("https://ns.inria.fr/otherTest/#", ast.prefixHandler().getNamespace("test"));
    }

    @Test
    @DisplayName("Basic Select with base and multiple prefix")
    public void selectWithBaseAndMultiplePrefix() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                PREFIX test1: <https://ns.inria.fr/otherTest1/#>
                PREFIX test2: <https://ns.inria.fr/otherTest2/#>
                PREFIX test3: <https://ns.inria.fr/otherTest3/#>
                SELECT * {
                    ?s ?p ?o .
                } LIMIT 10
                """;

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
        assertTrue(ast.prefixHandler().hasPrefix("test1"));
        assertEquals("test1", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertTrue(ast.prefixHandler().hasNamespace("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest1/#", ast.prefixHandler().getNamespace("test1"));
        assertTrue(ast.prefixHandler().hasPrefix("test2"));
        assertEquals("test2", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest2/#"));
        assertTrue(ast.prefixHandler().hasNamespace("https://ns.inria.fr/otherTest2/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prefixHandler().getNamespace("test2"));
        assertTrue(ast.prefixHandler().hasPrefix("test3"));
        assertEquals("test3", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest3/#"));
        assertTrue(ast.prefixHandler().hasNamespace("https://ns.inria.fr/otherTest3/#"));
        assertEquals("https://ns.inria.fr/otherTest3/#", ast.prefixHandler().getNamespace("test3"));
    }

    @Test
    @DisplayName("Basic Select with base and multiple prefix with overlap")
    public void selectWithBaseAndMultiplePrefixWithOverlap() {
        String query = """
                BASE <http://ns.inria.fr/test/>
                PREFIX test1: <https://ns.inria.fr/otherTest1/#>
                PREFIX test2: <https://ns.inria.fr/otherTest1/#>
                PREFIX test1: <https://ns.inria.fr/otherTest2/#>
                SELECT * {
                    ?s ?p ?o .
                } LIMIT 10
                """;
        // test1 -> ns:otherTest1
        // ns:otherTest1 -> test1
        // test2 -> ns:otherTest1
        // ns:otherTest1 -> test2
        // test1 -> ns:otherTest2
        // ns:otherTest2 -> test1

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prefixHandler().getDefaultNamespace());
        assertTrue(ast.prefixHandler().hasPrefix("test1"));
        assertEquals("test2", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prefixHandler().getNamespace("test1"));
        assertTrue(ast.prefixHandler().hasPrefix("test2"));
        assertEquals("test2", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertTrue(ast.prefixHandler().hasNamespace("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest1/#", ast.prefixHandler().getNamespace("test2"));
        assertTrue(ast.prefixHandler().hasPrefix("test1"));
        assertEquals("test1", ast.prefixHandler().getPrefix("https://ns.inria.fr/otherTest2/#"));
        assertTrue(ast.prefixHandler().hasNamespace("https://ns.inria.fr/otherTest2/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prefixHandler().getNamespace("test1"));
    }

    @Test
    @DisplayName("Basic Select with multiple base should throw")
    public void selectWithMultipleBase() {
        String query = """
                BASE <http://ns.inria.fr/test1/>
                BASE <http://ns.inria.fr/test2/>
                SELECT * {
                    ?s ?p ?o .
                } LIMIT 10
                """;

        SparqlParser parser = newParserDefault();

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }
}
