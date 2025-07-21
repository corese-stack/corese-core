package fr.inria.corese.core.next.impl.common.vocabulary;

import fr.inria.corese.core.next.api.IRI;

/**
 * Common interface for the definition of enum-based vocabularies.
 */
public interface Vocabulary {

    /**
     * @return the IRI of this vocabulary element
     */
    IRI getIRI();

    /**
     * @return the namespace of this vocabulary
     */
    String getNamespace();

    /**
     * @return the preferred prefix of this vocabulary
     */
    String getPreferredPrefix();
}
