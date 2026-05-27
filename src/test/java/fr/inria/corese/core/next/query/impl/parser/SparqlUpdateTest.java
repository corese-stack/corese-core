package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.io.parser.QueryParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlUpdateTest extends AbstractSparqlParserFeatureTest {

    @Test
    void chainedLoadShouldCreateTwoUpdateOperations() {
        QueryParser parser = newParserDefault();
        String query = """
            LOAD <http://example.org/a> INTO GRAPH <http://example.org/g> ;
            LOAD <http://example.org/b>
            """;

        QueryAst queryAst = parser.parse(query);
        UpdateRequestAst update = assertInstanceOf(UpdateRequestAst.class, queryAst);

        assertEquals(2, update.operations().size());

        LoadRequestAst first = assertInstanceOf(LoadRequestAst.class, update.operations().get(0));
        assertEquals("<http://example.org/a>", first.fromClause().graph().raw());
        assertNotNull(first.toClause());
        assertEquals("<http://example.org/g>", first.toClause().graph().raw());

        LoadRequestAst second = assertInstanceOf(LoadRequestAst.class, update.operations().get(1));
        assertEquals("<http://example.org/b>", second.fromClause().graph().raw());
        assertNull(second.toClause());
    }
}
