package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.io.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
        assertEquals(1, loadQueryAst.fromClause().size());
        IriAst graphIri = (IriAst) loadQueryAst.fromClause().toArray()[0];
        assertEquals("<http://ns.inria.fr/test>", graphIri.raw());
        assertEquals(0, loadQueryAst.toClause().size());
        assertEquals(false, loadQueryAst.silent());
    }
}
