package fr.inria.corese.core.next.data.api.config;

import java.util.Optional;

import fr.inria.corese.core.next.common.config.ConfigurationProperty;

/** Configuration keys owned by the data layer. */
public enum DataProperty implements ConfigurationProperty {
    DEFAULT_BASE_URI("core.default-uri", "https://ns.inria.fr/corese/");

    private final String name;
    private final String defaultValue;

    DataProperty(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.of(defaultValue);
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
