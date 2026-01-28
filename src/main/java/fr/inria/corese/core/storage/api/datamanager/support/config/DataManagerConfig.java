package fr.inria.corese.core.storage.api.datamanager.support.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Typed configuration for the DataManager.
 */
public final class DataManagerConfig {

    private final Map<String, Object> properties;
    private final boolean transactionSupport;
    private final boolean debug;

    /**
     * Private constructor - use the Builder.
     */
    private DataManagerConfig(Builder builder) {
        this.properties = Map.copyOf(builder.properties);
        this.transactionSupport = builder.transactionSupport;
        this.debug = builder.debug;
    }

    /**
     * Returns a property.
     *
     * @param key Property key
     * @return Property value, or Optional.empty() if absent
     */
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Returns a property with a specific type.
     *
     * @param <T>  Property type
     * @param key  Property key
     * @param type Expected type class
     * @return Typed value, or Optional.empty() if absent or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Returns all properties.
     *
     * @return Immutable map of properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }


    /**
     * Indicates whether debug mode is enabled.
     *
     * @return true if debug enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Creates a new builder.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "DataManagerConfig{" +
                ", transactionSupport=" + transactionSupport +
                ", debug=" + debug +
                ", properties=" + properties +
                '}';
    }

    /**
     * Builder for DataManagerConfig.
     */
    public static final class Builder {
        private final Map<String, Object> properties = new HashMap<>();
        private boolean transactionSupport = false;
        private boolean debug = false;

        private Builder() {
        }

        /**
         * Adds a custom property to the configuration.
         *
         * @param key   Property key (must not be null, empty, or blank)
         * @param value Property value (must not be null)
         * @return this builder instance
         * @throws IllegalArgumentException if key is null/empty/blank or value is null
         */
        public Builder property(String key, Object value) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Property key cannot be null, empty, or blank");
            }
            if (value == null) {
                throw new IllegalArgumentException("Property value cannot be null");
            }
            this.properties.put(key, value);
            return this;
        }

        /**
         * Enables or disables transaction support.
         *
         * @param enable true to enable
         * @return This builder (for chaining)
         */
        public Builder enableTransactions(boolean enable) {
            this.transactionSupport = enable;
            return this;
        }

        /**
         * Enables or disables transaction support (alias for enableTransactions).
         *
         * @param enable true to enable
         * @return This builder (for chaining)
         */
        public Builder transactionSupport(boolean enable) {
            return enableTransactions(enable);
        }

        /**
         * Enables or disables debug mode.
         *
         * @param debug true to enable
         * @return This builder (for chaining)
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Builds the DataManagerConfig instance.
         *
         * @return New configured instance
         * @throws IllegalStateException if storagePath has not been set
         */
        public DataManagerConfig build() {
            return new DataManagerConfig(this);
        }
    }
}