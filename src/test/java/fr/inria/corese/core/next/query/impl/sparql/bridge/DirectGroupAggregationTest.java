package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateFunction;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectGroupAggregationTest {
    private static final Node GROUP = NodeImpl.forVariable("?g");
    private static final Node VALUE = NodeImpl.forVariable("?v");

    @Test
    void aggregatesAllScalarsOnFirstRepresentative() {
        QueryPlan query = query();
        Mappings mappings = Mappings.create(query);
        Mapping first = row("a", integer(10));
        mappings.add(first);
        mappings.add(row("b", integer(7)));
        mappings.add(row("a", integer(20)));
        mappings.add(row("a", null));

        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertEquals(2, mappings.size());
        assertSame(first, mappings.get(0));
        assertValue(first, "?all", "3");
        assertValue(first, "?count", "2");
        assertValue(first, "?sum", "30");
        assertValue(first, "?avg", "15");
        assertValue(first, "?min", "10");
        assertValue(first, "?max", "20");
        assertEquals(XSDDatatype.DECIMAL,
                ((Literal) first.getNode("?avg").getDatatypeValue()).getCoreDatatype());
    }

    @Test
    void allUnboundValuesAndUnboundGroupKey() {
        QueryPlan query = query();
        Mappings mappings = Mappings.create(query);
        Mapping first = row(null, null);
        mappings.add(first);
        mappings.add(row(null, null));
        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertEquals(1, mappings.size());
        assertValue(first, "?all", "2");
        assertValue(first, "?count", "0");
        assertValue(first, "?sum", "0");
        assertNull(first.getNode("?avg"));
        assertNull(first.getNode("?min"));
        assertNull(first.getNode("?max"));
    }

    @Test
    void numericErrorDoesNotAffectOtherAggregates() {
        QueryPlan query = query();
        Mappings mappings = Mappings.create(query);
        Mapping first = row("a", integer(10));
        mappings.add(first);
        mappings.add(row("a", NodeImpl.forLiteral("text", null, null)));
        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertNull(first.getNode("?sum"));
        assertNull(first.getNode("?avg"));
        assertValue(first, "?count", "2");
        assertValue(first, "?max", "text");
    }

    @Test
    void promotesMixedNumericTypesAndPreservesLargeIntegers() {
        QueryPlan query = query();
        Mappings mappings = Mappings.create(query);
        Mapping first = row("a", integer(1));
        mappings.add(first);
        mappings.add(row("a", NodeImpl.forValue(Values.factory().createLiteral(new BigDecimal("1.5")))));
        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertValue(first, "?sum", "2.5");
        assertValue(first, "?avg", "1.25");

        mappings = Mappings.create(query);
        BigInteger large = BigInteger.ONE.shiftLeft(80);
        first = row("a", NodeImpl.forValue(Values.factory().createLiteral(large)));
        mappings.add(first);
        mappings.add(row("a", integer(1)));
        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertValue(first, "?sum", large.add(BigInteger.ONE).toString());
    }

    @Test
    void emptyGroupedInputProducesNoRepresentative() {
        QueryPlan query = query();
        Mappings mappings = Mappings.create(query);
        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertEquals(0, mappings.size());
    }

    @Test
    void usesAllGroupKeysAndPreparedGroupValues() {
        QueryPlan query = query();
        Node secondKey = NodeImpl.forVariable("?second");
        query.addGroupBy(secondKey);
        Mappings mappings = Mappings.create(query);
        Mapping first = row("a", integer(1));
        first.setGroupBy(new Node[]{integer(5), integer(1)});
        Mapping second = row("a", integer(2));
        second.setGroupBy(new Node[]{integer(5), integer(2)});
        Mapping third = row("b", integer(3));
        third.setGroupBy(new Node[]{integer(5), integer(1)});
        mappings.add(first);
        mappings.add(second);
        mappings.add(third);
        assertTrue(DirectGroupAggregation.tryAggregate(query, mappings));
        assertEquals(2, mappings.size());
        assertSame(first, mappings.get(0));
        assertValue(first, "?sum", "4");
        assertValue(second, "?sum", "2");
    }

    @Test
    void rejectsDistinctWithoutChangingInput() {
        QueryPlan query = query();
        query.getSelectFun().add(projection("?distinct", AggregateFunction.COUNT, true, new VarAst("?v")));
        Mappings mappings = Mappings.create(query);
        Mapping first = row("a", integer(1));
        mappings.add(first);
        assertFalse(DirectGroupAggregation.isEligible(query));
        assertFalse(DirectGroupAggregation.tryAggregate(query, mappings));
        assertSame(first, mappings.get(0));
        assertNull(first.getNode("?sum"));
    }

    @Test
    void rejectsUnsupportedModifiersAndAggregateArguments() {
        QueryPlan query = query();
        query.addOrderBy(GROUP);
        assertFalse(DirectGroupAggregation.isEligible(query));
        query = query();
        query.getGroupBy().clear();
        assertFalse(DirectGroupAggregation.isEligible(query));
        query = query();
        query.getSelectFun().add(projection("?sample", AggregateFunction.SAMPLE, false, new VarAst("?v")));
        assertFalse(DirectGroupAggregation.isEligible(query));
        query = query();
        query.getSelectFun().add(projection("?invalid", AggregateFunction.SUM, false, null));
        assertFalse(DirectGroupAggregation.isEligible(query));
    }

    private static QueryPlan query() {
        QueryPlan query = QueryPlan.create();
        query.addGroupBy(GROUP);
        query.addSelect(GROUP);
        query.getSelectFun().add(projection("?all", AggregateFunction.COUNT, false, null));
        query.getSelectFun().add(projection("?count", AggregateFunction.COUNT, false, new VarAst("?v")));
        query.getSelectFun().add(projection("?sum", AggregateFunction.SUM, false, new VarAst("?v")));
        query.getSelectFun().add(projection("?avg", AggregateFunction.AVG, false, new VarAst("?v")));
        query.getSelectFun().add(projection("?min", AggregateFunction.MIN, false, new VarAst("?v")));
        query.getSelectFun().add(projection("?max", AggregateFunction.MAX, false, new VarAst("?v")));
        return query;
    }

    private static Exp projection(String target, AggregateFunction function, boolean distinct, VarAst variable) {
        Exp exp = Exp.create(Type.NODE, NodeImpl.forVariable(target));
        exp.setFilter(new AstBackedExpr(new AggregateAst(function, distinct, variable, null)).getFilter());
        return exp;
    }

    private static Mapping row(String group, Node value) {
        Node groupValue = group == null ? null : NodeImpl.forLiteral(group, null, null);
        return Mapping.create(new Node[]{GROUP, VALUE}, new Node[]{groupValue, value});
    }

    private static Node integer(long value) {
        return NodeImpl.forValue(Values.factory().createLiteral(BigInteger.valueOf(value)));
    }

    private static void assertValue(Mapping mapping, String variable, String expected) {
        assertEquals(expected, mapping.getNode(variable).getDatatypeValue().stringValue());
    }
}
