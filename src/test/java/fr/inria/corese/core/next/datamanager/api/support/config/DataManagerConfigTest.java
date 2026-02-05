package fr.inria.corese.core.next.datamanager.api.support.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataManagerConfig.
 */
@DisplayName("DataManagerConfig Tests")
class DataManagerConfigTest {

    @Test
    @DisplayName("Should build config with all properties set")
    void testBuilderWithAllProperties() {
        DataManagerConfig config = DataManagerConfig.builder()
                .debug(true)
                .transactionSupport(true)
                .property("timeout", 5000)
                .property("maxConnections", 100)
                .build();

        assertTrue(config.isDebug());
        assertEquals(Optional.of(5000), config.getProperty("timeout", Integer.class));
        assertEquals(Optional.of(100), config.getProperty("maxConnections", Integer.class));
    }

    @Test
    @DisplayName("Should use default values when no properties are set")
    void testDefaultValues() {
        DataManagerConfig config = DataManagerConfig.builder()
                .build();

        assertFalse(config.isDebug());
        assertTrue(config.getProperties().isEmpty());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid property keys or values")
    void testPropertyValidation() {
        DataManagerConfig.Builder builder = DataManagerConfig.builder();

        assertThrows(IllegalArgumentException.class,
                () -> builder.property(null, "value"));

        assertThrows(IllegalArgumentException.class,
                () -> builder.property("", "value"));

        assertThrows(IllegalArgumentException.class,
                () -> builder.property("   ", "value"));

        assertThrows(IllegalArgumentException.class,
                () -> builder.property("key", null));
    }

    @Test
    @DisplayName("Should retrieve typed property correctly and return empty for wrong type")
    void testGetPropertyTyped() {
        DataManagerConfig config = DataManagerConfig.builder()
                .property("timeout", 5000)
                .property("name", "test")
                .build();

        Optional<Integer> timeout = config.getProperty("timeout", Integer.class);
        assertTrue(timeout.isPresent());
        assertEquals(5000, timeout.get());

        Optional<String> timeoutAsString = config.getProperty("timeout", String.class);
        assertFalse(timeoutAsString.isPresent());

        Optional<Integer> missing = config.getProperty("missing", Integer.class);
        assertFalse(missing.isPresent());
    }

    @Test
    @DisplayName("Should retrieve untyped property as Optional<Object>")
    void testGetPropertyUntyped() {
        DataManagerConfig config = DataManagerConfig.builder()
                .property("key", "value")
                .build();

        Optional<Object> value = config.getProperty("key");
        assertTrue(value.isPresent());
        assertEquals("value", value.get());

        Optional<Object> missing = config.getProperty("missing");
        assertFalse(missing.isPresent());
    }

    @Test
    @DisplayName("Should return all properties as immutable map")
    void testGetAllProperties() {
        DataManagerConfig config = DataManagerConfig.builder()
                .property("key1", "value1")
                .property("key2", 123)
                .build();

        Map<String, Object> properties = config.getProperties();
        assertEquals(2, properties.size());
        assertEquals("value1", properties.get("key1"));
        assertEquals(123, properties.get("key2"));
    }

    @Test
    @DisplayName("Should ensure properties map is immutable")
    void testPropertiesImmutability() {
        DataManagerConfig config = DataManagerConfig.builder()
                .property("key", "value")
                .build();

        Map<String, Object> properties = config.getProperties();

        assertThrows(UnsupportedOperationException.class,
                () -> properties.put("newKey", "newValue"));
    }

    @Test
    @DisplayName("Should include debug and transactionSupport in toString() output")
    void testToString() {
        DataManagerConfig config = DataManagerConfig.builder()
                .debug(true)
                .transactionSupport(false)
                .property("test", "value")
                .build();

        String str = config.toString();
        assertTrue(str.contains("DataManagerConfig"));
        assertTrue(str.contains("debug=true"));
        assertTrue(str.contains("transactionSupport=false"));
    }

    @Test
    @DisplayName("Should support builder method chaining with last value winning")
    void testBuilderChaining() {
        DataManagerConfig config = DataManagerConfig.builder()
                .debug(true)
                .transactionSupport(true)
                .property("key1", "value1")
                .property("key2", 123)
                .debug(false)
                .build();

        assertFalse(config.isDebug());
    }

    @Test
    @DisplayName("Should create independent instances on multiple builds from same builder")
    void testMultipleBuilds() {
        DataManagerConfig.Builder builder = DataManagerConfig.builder()
                .debug(true)
                .property("key", "value");

        DataManagerConfig config1 = builder.build();
        DataManagerConfig config2 = builder.build();

        assertTrue(config1.isDebug());
        assertTrue(config2.isDebug());
        assertNotSame(config1, config2);
    }
}