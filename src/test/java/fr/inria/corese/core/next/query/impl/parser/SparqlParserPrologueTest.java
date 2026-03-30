package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserPrologueTest extends AbstractSparqlParserFeatureTest {

    private static final Logger logger = LoggerFactory.getLogger(SparqlParserPrologueTest.class);

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
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
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
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
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
        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
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

        logger.debug("{}", ast.prologue());

        assertEquals("http://ns.inria.fr/test/", ast.prologue().baseIri().raw());
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test")));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test") && prefixDecl.namespace().raw().equals("https://ns.inria.fr/otherTest/#")));
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
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test1")));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test1") && prefixDecl.namespace().raw().equals("https://ns.inria.fr/otherTest1/#")));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test2")));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test2") && prefixDecl.namespace().raw().equals("https://ns.inria.fr/otherTest2/#")));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test3")));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("test3") && prefixDecl.namespace().raw().equals("https://ns.inria.fr/otherTest3/#")));
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
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().isEmpty()));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().isEmpty() && prefixDecl.namespace().raw().equals("https://ns.inria.fr/default/#")));
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
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("ex")), "ex: is in the prologue");
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("ex") && prefixDecl.namespace().raw().equals("http://example.org/root/ns/")), "the IRI of ex: in http://example.org/root/ns/");
    }

    @Test
    @DisplayName("Relative PREFIX IRI should use RFC3986 resolution, not string concatenation")
    public void relativePrefixShouldUseRfc3986Resolution() {
        String query = """
            BASE <http://example.org/root>
            PREFIX ex: <ns/>
            SELECT * {
                ?s ex:p ?o .
            }
            """;

        SparqlParser parser = newParserDefault();

        SelectQueryAst ast = assertDoesNotThrow(() -> (SelectQueryAst) parser.parse(query));
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("ex")), "ex: is in the prologue");
        assertTrue(ast.prologue().prefixDeclarations().stream().anyMatch(prefixDecl -> prefixDecl.prefix().equals("ex") && prefixDecl.namespace().raw().equals("http://example.org/ns/")), "the IRI of ex: in http://example.org/ns/");
    }
}
