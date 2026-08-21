package fr.inria.corese.core.next.query.impl.sparql.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Md5Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha1Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha256Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha384Ast;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.Sha512Ast;

@DisplayName("SPARQL 1.1 - Parser and AST : Hash function")
class SparqlParserHashFunctionsTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(MD5(?label) AS ?hash)")
    void shouldParseMd5InBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(MD5(?label) AS ?hash)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        Md5Ast md5 = assertInstanceOf(Md5Ast.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, md5.argument()).name());
        assertEquals("hash", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(SHA1(?label) AS ?hash)")
    void shouldParseSha1InBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(SHA1(?label) AS ?hash)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        Sha1Ast sha1 = assertInstanceOf(Sha1Ast.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, sha1.argument()).name());
        assertEquals("hash", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(SHA256(?label) AS ?hash)")
    void shouldParseSha256InBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(SHA256(?label) AS ?hash)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        Sha256Ast sha256 = assertInstanceOf(Sha256Ast.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, sha256.argument()).name());
        assertEquals("hash", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(SHA384(?label) AS ?hash)")
    void shouldParseSha384InBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(SHA384(?label) AS ?hash)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        Sha384Ast sha384 = assertInstanceOf(Sha384Ast.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, sha384.argument()).name());
        assertEquals("hash", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(SHA512(?label) AS ?hash)")
    void shouldParseSha512InBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(SHA512(?label) AS ?hash)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        Sha512Ast sha512 = assertInstanceOf(Sha512Ast.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, sha512.argument()).name());
        assertEquals("hash", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY MD5(?label)")
    void shouldParseOrderByMd5() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY MD5(?label)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(Md5Ast.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY SHA1(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderBySha1WhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY SHA1(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
