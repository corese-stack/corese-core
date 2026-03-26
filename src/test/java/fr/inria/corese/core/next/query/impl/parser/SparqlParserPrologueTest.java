package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
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

        SelectQueryAst ast = (SelectQueryAst) parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
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

        SelectQueryAst ast = (SelectQueryAst) parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test"));
        assertEquals("test", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest/#"));
        assertTrue(ast.prologue().toPrefixHandler().hasNamespace("https://ns.inria.fr/otherTest/#"));
        assertEquals("https://ns.inria.fr/otherTest/#", ast.prologue().toPrefixHandler().getNamespace("test"));
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

        SelectQueryAst ast = (SelectQueryAst) parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test1"));
        assertEquals("test1", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertTrue(ast.prologue().toPrefixHandler().hasNamespace("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest1/#", ast.prologue().toPrefixHandler().getNamespace("test1"));
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test2"));
        assertEquals("test2", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest2/#"));
        assertTrue(ast.prologue().toPrefixHandler().hasNamespace("https://ns.inria.fr/otherTest2/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prologue().toPrefixHandler().getNamespace("test2"));
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test3"));
        assertEquals("test3", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest3/#"));
        assertTrue(ast.prologue().toPrefixHandler().hasNamespace("https://ns.inria.fr/otherTest3/#"));
        assertEquals("https://ns.inria.fr/otherTest3/#", ast.prologue().toPrefixHandler().getNamespace("test3"));
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

        SelectQueryAst ast = (SelectQueryAst) parser.parse(query);
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test1"));
        assertEquals("test2", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prologue().toPrefixHandler().getNamespace("test1"));
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test2"));
        assertEquals("test2", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertTrue(ast.prologue().toPrefixHandler().hasNamespace("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest1/#", ast.prologue().toPrefixHandler().getNamespace("test2"));
        assertTrue(ast.prologue().toPrefixHandler().hasPrefix("test1"));
        assertEquals("test1", ast.prologue().toPrefixHandler().getPrefix("https://ns.inria.fr/otherTest2/#"));
        assertTrue(ast.prologue().toPrefixHandler().hasNamespace("https://ns.inria.fr/otherTest2/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prologue().toPrefixHandler().getNamespace("test1"));
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
