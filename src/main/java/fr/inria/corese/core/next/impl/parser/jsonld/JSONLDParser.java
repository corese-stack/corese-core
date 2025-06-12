package fr.inria.corese.core.next.impl.parser.jsonld;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.utils.JsonUtils;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.parser.RDFFormat;
import fr.inria.corese.core.next.api.parser.RDFFormats;
import fr.inria.corese.core.next.api.parser.RDFParser;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class JSONLDParser implements RDFParser {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JSONLDParser.class);

    private static final String jsonldjavadefaultGraphName = "@default";

    private final Model model;
    private final ValueFactory valueFactory;
    private JsonLdOptions commonOptions = new JsonLdOptions();

    public JSONLDParser(Model model, ValueFactory factory) {
        this.model = model;
        this.valueFactory = factory;
        commonOptions.setCompactArrays(true);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormats.JSON_LD;
    }

    @Override
    public void parse(InputStream in) throws ParsingErrorException {
        try {
            Object input = JsonUtils.fromInputStream(in);
            RDFDataset output = (RDFDataset)JsonLdProcessor.toRDF(input, this.commonOptions);
            logger.debug("JSON-LD output: {}", output);
            for(String gName: output.graphNames()) {
                logger.debug("Graph name: {}", gName);
                for(RDFDataset.Quad q : output.getQuads(gName)) {
                   logger.debug("Quad: {}", q);
                   Resource subject = null;
                   if (q.getSubject().isIRI()) {
                       subject = valueFactory.createIRI(q.getSubject().getValue());
                   } else {
                       subject = valueFactory.createBNode(q.getSubject().getValue());
                   }

                    IRI predicate = valueFactory.createIRI(q.getPredicate().getValue());

                   Value object = null;
                   if (q.getObject().isIRI()) {
                       object = valueFactory.createIRI(q.getObject().getValue());
                   } else if (q.getObject().isBlankNode()) {
                       object = valueFactory.createBNode(q.getObject().getValue());
                   } else {
                       object = valueFactory.createLiteral(q.getObject().getValue());
                   }

                   if(gName.equals("@default")) {
                       model.add(subject, predicate, object);
                   } else {
                       IRI graph = valueFactory.createIRI(gName);
                       model.add(subject, predicate, object, graph);
                   }
                }
            }
        } catch (IOException | JsonLdError e) {
            throw new ParsingErrorException(e);
        }
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {

    }

    @Override
    public void parse(Reader reader) throws ParsingErrorException {

    }

    @Override
    public void parse(Reader reader, String baseURI) {

    }
}
