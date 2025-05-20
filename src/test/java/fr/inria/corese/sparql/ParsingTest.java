/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.corese.sparql;

import fr.inria.corese.core.sparql.exceptions.QueryLexicalException;
import fr.inria.corese.core.sparql.exceptions.QuerySyntaxException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.ParserSparql1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author gaignard
 */
public class ParsingTest {

    public ParsingTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //

    @Test
    //@Ignore
    public void hello1() throws QueryLexicalException, QuerySyntaxException {
        String query = "insert data {\n"
                + "<http://dbpedia.org/resource/Alban_Bagbin> <http://xmlns.com/foaf/0.1/name> \"Alban Kingsford Sumana Bagbin\"\n"
                + "<http://dbpedia.org/resource/Alban_Berg> <http://xmlns.com/foaf/0.1/name> \"Alban Berg\"\n"
                + "<http://dbpedia.org/resource/Alban_Ceray> <http://xmlns.com/foaf/0.1/name> \"Alban Ceray\"\n"
                + "<http://dbpedia.org/resource/Alban_Maginness> <http://xmlns.com/foaf/0.1/name> \"Alban Maginness\"\n"
                + "}";

        ASTQuery ast = ASTQuery.create(query);
       // ast.setKgram(true);
        ParserSparql1.create(ast).parse();
        System.out.println(ast);
    }
}
