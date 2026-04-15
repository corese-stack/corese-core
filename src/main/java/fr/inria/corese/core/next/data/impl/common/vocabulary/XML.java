package fr.inria.corese.core.next.data.impl.common.vocabulary;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.impl.common.BasicIRI;

public enum XML implements Vocabulary {
    ;
    private final IRI iri;

    XML(String localName) {
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
        return "http://www.w3.org/XML/1998/namespace#";
    }

    public static String getVocabularyPreferredPrefix() {
        return "xml";
    }
}
