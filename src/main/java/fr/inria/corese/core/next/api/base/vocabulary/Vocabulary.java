<<<<<<<< HEAD:src/main/java/fr/inria/corese/core/next/impl/common/vocabulary/Vocabulary.java
package fr.inria.corese.core.next.impl.common.vocabulary;
========
package fr.inria.corese.core.next.api.base.vocabulary;
>>>>>>>> feature/temporal_literal:src/main/java/fr/inria/corese/core/next/api/base/vocabulary/Vocabulary.java

import fr.inria.corese.core.next.api.IRI;

/**
 * Common interface for the definition of enum-based vocabularies.
 */
public interface Vocabulary {

    IRI getIRI();

    String getNamespace();

    String getPreferredPrefix();
}
