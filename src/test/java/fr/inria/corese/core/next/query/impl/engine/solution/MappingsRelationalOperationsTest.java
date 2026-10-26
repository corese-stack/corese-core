package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("MappingsRelationalOperations Tests")
class MappingsRelationalOperationsTest {

    @Test
    @DisplayName("union combines both solution tables")
    void testUnion() {
        Mappings m1 = new Mappings();
        m1.add(Mapping.create());

        Mappings m2 = new Mappings();
        m2.add(Mapping.create());
        m2.add(Mapping.create());

        MappingsRelationalOperations ops = new MappingsRelationalOperations(m1);
        Mappings res = ops.union(m2);

        assertEquals(3, res.size());
    }

    @Test
    @DisplayName("getCommonNode returns common node or null")
    void testGetCommonNode() {
        Mappings m1 = new Mappings();
        Mappings m2 = new Mappings();
        MappingsRelationalOperations ops = new MappingsRelationalOperations(m1);

        assertNull(ops.getCommonNode(m2));

        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("1", null, null);

        Mapping row1 = Mapping.create();
        row1.addNode(x, v1);
        m1.add(row1);

        Mapping row2 = Mapping.create();
        row2.addNode(x, v1);
        m2.add(row2);

        assertNotNull(ops.getCommonNode(m2));
    }

    @Test
    @DisplayName("minus filters compatible solutions")
    void testMinus() {
        Node x = NodeImpl.forVariable("?x");
        Node v1 = NodeImpl.forLiteral("val", null, null);

        Mappings m1 = new Mappings();
        Mapping row1 = Mapping.create();
        row1.addNode(x, v1);
        m1.add(row1);

        Mappings m2 = new Mappings();
        Mapping row2 = Mapping.create();
        row2.addNode(x, v1);
        m2.add(row2);

        MappingsRelationalOperations ops = new MappingsRelationalOperations(m1);
        Mappings res = ops.minus(m2);

        assertEquals(0, res.size());
    }
}
