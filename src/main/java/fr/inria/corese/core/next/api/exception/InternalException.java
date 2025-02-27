package fr.inria.corese.core.next.api.exception;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * Used for exception related to the inner workings of the system.
 */
public class InternalException extends CoreseException{
    public InternalException(Exception e) {
        super(e);
    }

    public InternalException(String s) {
        super(s);
    }
}
