package fr.inria.corese.core.next.util;

import fr.inria.corese.core.next.util.exception.ConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesTest {

    enum MockProperties implements ConfigurationProperty {
        FOOBAR("foo.bar", null, false),
        FOOFOOBAR("foo.foo.bar", null, false),
        FOOFOOBARBAR("foo.foo.bar.bar", null, true),
        INTPROP("foo.intprop", "0", false),
        INTOPTPROP("foo.intoptprop", "0", true);

        private String name;
        private String defaultValue;
        private boolean optional;

        MockProperties(String name, String defaultValue, boolean optional) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.optional = optional;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Optional<String> getDefaultValue() {
            return Optional.ofNullable(this.defaultValue);
        }

        @Override
        public boolean isOptional() {
            return this.optional;
        }
    }

    @Test
    @DisplayName("Properties returns the value of a non-optional property")
    void getPropertyValue() {
        assertEquals("foobar", MockProperties.FOOBAR.getValue().get());
    }

    @Test
    @DisplayName("Properties returns the value of a non-optional int property")
    void getPropertyIntValue() {
        assertEquals(4, Integer.valueOf(MockProperties.INTPROP.getValue().get()));
    }

    @Test
    @DisplayName("Properties returns the default value of an optional int property")
    void getPropertyDefaultIntValue() {
        assertEquals(0, Integer.valueOf(MockProperties.INTOPTPROP.getValue().get()));
    }

    @Test
    @DisplayName("Properties throws an exception when the value of a non-existing non-optional property is accessed")
    void throwExceptionNonExistentProperty() {
        assertThrows(ConfigurationException.class, () -> {
           MockProperties.FOOFOOBAR.getValue();
        });
    }

    @Test
    @DisplayName("Properties does not throw an exception if the value of an non-existent optional property is accessed")
    void throwDoNotThrowNonExistentNonOptionalProperty() {
        assertDoesNotThrow(() -> {
            MockProperties.FOOFOOBARBAR.getValue();
        });
    }

}