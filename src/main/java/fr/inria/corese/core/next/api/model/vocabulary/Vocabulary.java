package fr.inria.corese.core.next.api.model.vocabulary;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;

/**
 * Common interface for the definition of enum-based vocabularies.
 */
public interface Vocabulary extends CoreDatatype {

    String getNamespace();
}
