package fr.inria.corese.core.next.api.io.serialization;

/**
 * Options for RDF parsers that support adding a prefix to blank nodes ids.
 */
public interface RDFSerializerBNPrefixOptions {

    /**
     * @return the prefix used for blank nodes ids in the serialization.
     */
    String getBlankNodePrefix();
}
