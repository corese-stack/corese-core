package fr.inria.corese.core.next.impl.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Configuration class for Corese Core
 */
public class CoreConfig {
    private static CoreConfig instance = null;

    private final Config config;
    private final String logLevel;

    /**
     * Private constructor to enforce singleton pattern
     * Initializes the configuration from the "core" section of the application.conf
     * file.
     * Sets the log level from the configuration.
     */
    private CoreConfig() {
        config = ConfigFactory.load().getConfig("core");
        logLevel = config.getString("log.level");
    }

    /**
     * Get the singleton instance of CoreConfig
     * 
     * @return the singleton instance of CoreConfig
     */
    public static CoreConfig getInstance() {
        if (instance == null) {
            instance = new CoreConfig();
        }
        return instance;
    }

    /**
     * Get the log level from the configuration
     * 
     * @return the log level
     */
    public String getLogLevel() {
        return logLevel;
    }

}
