package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.storage.ProducerDefault;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompiledQueryPlanTest {
    private static final String TEXT = """
            SELECT ?s ?label {
              VALUES ?s { <urn:s> <urn:s> }
              ?s <urn:p> ?o
              BIND(STR(?o) AS ?label)
              FILTER EXISTS { ?s <urn:p> ?o }
            }
            """;

    @Test
    void captureDetachesTheCompilerAndEveryExecutionHasIndependentMutableState() {
        Query compiler = query();
        CompiledQueryPlan image = CompiledQueryPlan.capture(compiler, Map.of());
        Query first = image.newExecution(null);
        Query second = image.newExecution(null);
        assertNotSame(first, second);
        assertEquals(first.getSelect().size(), second.getSelect().size());
        Node firstVariable = first.getSelect().getFirst();
        Node secondVariable = second.getSelect().getFirst();
        assertNotSame(firstVariable, secondVariable);
        int compiledIndex = secondVariable.getIndex();
        firstVariable.setIndex(999);
        first.getSelect().clear();
        first.setLimit(1);
        compiler.getSelect().clear();
        compiler.setLimit(2);
        assertEquals(compiledIndex, secondVariable.getIndex());
        Query fresh = image.newExecution(null);
        assertEquals(2, fresh.getSelect().size());
        assertEquals(Integer.MAX_VALUE, fresh.getLimit());
        assertEquals(compiledIndex, fresh.getSelect().getFirst().getIndex());
        assertEquals(2, second.getSelect().size());
    }

    @Test
    void valuesRowsAndFilterIndexesAreNotShared() {
        CompiledQueryPlan image = CompiledQueryPlan.capture(query(), Map.of());
        Query first = image.newExecution(null);
        Query second = image.newExecution(null);
        Exp firstValues = operators(first).stream().filter(e -> e.type() == Exp.Type.VALUES).findFirst().orElseThrow();
        Exp secondValues = operators(second).stream().filter(e -> e.type() == Exp.Type.VALUES).findFirst().orElseThrow();
        assertNotSame(firstValues.getMappings(), secondValues.getMappings());
        assertNotSame(firstValues.getMappings().get(0), secondValues.getMappings().get(0));
        firstValues.getMappings().get(0).getTargetNodes()[0] = NodeImpl.forIRI("urn:changed");
        assertEquals("urn:s", secondValues.getMappings().get(0).getTargetNodes()[0].getLabel());
        Exp firstBind = operators(first).stream().filter(Exp::isBind).findFirst().orElseThrow();
        Exp secondBind = operators(second).stream().filter(Exp::isBind).findFirst().orElseThrow();
        assertNotSame(firstBind.getFilter(), secondBind.getFilter());
        int index = secondBind.getFilter().getExp().getIndex();
        firstBind.getFilter().getExp().setIndex(731);
        assertEquals(index, secondBind.getFilter().getExp().getIndex());
        firstValues.getMappings().clear();
        assertEquals(2, secondValues.getMappings().size());
    }

    @Test
    void copiesPreserveIdentityWithinAFrameAndRejectUnknownMutablePayloads() {
        Query first = CompiledQueryPlan.capture(query(), Map.of()).newExecution(null);
        Exp values = operators(first).stream().filter(e -> e.type() == Exp.Type.VALUES).findFirst().orElseThrow();
        Node variable = first.getSelect().getFirst();
        assertSame(variable, values.getNodeList().getFirst());
        assertSame(variable, values.getMappings().get(0).getQueryNodes()[0]);
        assertThrows(IllegalArgumentException.class, () -> new PlanCopy().copy(new StringBuilder()));
    }

    private static Query query() {
        var ast = (SelectQueryAst) new SparqlParser().parse(TEXT);
        Query query = new CoreseAstQueryBuilder().toNextQuery(ast);
        query.complete(new ProducerDefault());
        return query;
    }

    private static List<Exp> operators(Exp expression) {
        List<Exp> result = new ArrayList<>();
        result.add(expression);
        for (Exp child : expression) result.addAll(operators(child));
        return result;
    }
}
