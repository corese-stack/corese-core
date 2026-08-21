package fr.inria.corese.core.next.data.api.vocabulary;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;

/**
 * Vocabulary for RDF Schema (RDFS).
 */
@SuppressWarnings("java:S115")
public enum RDFS implements Vocabulary {
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#Resource">RDFS Resource</a>
     */
    Resource("Resource"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#Class">RDFS Class</a>
     */
    Class("Class"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#Literal">RDFS Literal</a>
     */
    Literal("Literal"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#Datatype">RDFS Datatype</a>
     */
    Property("Property"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#Datatype">RDFS Datatype</a>
     */
    Datatype("Datatype"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#Container">RDFS Container</a>
     */
    Container("Container"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#ContainerMembershipProperty">RDFS ContainerMembershipProperty</a>
     */
    ContainerMembershipProperty("ContainerMembershipProperty"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#ContainerMembershipProperty">RDFS ContainerMembershipProperty</a>
     */
    subClassOf("subClassOf"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#subPropertyOf">RDFS subPropertyOf</a>
     */
    subPropertyOf("subPropertyOf"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#domain">RDFS domain</a>
     */
    domain("domain"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#range">RDFS range</a>
     */
    range("range"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#member">RDFS member</a>
     */
    label("label"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#comment">RDFS comment</a>
     */
    comment("comment"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#member">RDFS member</a>
     */
    member("member"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#seeAlso">RDFS seeAlso</a>
     */
    seeAlso("seeAlso"),
    /**
     * @see <a href="http://www.w3.org/2000/01/rdf-schema#isDefinedBy">RDFS isDefinedBy</a>
     */
    isDefinedBy("isDefinedBy")
    ;

    public static final String NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String PREFERRED_PREFIX = "rdfs";

    private final IRI iri;
    /**
     * Constructor for the RDFS vocabulary enum.
     *
     * @param localName the local name of the IRI
     * @throws IncorrectFormatException if the namespace and the local name do not form a correct IRI
     */
    RDFS(String localName) {
        this.iri = new SimpleIRI(getNamespace(), localName);
    }

    @Override
    public String getNamespace() {
        return getVocabularyNamespace();
    }

    @Override
    public IRI getIRI() {
        return this.iri;
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
