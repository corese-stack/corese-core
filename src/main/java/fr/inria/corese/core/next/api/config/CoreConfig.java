package fr.inria.corese.core.next.api.config;

import com.typesafe.config.*;

/**
 * Configuration class for Corese Core
 */
public class CoreConfig {
    public static CoreConfig instance = null;

    Config conf;
    String logLevel;
    private CoreConfig(){
        conf = ConfigFactory.load().getConfig("core");
        logLevel = conf.getString("log.level");
    }
    public static CoreConfig getInstance() {
        if(instance==null) { instance = new CoreConfig(); }
        return instance;
    }

}
