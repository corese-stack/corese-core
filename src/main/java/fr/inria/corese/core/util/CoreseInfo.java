package fr.inria.corese.core.util;

/**
 * The {@code CoreseInfo} class provides information about the Corese
 * application.
 */
public class CoreseInfo {

    private static final String VERSION = "4.6.4-SNAPSHOT";

    /**
     * Retrieves the current version of the Corese application.
     *
     * @return the version of Corese as a {@code String}.
     */
    public static String getVersion() {
        return VERSION;
    }
}
