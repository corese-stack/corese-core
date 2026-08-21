package fr.inria.corese.core.next.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import fr.inria.corese.core.next.common.exception.ConfigurationException;

import java.util.Optional;

/** Loads and exposes the shared Corese configuration. */
public final class ConfigurationProperties {

    private static final String CONFIG_FILE = "application.conf";

    private final Config config;

    private ConfigurationProperties() {
        try {
            this.config = ConfigFactory.load(CONFIG_FILE);
        } catch (ConfigException e) {
            throw new ConfigurationException("Error during configuration loading", e);
        }
    }

    public static ConfigurationProperties instance() {
        return Holder.INSTANCE;
    }

    public Optional<String> getValue(ConfigurationProperty property) {
        if (contains(property)) {
            return Optional.of(this.config.getString(property.getName()));
        }
        if (property.isOptional()) {
            return property.getDefaultValue();
        }
        throw new ConfigurationException("Required configuration property is missing: " + property.getName());
    }

    public boolean contains(ConfigurationProperty property) {
        return this.config.hasPath(property.getName());
    }

    private static final class Holder {
        private static final ConfigurationProperties INSTANCE = new ConfigurationProperties();
    }
}
