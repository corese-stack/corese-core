package fr.inria.corese.core.next.common.config;

import fr.inria.corese.core.next.common.exception.ConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPropertiesTest {

    private enum MockProperty implements ConfigurationProperty {
        FOOBAR("foo.bar", null, false),
        FOOFOOBAR("foo.foo.bar", null, false),
        FOOFOOBARBAR("foo.foo.bar.bar", null, true),
        INTPROP("foo.intprop", "0", false),
        INTOPTPROP("foo.intoptprop", "0", true);

        private final String name;
        private final String defaultValue;
        private final boolean optional;

        MockProperty(String name, String defaultValue, boolean optional) {
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
    @DisplayName("Configuration returns the value of a required property")
    void getPropertyValue() {
        assertEquals("foobar", MockProperty.FOOBAR.getValue().orElseThrow());
    }

    @Test
    @DisplayName("Configuration returns the value of a required integer property")
    void getPropertyIntValue() {
        assertEquals(4, Integer.valueOf(MockProperty.INTPROP.getValue().orElseThrow()));
    }

    @Test
    @DisplayName("Configuration returns the default value of an optional integer property")
    void getPropertyDefaultIntValue() {
        assertEquals(0, Integer.valueOf(MockProperty.INTOPTPROP.getValue().orElseThrow()));
    }

    @Test
    @DisplayName("Configuration rejects a missing required property")
    void throwExceptionNonExistentProperty() {
        assertThrows(ConfigurationException.class, MockProperty.FOOFOOBAR::getValue);
    }

    @Test
    @DisplayName("Configuration accepts a missing optional property")
    void throwDoNotThrowNonExistentNonOptionalProperty() {
        assertDoesNotThrow(MockProperty.FOOFOOBARBAR::getValue);
    }

}
