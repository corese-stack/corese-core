package fr.inria.corese.core.next.query.impl.engine.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationStateTest {

    @Test
    void initialStateShouldHaveDefaultValues() {
        EvaluationState state = new EvaluationState();

        assertEquals(0, state.getNbEdge());
        assertEquals(0, state.getNbCall());
        assertEquals(0, state.getNbResult());
        assertEquals(-1, state.getBackjump());
        assertFalse(state.hasBackjump());
        assertEquals(-1, state.getMaxLevel());
        assertEquals(Integer.MAX_VALUE, state.getLimit());
        assertFalse(state.isStopped());
    }

    @Test
    void countersShouldIncrementCorrectly() {
        EvaluationState state = new EvaluationState();

        state.incrementEdge();
        state.incrementEdge();
        state.incrementCall();
        state.incrementResult();

        assertEquals(2, state.getNbEdge());
        assertEquals(1, state.getNbCall());
        assertEquals(1, state.getNbResult());
    }

    @Test
    void maxLevelShouldTrackHighestDepth() {
        EvaluationState state = new EvaluationState();

        state.updateMaxLevel(2);
        assertEquals(2, state.getMaxLevel());

        state.updateMaxLevel(5);
        assertEquals(5, state.getMaxLevel());

        state.updateMaxLevel(3);
        assertEquals(5, state.getMaxLevel());
    }

    @Test
    void backjumpLifecycleShouldWork() {
        EvaluationState state = new EvaluationState();

        state.setBackjump(3);
        assertTrue(state.hasBackjump());
        assertEquals(3, state.getBackjump());

        state.clearBackjump();
        assertFalse(state.hasBackjump());
        assertEquals(-1, state.getBackjump());
    }

    @Test
    void stopAndResetShouldOperateCorrectly() {
        EvaluationState state = new EvaluationState();

        state.stop();
        assertTrue(state.isStopped());

        state.setLimit(100);
        state.incrementCall();
        state.setBackjump(2);

        state.reset();
        assertFalse(state.isStopped());
        assertEquals(Integer.MAX_VALUE, state.getLimit());
        assertEquals(0, state.getNbCall());
        assertFalse(state.hasBackjump());
    }
}
