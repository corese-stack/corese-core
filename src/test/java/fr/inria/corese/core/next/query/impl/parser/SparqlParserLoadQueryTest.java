package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.io.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserLoadQueryTest extends AbstractSparqlParserFeatureTest{

    @Test
    public void simpleQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertNotNull(loadQueryAst.fromClause());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNull(loadQueryAst.toClause());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void simpleSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD SILENT <http://ns.inria.fr/test>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertNotNull(loadQueryAst.fromClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNull(loadQueryAst.toClause());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void fullQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertNotNull(loadQueryAst.fromClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNotNull(loadQueryAst.toClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst targetGraphIri = loadQueryAst.toClause().graph();
        assertEquals("<http://ns.inria.fr/otherGraph>", targetGraphIri.raw());
        assertFalse(loadQueryAst.silent());
    }

    @Test
    public void fullSilentQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD SILENT <http://ns.inria.fr/test> INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        QueryAst queryAst = parser.parse(query);
        assertInstanceOf(LoadQueryAst.class, queryAst);
        LoadQueryAst loadQueryAst = (LoadQueryAst) queryAst;
        assertNotNull(loadQueryAst.fromClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst graphIri = loadQueryAst.fromClause().graph();
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertNotNull(loadQueryAst.toClause());
        assertNotNull(loadQueryAst.fromClause().graph());
        IriAst targetGraphIri = loadQueryAst.toClause().graph();
        assertEquals("<http://ns.inria.fr/otherGraph>", targetGraphIri.raw());
        assertTrue(loadQueryAst.silent());
    }

    @Test
    public void failingDefaultSourceQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD DEFAULT INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    @Test
    public void failingDefaultTargetQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO DEFAULT
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    @Test
    public void failingNamedSourceQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD NAMED INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    @Test
    public void failingNamedTargetQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO NAMED
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    @Test
    public void failingAllSourceQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD ALL INTO GRAPH <http://ns.inria.fr/otherGraph>
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }

    @Test
    public void failingAllTargetQueryTest() {
        QueryParser parser = newParserDefault();
        String query = """
                LOAD <http://ns.inria.fr/test> INTO ALL
                """;

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse(query);
        });
    }
}
