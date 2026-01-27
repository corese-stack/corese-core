package fr.inria.corese.core.next.impl.common.vocabulary;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.impl.common.BasicIRI;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;

public enum RDFa implements Vocabulary {

    PGClass("PGClass"),
    Pattern("Pattern"),
    PrefixOrTermMapping("PrefixOrTermMapping"),
    DocumentError("DocumentError"),
    Info("Info"),
    PrefixRedefinition("PrefixRedefinition"),
    UnresolvedCURIE("UnresolvedCURIE"),
    UnresolvedTerm("UnresolvedTerm"),
    VocabReferenceError("VocabReferenceError"),
    context("context"),
    copy("copy"),
    prefix("prefix"),
    term("term"),
    uri("uri"),
    usesVocabulary("usesVocabulary"),
    vocabulary("vocabulary"),
    Error("Error"),
    PrefixMapping("PrefixMapping"),
    TermMapping("TermMapping"),
    Warning("Warning");

    private final IRI iri;

    /**
     * Constructor for the RDFa vocabulary enum.
     *
     * @param localName the local name of the IRI
     * @throws IncorrectFormatException if the namespace and the local name do not form a correct IRI
     */
    RDFa(String localName) {
        this.iri = new BasicIRI(getNamespace(), localName);
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
        return "http://www.w3.org/ns/rdfa#";
    }

    public static String getVocabularyPreferredPrefix() {
        return "rdfa";
    }
}
