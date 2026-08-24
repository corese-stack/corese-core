package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.GraphRefAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SparqlParserGraphRefAstTest extends AbstractSparqlParserFeatureTest {

    @Test
    void constructorGraph() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        GraphRefAst graphRefAst = new GraphRefAst(graphIri);
        assertNotNull(graphRefAst);
        assertEquals(graphIri, graphRefAst.graph());
        assertFalse(graphRefAst.named());
        assertFalse(graphRefAst.all());
        assertFalse(graphRefAst.defaultGraph());
    }

    @Test
    void constructorNamed() {
        GraphRefAst graphRefAst = new GraphRefAst(true, false, false);
        assertNotNull(graphRefAst);
        assertTrue(graphRefAst.named());
        assertNull(graphRefAst.graph());
        assertFalse(graphRefAst.all());
        assertFalse(graphRefAst.defaultGraph());
    }

    @Test
    void constructorAll() {
        GraphRefAst graphRefAst = new GraphRefAst(false, true, false);
        assertNotNull(graphRefAst);
        assertFalse(graphRefAst.named());
        assertNull(graphRefAst.graph());
        assertTrue(graphRefAst.all());
        assertFalse(graphRefAst.defaultGraph());
    }

    @Test
    void constructorDefault() {
        GraphRefAst graphRefAst = new GraphRefAst(false, false, true);
        assertNotNull(graphRefAst);
        assertFalse(graphRefAst.named());
        assertNull(graphRefAst.graph());
        assertFalse(graphRefAst.all());
        assertTrue(graphRefAst.defaultGraph());
    }

    @Test
    void constructorExclusiveValuesGraphNamed() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, true, false, false);
        });
    }

    @Test
    void constructorExclusiveValuesGraphAll() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, false, true, false);
        });
    }

    @Test
    void constructorExclusiveValuesGraphDefault() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, false, false, true);
        });
    }

    @Test
    void constructorExclusiveValuesNamedAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(true, true, false);
        });
    }

    @Test
    void constructorExclusiveValuesNamedDefault() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(true, false, true);
        });
    }

    @Test
    void constructorExclusiveValuesDefaultAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(false, true, true);
        });
    }

    @Test
    void constructorExclusiveValuesNamedDefaultAll() {
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(true, true, true);
        });
    }

    @Test
    void constructorExclusiveValuesGraphNamedAll() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, true, true, false);
        });
    }

    @Test
    void constructorExclusiveValuesGraphNamedDefault() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, true, false, true);
        });
    }

    @Test
    void constructorExclusiveValuesGraphAllDefault() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, false, true, true);
        });
    }

    @Test
    void constructorExclusiveValuesGraphNamedDefaultAll() {
        IriAst graphIri = new IriAst("<http://ns.inria.fr/test>");
        assertThrows(QueryEvaluationException.class, () -> {
            new GraphRefAst(graphIri, true, true, true);
        });
    }
}
