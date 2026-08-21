package fr.inria.corese.core.next.data.api.vocabulary;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;

public enum FOAF implements Vocabulary {
    ;

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
        return getVocabularyNamespace(); // Referencing the directly defined static NS
    }

    @Override
    public String getPreferredPrefix() {
        return getVocabularyPreferredPrefix();
    }

    public static String getVocabularyNamespace() {
        return "http://xmlns.com/foaf/0.1/";
    }

    public static String getVocabularyPreferredPrefix() {
        return "foaf";
    }
}
