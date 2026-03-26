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
        assertEquals("http://ns.inria.fr/test/", ast.prologue().prefixHandler().getDefaultNamespace());
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
        assertEquals("http://ns.inria.fr/test/", ast.prologue().prefixHandler().getDefaultNamespace());
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
        assertEquals("http://ns.inria.fr/test/", ast.prologue().prefixHandler().getDefaultNamespace());
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
        assertTrue(ast.prologue().prefixHandler().hasPrefix("test"));
        assertEquals("test", ast.prologue().prefixHandler().getPrefix("https://ns.inria.fr/otherTest/#"));
        assertTrue(ast.prologue().prefixHandler().hasNamespace("https://ns.inria.fr/otherTest/#"));
        assertEquals("https://ns.inria.fr/otherTest/#", ast.prologue().prefixHandler().getNamespace("test"));
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
        assertTrue(ast.prologue().prefixHandler().hasPrefix("test1"));
        assertEquals("test1", ast.prologue().prefixHandler().getPrefix("https://ns.inria.fr/otherTest1/#"));
        assertTrue(ast.prologue().prefixHandler().hasNamespace("https://ns.inria.fr/otherTest1/#"));
        assertEquals("https://ns.inria.fr/otherTest1/#", ast.prologue().prefixHandler().getNamespace("test1"));
        assertTrue(ast.prologue().prefixHandler().hasPrefix("test2"));
        assertEquals("test2", ast.prologue().prefixHandler().getPrefix("https://ns.inria.fr/otherTest2/#"));
        assertTrue(ast.prologue().prefixHandler().hasNamespace("https://ns.inria.fr/otherTest2/#"));
        assertEquals("https://ns.inria.fr/otherTest2/#", ast.prologue().prefixHandler().getNamespace("test2"));
        assertTrue(ast.prologue().prefixHandler().hasPrefix("test3"));
        assertEquals("test3", ast.prologue().prefixHandler().getPrefix("https://ns.inria.fr/otherTest3/#"));
        assertTrue(ast.prologue().prefixHandler().hasNamespace("https://ns.inria.fr/otherTest3/#"));
        assertEquals("https://ns.inria.fr/otherTest3/#", ast.prologue().prefixHandler().getNamespace("test3"));
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

        SparqlParser parser = newParserDefault();

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
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

    @Test
    @DisplayName("PREFIX with empty prefix label should be accepted")
    public void selectWithDefaultPrefixDeclaration() {
        String query = """
                PREFIX : <https://ns.inria.fr/default/#>
                SELECT * {
                    ?s :p ?o .
                }
                """;

        SparqlParser parser = newParserDefault();

        SelectQueryAst ast = assertDoesNotThrow(() -> (SelectQueryAst) parser.parse(query));
        assertTrue(ast.prologue().prefixHandler().hasPrefix(""));
        assertEquals("", ast.prologue().prefixHandler().getPrefix("https://ns.inria.fr/default/#"));
        assertEquals("https://ns.inria.fr/default/#", ast.prologue().prefixHandler().getNamespace(""));
    }

    @Test
    @DisplayName("Relative PREFIX IRI should be resolved against effective base")
    public void relativePrefixShouldBeResolvedAgainstEffectiveBase() {
        String query = """
            BASE <http://example.org/root/>
            PREFIX ex: <ns/>
            SELECT * {
                ?s ex:p ?o .
            }
            """;

        SparqlParser parser = newParserDefault();

        SelectQueryAst ast = assertDoesNotThrow(() -> (SelectQueryAst) parser.parse(query));
        assertEquals("http://example.org/root/ns/", ast.prologue().prefixHandler().getNamespace("ex"));
    }
}
