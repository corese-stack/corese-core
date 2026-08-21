package fr.inria.corese.core.next.data.api.vocabulary;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;

/**
 * Defines the OWL (Web Ontology Language) vocabulary.
 */
public enum OWL implements Vocabulary {
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#Class">OWL Class</a>
     */
    Class("Class"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#Ontology">OWL Ontology</a>
     */
    Ontology("Ontology"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#ObjectProperty">OWL ObjectProperty</a>
     */
    ObjectProperty("ObjectProperty"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#DatatypeProperty">OWL DatatypeProperty</a>
     */
    DatatypeProperty("DatatypeProperty"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#AnnotationProperty">OWL AnnotationProperty</a>
     */
    AnnotationProperty("AnnotationProperty"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#NamedIndividual">OWL NamedIndividual</a>
     */
    NamedIndividual("NamedIndividual"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#Restriction">OWL Restriction</a>
     */
    Restriction("Restriction"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#equivalentClass">OWL equivalentClass</a>
     */
    equivalentClass("equivalentClass"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#sameAs">OWL sameAs</a>
     */
    sameAs("sameAs"),
    /**
     * @see <a href="http://www.w3.org/2002/07/owl#differentFrom">OWL differentFrom</a>
     */
    differentFrom("differentFrom")
    ;

    private final IRI iri;

    OWL(String localName) {
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
        return "http://www.w3.org/2002/07/owl#";
    }

    public static String getVocabularyPreferredPrefix() {
        return "owl";
    }
}
