package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupAggregatorTest {

    private GroupAggregator original;

    @BeforeEach
    void setUp() {
        original = Mappings.getGroupAggregator();
    }

    @AfterEach
    void tearDown() {
        Mappings.setGroupAggregator(original);
    }

    @Test
    void canConfigureAndRetrieveCustomGroupAggregator() {
        GroupAggregator mockAggregator = (query, mappings) -> true;
        Mappings.setGroupAggregator(mockAggregator);

        assertSame(mockAggregator, Mappings.getGroupAggregator());
        assertTrue(Mappings.getGroupAggregator().tryAggregate(new Query(), new Mappings()));
    }
}
