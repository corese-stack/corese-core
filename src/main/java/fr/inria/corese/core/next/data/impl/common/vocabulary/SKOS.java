package fr.inria.corese.core.next.data.impl.common.vocabulary;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.impl.common.BasicIRI;

public enum SKOS implements Vocabulary {
    ;
    private final IRI iri;

    SKOS(String localName) {
        this.iri = new BasicIRI(getNamespace(), localName);
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
        return "http://www.w3.org/2004/02/skos/core#";
    }

    public static String getVocabularyPreferredPrefix() {
        return "skos";
    }
}
