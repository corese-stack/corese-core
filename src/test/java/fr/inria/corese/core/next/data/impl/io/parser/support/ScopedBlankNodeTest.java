package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.storage.StorageModels;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AbstractTurtleTriGListener#scopedBlankNode(String)}.
 *
 * <p>The method ensures that blank-node labels are scoped to a single parse session:
 * the same label maps to the same {@link BNode} within one document but to a different
 * {@link BNode} in every other document.</p>
 */
@DisplayName("AbstractTurtleTriGListener - blank-node document scoping")
class ScopedBlankNodeTest {

    private static final ValueFactory FACTORY = Values.factory();

    /** Creates a fresh listener backed by an in-memory model. */
    private static AbstractTurtleTriGListener listener() {
        return new AbstractTurtleTriGListener(
                StorageModels.create(MemoryStorageManager.builder().build()),
                FACTORY,
                "") {};
    }

    @Test
    @DisplayName("same label returns the same BNode instance within one document")
    void sameLabelReturnsSameBNodeWithinDocument() {
        AbstractTurtleTriGListener l = listener();
        BNode first = l.scopedBlankNode("x");
        BNode second = l.scopedBlankNode("x");
        assertSame(first, second,
                "repeated calls with the same label must return the same BNode");
    }

    @Test
    @DisplayName("different labels return distinct BNodes within one document")
    void differentLabelsReturnDifferentBNodes() {
        AbstractTurtleTriGListener l = listener();
        BNode b1 = l.scopedBlankNode("a");
        BNode b2 = l.scopedBlankNode("b");
        assertNotEquals(b1, b2,
                "different labels must map to distinct BNode instances");
    }

    @Test
    @DisplayName("separate listener instances produce distinct BNodes for the same label")
    void separateListenersProduceDistinctBNodesForSameLabel() {
        BNode fromFirst = listener().scopedBlankNode("x");
        BNode fromSecond = listener().scopedBlankNode("x");
        assertNotEquals(fromFirst, fromSecond,
                "blank nodes from separate parse sessions must be distinct even when labels match");
    }
}
