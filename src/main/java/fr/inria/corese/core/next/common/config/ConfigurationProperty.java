package fr.inria.corese.core.next.common.config;

import java.util.Optional;

/**
 * Describes a typed configuration key and its optional default value.
 */
public interface ConfigurationProperty {

    /**
     * @return The path of the property in the configuration file
     */
    String getName();

    /**
     * @return the configured value, or the default value of an optional property
     * @throws fr.inria.corese.core.next.common.exception.ConfigurationException if a required property is absent
     */
    default Optional<String> getValue() {
        return ConfigurationProperties.instance().getValue(this);
    }

    /**
     * @return The string value representation of the default value if there is one
     */
    Optional<String> getDefaultValue();

    /**
     * @return whether the property may be absent from the configuration file
     */
    boolean isOptional();
}
