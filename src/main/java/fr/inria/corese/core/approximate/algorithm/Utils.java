package fr.inria.corese.core.approximate.algorithm;

/**
 * Utils class
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 15 oct. 2015
 */
public class Utils {

    private Utils() {
    }
    
    /**
     * Format a double using default format "##.####"
     *
     * @param d
     * @return
     */
    public static String format(double d) {
        return String.format("%2.4f", d).replace(',', '.'); // Had to add the replace to force replace the ","
    }

    /**
     * Split a URL into prefix+suffix
     * (to be elaborated ...)
     * @param uri
     * @return 
     */
    public static String[] split(String uri) {
        int index = uri.lastIndexOf("#");
        if (index == -1) {
            index = uri.lastIndexOf("/");
        }

        String prefix = (index == -1) ? "" : uri.substring(0, index + 1);
        String suffix = (index == -1) ? uri : uri.substring(index + 1);
        return new String[]{prefix, suffix};
    }

    /**
     * Check if a string is null or empty
     * @param s
     * @return 
     */
    public static boolean empty(String s) {
        return s == null || s.length() == 0;
    }
}
