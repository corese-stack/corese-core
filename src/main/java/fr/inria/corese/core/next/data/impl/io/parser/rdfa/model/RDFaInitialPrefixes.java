package fr.inria.corese.core.next.data.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.vocabulary.*;

/**
 * <a href="https://www.w3.org/2011/rdfa-context/rdfa-1.1">https://www.w3.org/2011/rdfa-context/rdfa-1.1</a> sets a list of predefined prefixes for RDFa contexts.
 */
public enum RDFaInitialPrefixes implements Namespace {

    //        as "https://www.w3.org/ns/activitystreams#"
    AS("as", "https://www.w3.org/ns/activitystreams#"),
    //        cc	"http://creativecommons.org/ns#"
    CC("cc", "http://creativecommons.org/ns#"),
    //        csvw	"http://www.w3.org/ns/csvw#"
    CSVW("csvw", "http://www.w3.org/ns/csvw#"),
    //        ctag	"http://commontag.org/ns#"
    CTAG("ctag", "http://commontag.org/ns#"),
    //        dc	"http://purl.org/dc/terms/"
    DC("dc", "http://purl.org/dc/terms/"),
    //        dc11	"http://purl.org/dc/elements/1.1/"
    DC11("dc11", "http://purl.org/dc/elements/1.1/"),
    //        dcat	"http://www.w3.org/ns/dcat#"
    DCAT("dcat", "http://www.w3.org/ns/dcat#"),
    //        dcterms	"http://purl.org/dc/terms/"
    DCTERMS("dcterms", "http://purl.org/dc/terms/"),
    //        dqv	"http://www.w3.org/ns/dqv#"
    DQV("dqv", "http://www.w3.org/ns/dqv#"),
    //        duv	"https://www.w3.org/ns/duv#"
    DUV("duv", "https://www.w3.org/ns/duv#"),
    //        foaf	"http://xmlns.com/foaf/0.1/"
    FOAF(fr.inria.corese.core.next.data.api.vocabulary.FOAF.getVocabularyPreferredPrefix(), fr.inria.corese.core.next.data.api.vocabulary.FOAF.getVocabularyNamespace()),
    //        gr	"http://purl.org/goodrelations/v1#"
    GR("gr", "http://purl.org/goodrelations/v1#"),
    //        grddl	"http://www.w3.org/2003/g/data-view#"
    GRDDL("grddl", "http://www.w3.org/2003/g/data-view#"),
    //        ical	"http://www.w3.org/2002/12/cal/icaltzd#"
    ICAL("ical", "http://www.w3.org/2002/12/cal/icaltzd#"),
    //        jsonld	"http://www.w3.org/ns/json-ld#"
    JSONLD("jsonld", "http://www.w3.org/ns/json-ld#"),
    //        ldp	"http://www.w3.org/ns/ldp#"
    LDP("ldp", "http://www.w3.org/ns/ldp#"),
    //        ma	"http://www.w3.org/ns/ma-ont#"
    MA("ma", "http://www.w3.org/ns/ma-ont#"),
    //        oa	"http://www.w3.org/ns/oa#"
    OA("oa", "http://www.w3.org/ns/oa#"),
    //        odrl	"http://www.w3.org/ns/odrl/2/"
    ODRL("odrl", "http://www.w3.org/ns/odrl/2/"),
    //        og	"http://ogp.me/ns#"
    OG("og", "http://ogp.me/ns#"),
    //        org	"http://www.w3.org/ns/org#"
    ORG("org", "http://www.w3.org/ns/org#"),
    //        owl	"http://www.w3.org/2002/07/owl#"
    OWL(fr.inria.corese.core.next.data.api.vocabulary.OWL.getVocabularyPreferredPrefix(), fr.inria.corese.core.next.data.api.vocabulary.OWL.getVocabularyNamespace()),
    //        prov	"http://www.w3.org/ns/prov#"
    PROV("prov", "http://www.w3.org/ns/prov#"),
    //        qb	"http://purl.org/linked-data/cube#"
    QB("qb", "http://purl.org/linked-data/cube#"),
    //        rdf	"http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    RDF(fr.inria.corese.core.next.data.api.vocabulary.RDF.getVocabularyPreferredPrefix(), fr.inria.corese.core.next.data.api.vocabulary.RDF.getVocabularyNamespace()),
    //        rdfa	"http://www.w3.org/ns/rdfa#"
    RDFA(RDFa.getVocabularyPreferredPrefix(), RDFa.getVocabularyNamespace()),
    //        rdfs	"http://www.w3.org/2000/01/rdf-schema#"
    RDFS(fr.inria.corese.core.next.data.api.vocabulary.RDFS.getVocabularyPreferredPrefix(), fr.inria.corese.core.next.data.api.vocabulary.RDFS.getVocabularyNamespace()),
    //        rev	"http://purl.org/stuff/rev#"
    REV("rev", "http://purl.org/stuff/rev#"),
    //        rif	"http://www.w3.org/2007/rif#"
    RIF("rif", "http://www.w3.org/2007/rif#"),
    //        rr	"http://www.w3.org/ns/r2rml#"
    RR("rr", "http://www.w3.org/ns/r2rml#"),
    //        schema	"http://schema.org/"
    SCHEMA("schema", "http://schema.org/"),
    //        sd	"http://www.w3.org/ns/sparql-service-description#"
    SD("sd", "http://www.w3.org/ns/sparql-service-description#"),
    //        sioc	"http://rdfs.org/sioc/ns#"
    SIOC("sioc", "http://rdfs.org/sioc/ns#"),
    //        skos	"http://www.w3.org/2004/02/skos/core#"
    SKOS("skos", "http://www.w3.org/2004/02/skos/core#"),
    //        skosxl	"http://www.w3.org/2008/05/skos-xl#"
    SKOSXL("skosxl", "http://www.w3.org/2008/05/skos-xl#"),
    //        sosa	"http://www.w3.org/ns/sosa/"
    SOSA("sosa", "http://www.w3.org/ns/sosa/"),
    //        ssn	"http://www.w3.org/ns/ssn/"
    SSN("ssn", "http://www.w3.org/ns/ssn/"),
    //        time	"http://www.w3.org/2006/time#"
    TIME("time", "http://www.w3.org/2006/time#"),
    //        v	"http://rdf.data-vocabulary.org/#"
    V("v", "http://rdf.data-vocabulary.org/#"),
    //        vcard	"http://www.w3.org/2006/vcard/ns#"
    VCARD("vcard", "http://www.w3.org/2006/vcard/ns#"),
    //        void	"http://rdfs.org/ns/void#"
    VOID("void", "http://rdfs.org/ns/void#"),
    //        wdr	"http://www.w3.org/2007/05/powder#"
    WDR("wdr", "http://www.w3.org/2007/05/powder#"),
    //        wdrs	"http://www.w3.org/2007/05/powder-s#"
    WDRS("wdrs", "http://www.w3.org/2007/05/powder-s#"),
    //        xhv	"http://www.w3.org/1999/xhtml/vocab#"
    XHV("xhv", "http://www.w3.org/1999/xhtml/vocab#"),
    //        xml	"http://www.w3.org/XML/1998/namespace"
    XML("xml", "http://www.w3.org/XML/1998/namespace"),
    //        xsd	"http://www.w3.org/2001/XMLSchema#"
    XSD(fr.inria.corese.core.next.data.api.vocabulary.XSD.getVocabularyPreferredPrefix(), fr.inria.corese.core.next.data.api.vocabulary.XSD.getVocabularyNamespace()),
    ;

    private final String prefix;
    private final String namespace;

    RDFaInitialPrefixes(String prefix, String namespaceString) {
        this.namespace = namespaceString;
        this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }
}
