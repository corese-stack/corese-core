package fr.inria.corese.core.next.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import fr.inria.corese.core.next.util.exception.ConfigurationException;

import java.util.Optional;

public class Properties {

    private static final String CONFIG_FILE = "application.conf";

    private final Config config;
    private static Properties INSTANCE = null;

    private Properties() {
        try {
            this.config = ConfigFactory.load(CONFIG_FILE);
        } catch (ConfigException e) {
            throw new ConfigurationException("Error during configuration loading", e);
        }
    }

    public static Properties instance() {
        if (INSTANCE == null) {
            INSTANCE = new Properties();
        }
        return INSTANCE;
    }

    public Optional<String> getPropertyValue(ConfigurationProperty property) {
        if (propertyValueExists(property)) {
            return Optional.of(this.config.getString(property.getName()));
        } else if (property.isOptional()) {
            if (property.getDefaultValue().isPresent()) {
                return property.getDefaultValue();
            } else {
                return Optional.empty();
            }
        } else {
            throw new ConfigurationException("Undefined configuration " + property.getName());
        }
    }

    public boolean propertyValueExists(ConfigurationProperty property) {
        return this.config.hasPath(property.getName());
    }
}
