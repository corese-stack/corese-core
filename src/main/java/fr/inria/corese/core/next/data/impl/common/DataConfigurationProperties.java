package fr.inria.corese.core.next.data.impl.common;

import fr.inria.corese.core.next.util.ConfigurationProperty;

import java.util.Optional;

public enum DataConfigurationProperties implements ConfigurationProperty {
    DEFAULT_BASE_URI("core.default-uri", true);

    private final String name;
    private final Optional<String> defaultValue;
    private final boolean optional;

    DataConfigurationProperties(String name, boolean optional) {
        this.name = name;
        this.defaultValue = Optional.empty();
        this.optional = optional;
    }

    DataConfigurationProperties(String name, String defaultValue, boolean optional) {
        this.name = name;
        this.defaultValue = Optional.of(defaultValue);
        this.optional = optional;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public boolean isOptional() {
        return this.optional;
    }
}
