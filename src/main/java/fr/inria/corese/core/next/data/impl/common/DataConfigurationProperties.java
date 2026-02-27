package fr.inria.corese.core.next.data.impl.common;

import fr.inria.corese.core.next.util.ConfigurationProperty;
import fr.inria.corese.core.next.util.Properties;
import fr.inria.corese.core.next.util.exception.ConfigurationException;

import java.util.Optional;

/**
 * Configuration properties for the high-level data management API of Corese
 */
public enum DataConfigurationProperties implements ConfigurationProperty {
    DEFAULT_BASE_URI("core.default-uri", "https://ns.inria.fr/corese/");

    private final String name;
    private final String defaultValue;
    private final boolean optional;

    /**
     * Main constructor.
     * @param name The path of the property in the configuration file (e.g. "core.default-uri")
     * @param defaultValue The default value of the property
     * @param optional Flag indicating if the property has to be present in the configuration file, or not
     * @throws ConfigurationException if the property is not optional and not defined in the configuration file
     */
    DataConfigurationProperties(String name, String defaultValue, boolean optional) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.optional = optional;
        if(! this.optional && ! Properties.instance().propertyValueExists(this)) {
            throw new ConfigurationException(this.name + " data configuration is not present in the configuration file");
        }
    }

    /**
     * Constructor without default value
     * @param name The path of the property in the configuration file (e.g. "core.default-uri")
     * @param optional Flag indicating if the property has to be present in the configuration file, or not
     */
    DataConfigurationProperties(String name, boolean optional) {
        this(name, null, optional);
    }

    /**
     * Constructor with a default value. A property with a default value is expected to be optional.
     * @param name The path of the property in the configuration file (e.g. "core.default-uri")
     * @param defaultValue String representation of the default value
     */
    DataConfigurationProperties(String name, String defaultValue) {
        this(name, defaultValue, true);
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
