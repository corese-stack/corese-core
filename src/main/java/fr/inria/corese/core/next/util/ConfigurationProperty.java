package fr.inria.corese.core.next.util;

import java.util.Optional;

/**
 * Interface to be inherited by all enum classes defining configuration properties.
 * This is done to ensure checks and defaults properties
 */
public interface ConfigurationProperty {

    String getName();
    Optional<String> getDefaultValue();
    boolean isOptional();
}
