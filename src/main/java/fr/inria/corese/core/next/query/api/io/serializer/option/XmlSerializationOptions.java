package fr.inria.corese.core.next.query.api.io.serializer.option;

import java.util.Map;

/**
 * Advanced XML document properties used by the SPARQL Results XML serializer.
 * These properties affect the lexical XML output, not the SPARQL result data.
 */
public interface XmlSerializationOptions extends ResultSerializationOptions {

    /**
     * Returns immutable XML output properties keyed by {@link
     * javax.xml.transform.OutputKeys} constants.
     */
    Map<String, String> xmlOutputProperties();
}
