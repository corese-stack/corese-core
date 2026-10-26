package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QueryClauses Tests")
class QueryClausesTest {

    @Test
    @DisplayName("manage select nodes and functions")
    void testSelectManagement() {
        QueryClauses clauses = new QueryClauses();
        Node x = NodeImpl.forVariable("?x");
        clauses.setSelect(List.of(x));

        assertEquals(1, clauses.getSelect().size());
        assertEquals(x, clauses.getSelectNode("?x"));
        assertNull(clauses.getSelectNode("?y"));

        clauses.addSelect(x);
        assertEquals(1, clauses.getSelectFun().size());
        assertNotNull(clauses.getSelectExp("?x"));
    }

    @Test
    @DisplayName("manage order by and group by")
    void testOrderAndGroupBy() {
        QueryClauses clauses = new QueryClauses();
        assertFalse(clauses.isOrderBy());
        assertFalse(clauses.hasGroupBy());

        Node x = NodeImpl.forVariable("?x");
        clauses.addOrderBy(x);
        clauses.addGroupBy(x);

        assertTrue(clauses.isOrderBy());
        assertTrue(clauses.hasGroupBy());
        assertEquals(1, clauses.getOrderBy().size());
        assertEquals(1, clauses.getGroupBy().size());
    }

    @Test
    @DisplayName("limit and offset calculation")
    void testLimitOffset() {
        QueryClauses clauses = new QueryClauses();
        clauses.setLimit(10);
        clauses.setOffset(5);

        assertEquals(10, clauses.getLimit());
        assertEquals(5, clauses.getOffset());
        assertEquals(15, clauses.getLimitOffset());

        clauses.setOrderBy(List.of(Exp.create(Exp.Type.NODE, NodeImpl.forVariable("?x"))));
        assertEquals(Integer.MAX_VALUE, clauses.getLimitOffset());
    }

    @Test
    @DisplayName("redundant distinct with group by detection")
    void testDistinctRedundantWithGroupBy() {
        QueryClauses clauses = new QueryClauses();
        Node x = NodeImpl.forVariable("?x");

        clauses.setDistinct(true);
        clauses.setSelect(List.of(x));
        clauses.addGroupBy(x);

        assertTrue(clauses.isDistinctRedundantWithGroupBy());

        clauses.setListGroup(true);
        assertFalse(clauses.isDistinctRedundantWithGroupBy());
    }
}
