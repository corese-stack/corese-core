package fr.inria.corese.core.next.impl.parser.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.parser.RDFFormat;
import fr.inria.corese.core.next.api.parser.RDFFormats;
import fr.inria.corese.core.next.api.parser.RDFParser;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;

import java.io.InputStream;
import java.io.Reader;

public class JSONLDParser implements RDFParser {

    private Model model;
    private final ValueFactory valueFactory;

    public JSONLDParser(Model model, ValueFactory factory) {
        this.model = model;
        this.valueFactory = factory;
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormats.JSON_LD;
    }

    @Override
    public void parse(InputStream in) throws ParsingErrorException {
        try {
            JsonDocument document = JsonDocument.of(in);
            JsonLd.toRdf(document).provide(new FromJsonLDConsumer(this.model, this.valueFactory));
        } catch (JsonLdError e) {
            throw  new ParsingErrorException(e);
        }
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        try {
            JsonDocument document = JsonDocument.of(in);
            JsonLd.toRdf(document).provide(new FromJsonLDConsumer(this.model, this.valueFactory));
        } catch (JsonLdError e) {
            throw  new ParsingErrorException(e);
        }
    }

    @Override
    public void parse(Reader reader) throws ParsingErrorException {
        try {
            JsonDocument document = JsonDocument.of(reader);
            JsonLd.toRdf(document).provide(new FromJsonLDConsumer(this.model, this.valueFactory));
        } catch (JsonLdError e) {
            throw  new ParsingErrorException(e);
        }
    }

    @Override
    public void parse(Reader reader, String baseURI) {
        try {
            JsonDocument document = JsonDocument.of(reader);
            JsonLd.toRdf(document).provide(new FromJsonLDConsumer(this.model, this.valueFactory));
        } catch (JsonLdError e) {
            throw  new ParsingErrorException(e);
        }
    }
}
