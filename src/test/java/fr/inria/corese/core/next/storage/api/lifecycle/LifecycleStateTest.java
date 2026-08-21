package fr.inria.corese.core.next.storage.api.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LifecycleState enum.
 */
@DisplayName("LifecycleState Enum Tests")
class LifecycleStateTest {

    @Test
    @DisplayName("Should have all 5 lifecycle states defined")
    void testAllStatesExist() {
        assertNotNull(LifecycleState.NOT_INITIALIZED);
        assertNotNull(LifecycleState.INITIALIZING);
        assertNotNull(LifecycleState.RUNNING);
        assertNotNull(LifecycleState.SHUTTING_DOWN);
        assertNotNull(LifecycleState.SHUTDOWN);
    }

    @Test
    @DisplayName("Should return true for isRunning() only when state is RUNNING")
    void testIsRunning() {
        assertTrue(LifecycleState.RUNNING.isRunning());

        assertFalse(LifecycleState.NOT_INITIALIZED.isRunning());
        assertFalse(LifecycleState.INITIALIZING.isRunning());
        assertFalse(LifecycleState.SHUTTING_DOWN.isRunning());
        assertFalse(LifecycleState.SHUTDOWN.isRunning());
    }

    @Test
    @DisplayName("Should return correct description for each state")
    void testDescriptions() {
        assertEquals("Not initialized", LifecycleState.NOT_INITIALIZED.getDescription());
        assertEquals("Initializing", LifecycleState.INITIALIZING.getDescription());
        assertEquals("Running", LifecycleState.RUNNING.getDescription());
        assertEquals("Shutting down", LifecycleState.SHUTTING_DOWN.getDescription());
        assertEquals("Shutdown", LifecycleState.SHUTDOWN.getDescription());
    }

    @Test
    @DisplayName("Should have exactly 5 enum values")
    void testEnumValues() {
        LifecycleState[] states = LifecycleState.values();
        assertEquals(5, states.length);
    }

    @Test
    @DisplayName("Should correctly resolve valueOf() for valid and invalid state names")
    void testValueOf() {
        assertEquals(LifecycleState.RUNNING, LifecycleState.valueOf("RUNNING"));
        assertThrows(IllegalArgumentException.class,
                () -> LifecycleState.valueOf("INVALID"));
    }
}