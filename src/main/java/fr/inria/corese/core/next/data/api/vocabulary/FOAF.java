package fr.inria.corese.core.next.data.api.vocabulary;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;

/**
 * Defines the FOAF (Friend of a Friend) vocabulary.
 */
@SuppressWarnings("java:S115")
public enum FOAF implements Vocabulary {
    Person("Person"),
    Agent("Agent"),
    Group("Group"),
    Organization("Organization"),
    name("name"),
    knows("knows"),
    mbox("mbox"),
    homepage("homepage");

    public static final String NAMESPACE = "http://xmlns.com/foaf/0.1/";
    public static final String PREFERRED_PREFIX = "foaf";

    private final IRI iri;

    FOAF(String localName) {
        this.iri = new SimpleIRI(getNamespace(), localName);
    }

    @Override
    public IRI getIRI() {
        return this.iri;
    }

    @Override
    public String getNamespace() {
        return getVocabularyNamespace();
    }

    @Override
    public String getPreferredPrefix() {
        return getVocabularyPreferredPrefix();
    }

    public static String getVocabularyNamespace() {
        return NAMESPACE;
    }

    public static String getVocabularyPreferredPrefix() {
        return PREFERRED_PREFIX;
    }
}
