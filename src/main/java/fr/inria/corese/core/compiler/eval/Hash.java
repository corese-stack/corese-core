package fr.inria.corese.core.compiler.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Use cryptographic library from saphir2 project
 * <p>
 * http://www.saphir2.com/sphlib/
 */
public class Hash {
    private static final Logger logger = LoggerFactory.getLogger(Hash.class);

    String name;

    public Hash(String n) {
        name = n;
    }

    public String hash(String str) {

        byte[] uniqueKey = str.getBytes();
        byte[] hash = null;

        try {
            hash = MessageDigest.getInstance(name).digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            logger.error("No support in this VM: " + name);
            return null;
        }

        return toString(hash);
    }


    String toString(byte[] hash) {
        StringBuilder hashString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }

        return hashString.toString();
    }


}
