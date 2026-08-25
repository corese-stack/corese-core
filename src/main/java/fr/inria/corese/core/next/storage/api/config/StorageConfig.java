package fr.inria.corese.core.next.storage.api.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for StorageManager instances.
 */
public final class StorageConfig {

    private static final String TYPE_PROPERTY = "type";
    private static final String MEMORY_TYPE = "memory";

    /**
     * Custom configuration properties (immutable).
     */
    private final Map<String, Object> properties;

    /**
     * Private constructor — use {@link #builder()} to create instances.
     *
     * @param builder the builder containing configuration values
     */
    private StorageConfig(Builder builder) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    /**
     * Returns a configuration property by key.
     *
     * @param key the property key (must not be {@code null})
     * @return an {@link Optional} containing the property value, or empty if not found
     */
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(properties.get(Objects.requireNonNull(key, "key")));
    }

    /**
     * Returns a typed configuration property by key.
     *
     * @param <T>  the expected type of the property
     * @param key  the property key (must not be {@code null})
     * @param type the expected class of the property value
     * @return an {@link Optional} containing the typed value, or empty if not found or wrong type
     */
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object value = properties.get(Objects.requireNonNull(key, "key"));
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
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
     * Returns the storage type from properties.
     *
     * <p>This is a convenience method for {@code getProperty("type", String.class)}.
     * The type is used by the plugin system to select the appropriate StorageManager.
     *
     * @return the storage type, or empty if not set
     */
    public Optional<String> getType() {
        return getProperty(TYPE_PROPERTY, String.class);
    }

    /**
     * Creates a new {@link Builder} for constructing {@code StorageConfig} instances.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns the standard configuration for the in-memory storage backend. */
    public static StorageConfig memory() {
        return builder().type(MEMORY_TYPE).build();
    }

    @Override
    public String toString() {
        return "StorageConfig{type=" + getType().orElse("not set")
                + ", propertyKeys=" + properties.keySet() + '}';
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof StorageConfig other
                && properties.equals(other.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    /**
     * Builder for constructing {@link StorageConfig} instances.
     */
    public static final class Builder {

        private final Map<String, Object> properties = new LinkedHashMap<>();

        /**
         * Private constructor — use {@link StorageConfig#builder()}.
         */
        private Builder() {
        }

        /**
         * Selects a storage plugin by type.
         *
         * @param type storage plugin name
         * @return this builder
         */
        public Builder type(String type) {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Storage type cannot be null or blank");
            }
            return property(TYPE_PROPERTY, type.trim());
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
            if (TYPE_PROPERTY.equals(key)) {
                if (!(value instanceof String type) || type.isBlank()) {
                    throw new IllegalArgumentException("Storage type must be a non-blank string");
                }
                value = type.trim();
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
