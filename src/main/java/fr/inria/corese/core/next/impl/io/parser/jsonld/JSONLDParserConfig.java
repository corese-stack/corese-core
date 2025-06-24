package fr.inria.corese.core.next.impl.io.parser.jsonld;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.io.IOConfig;

public class JSONLDParserConfig implements IOConfig {

    private IRI baseIRI = null;

    public JSONLDParserConfig() {
        super();
    }

    public void setBaseIRI(IRI baseIRI) {
        this.baseIRI = baseIRI;
    }

    public IRI getBaseIRI() {
        return baseIRI;
    }
}
