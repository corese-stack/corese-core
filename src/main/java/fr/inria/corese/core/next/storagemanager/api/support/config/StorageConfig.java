package fr.inria.corese.core.next.storagemanager.api.support.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for StorageManager instances.
 */
public final class StorageConfig {

    /**
     * Custom configuration properties (immutable).
     */
    private final Map<String, Object> properties;

    /**
     * Whether transaction support is enabled.
     */
    private final boolean transactionSupport;


    /**
     * Private constructor — use {@link #builder()} to create instances.
     *
     * @param builder the builder containing configuration values
     */
    private StorageConfig(Builder builder) {
        this.properties = Map.copyOf(builder.properties);
        this.transactionSupport = builder.transactionSupport;
    }

    /**
     * Returns a configuration property by key.
     *
     * @param key the property key (must not be {@code null})
     * @return an {@link Optional} containing the property value, or empty if not found
     */
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Returns a typed configuration property by key.
     *
     * @param <T>  the expected type of the property
     * @param key  the property key (must not be {@code null})
     * @param type the expected class of the property value
     * @return an {@link Optional} containing the typed value, or empty if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        return type.isInstance(value) ? Optional.of((T) value) : Optional.empty();
    }

    /**
     * Returns all configuration properties as an immutable map.
     *
     * @return an immutable copy of all custom properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }


    /**
     * Creates a new {@link Builder} for constructing {@code StorageConfig} instances.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "StorageConfig{" +
                "transactionSupport=" + transactionSupport +
                ", properties=" + properties +
                '}';
    }

    /**
     * Builder for constructing {@link StorageConfig} instances.
     */
    public static final class Builder {


        private final Map<String, Object> properties = new HashMap<>();

        private final boolean transactionSupport = false;

        /**
         * Private constructor — use {@link StorageConfig#builder()}.
         */
        private Builder() {
        }

        /**
         * Adds a custom property to the configuration.
         *
         * @param key   the property key (must not be {@code null} or blank)
         * @param value the property value (must not be {@code null})
         * @return this builder for method chaining
         * @throws IllegalArgumentException if {@code key} is {@code null} or blank,
         *                                  or if {@code value} is {@code null}
         */
        public Builder property(String key, Object value) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Property key cannot be null or blank");
            }
            if (value == null) {
                throw new IllegalArgumentException("Property value cannot be null");
            }
            properties.put(key, value);
            return this;
        }


        /**
         * Builds the {@link StorageConfig} instance with the current configuration.
         *
         * @return a new immutable {@code StorageConfig}
         */
        public StorageConfig build() {
            return new StorageConfig(this);
        }
    }
}