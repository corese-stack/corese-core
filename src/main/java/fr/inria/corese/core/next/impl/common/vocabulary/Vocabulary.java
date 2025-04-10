package fr.inria.corese.core.next.impl.common.vocabulary;

import fr.inria.corese.core.next.api.IRI;

/**
 * Common interface for the definition of enum-based vocabularies.
 */
public interface Vocabulary {

    IRI getIRI();

    String getNamespace();

    String getPreferredPrefix();
}
