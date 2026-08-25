package fr.inria.corese.core.next.query.api.io.serializer;

import java.util.Map;

/** XML output properties used by the SPARQL Results XML serializer. */
public interface XmlOutputOptions extends ResultIOOptions {

    /** Returns immutable XML output properties keyed by {@code OutputKeys} constants. */
    Map<String, String> xmlOutputProperties();
}
