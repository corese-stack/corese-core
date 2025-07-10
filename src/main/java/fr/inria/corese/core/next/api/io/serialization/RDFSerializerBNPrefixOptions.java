package fr.inria.corese.core.next.api.io.serialization;

public interface RDFSerializerBNPrefixOptions {

    /**
     * @return the prefix used for blank nodes ids in the serialization.
     */
    String getBlankNodePrefix();
}
