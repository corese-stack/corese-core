package fr.inria.corese.core.next.util;

import fr.inria.corese.core.next.util.exception.ConfigurationException;

import java.util.Optional;

/**
 * Interface to be inherited by all enum classes defining configuration properties.
 * it is recommended that the enum implementing this interface use {@link Properties.propertyValueExists() } in their
 * constructor to throw an exception at class loading for any undefined non-optional property.
 */
public interface ConfigurationProperty {

    /**
     * @return The path of the property in the configuration file
     */
    String getName();

    /**
     * @return The string value representation of the property if it exists in the configuration file
     * @throws fr.inria.corese.core.next.util.exception.ConfigurationException If a non-optional property has no value
     */
    default Optional<String> getValue() {
        if(Properties.instance().propertyValueExists(this)) {
            return Properties.instance().getPropertyValue(this);
        } else if(this.isOptional()) {
            return this.getDefaultValue();
        } else {
            throw new ConfigurationException("Non-optional property " + this.getName() + " has no value");
        }
    }

    /**
     * @return The string value representation of the default value if there is one
     */
    Optional<String> getDefaultValue();

    /**
     * @return A flag indicating in the value has to be defined in the configuration file or not
     */
    boolean isOptional();
}
