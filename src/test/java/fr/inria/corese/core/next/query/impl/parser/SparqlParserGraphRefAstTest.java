package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.GraphRefAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlParserGraphRefAstTest extends AbstractSparqlParserFeatureTest {

    @Test
    public void constructorGraph() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        GraphRefAst graphRefAst = new GraphRefAst(graphIri);
        assertNotNull(graphRefAst);
        assertEquals(graphIri, graphRefAst.graph());
        assertFalse(graphRefAst.named());
        assertFalse(graphRefAst.all());
        assertFalse(graphRefAst.defaultGraph());
    }

    @Test
    public void constructorNamed() {
        GraphRefAst graphRefAst = new GraphRefAst(true, false, false);
        assertNotNull(graphRefAst);
        assertTrue(graphRefAst.named());
        assertNull(graphRefAst.graph());
        assertFalse(graphRefAst.all());
        assertFalse(graphRefAst.defaultGraph());
    }

    @Test
    public void constructorAll() {
        GraphRefAst graphRefAst = new GraphRefAst(false, true, false);
        assertNotNull(graphRefAst);
        assertFalse(graphRefAst.named());
        assertNull(graphRefAst.graph());
        assertTrue(graphRefAst.all());
        assertFalse(graphRefAst.defaultGraph());
    }

    @Test
    public void constructorDefault() {
        GraphRefAst graphRefAst = new GraphRefAst(false, false, true);
        assertNotNull(graphRefAst);
        assertFalse(graphRefAst.named());
        assertNull(graphRefAst.graph());
        assertFalse(graphRefAst.all());
        assertTrue(graphRefAst.defaultGraph());
    }

    @Test
    public void constructorExclusiveValuesGraphNamed() {
        assertThrows(QueryEvaluationException.class, () -> {
            IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
            new GraphRefAst(graphIri, true, false, false);
        });
    }

    @Test
    public void constructorExclusiveValuesGraphAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
            new GraphRefAst(graphIri, false, true, false);
        });
    }

    @Test
    public void constructorExclusiveValuesGraphDefault() {
        assertThrows(QueryEvaluationException.class, () -> {
            IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
            new GraphRefAst(graphIri, false, false, true);
        });
    }

    @Test
    public void constructorExclusiveValuesNamedAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(true, true, false);
        });
    }

    @Test
    public void constructorExclusiveValuesNamedDefault() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(true, false, true);
        });
    }

    @Test
    public void constructorExclusiveValuesDefaultAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(false, true, true);
        });
    }

    @Test
    public void constructorExclusiveValuesNamedDefaultAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(true, true, true);
        });
    }

    @Test
    public void constructorExclusiveValuesGraphNamedDefaultAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
            new GraphRefAst(graphIri, true, true, true);
        });
    }
}
