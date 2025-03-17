package fr.inria.corese.core.sparql.api;

/**
 * @author corby
 */
public interface ResultFormatDef {
    enum format {
        UNDEF_FORMAT,

        TEXT_FORMAT,

        RDF_XML_FORMAT,
        TURTLE_FORMAT,
        TRIG_FORMAT,
        JSONLD_FORMAT,
        NTRIPLES_FORMAT,
        NQUADS_FORMAT,
        RDFC10_FORMAT,
        RDFC10_SHA384_FORMAT,

        XML_FORMAT,
        RDF_FORMAT,
        JSON_FORMAT,
        CSV_FORMAT,
        TSV_FORMAT,
        MARKDOWN_FORMAT,

        HTML_FORMAT
    }

}
