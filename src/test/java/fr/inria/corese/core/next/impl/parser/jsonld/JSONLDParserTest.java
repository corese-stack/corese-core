package fr.inria.corese.core.next.impl.parser.jsonld;

import fr.inria.corese.core.next.api.parser.RDFFormats;
import fr.inria.corese.core.next.api.parser.RDFParser;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JSONLDParserTest {
    @Test
    void testGetRDFFormat() {
        RDFParser parser = JSONLDParserFactory.getInstance().createRDFParser(RDFFormats.JSON_LD, new CoreseModel(), new CoreseAdaptedValueFactory());
        assertEquals(RDFFormats.JSON_LD, parser.getRDFFormat());
    }

    @Test
    void testParseInputStream() {
        String sampleJsonLD = "{\n" +
                "  \"@context\": {\n" +
                "    \"name\": \"http://xmlns.com/foaf/0.1/name\",\n" +
                "    \"knows\": \"http://xmlns.com/foaf/0.1/knows\"\n" +
                "  },\n" +
                "  \"@id\": \"http://me.markus-lanthaler.com/\",\n" +
                "  \"name\": \"Markus Lanthaler\",\n" +
                "  \"knows\": [\n" +
                "    {\n" +
                "      \"name\": \"Dave Longley\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        CoreseModel model = new CoreseModel();
        RDFParser parser = JSONLDParserFactory.getInstance().createRDFParser(RDFFormats.JSON_LD, model, new CoreseAdaptedValueFactory());
        parser.parse(new ByteArrayInputStream(sampleJsonLD.getBytes()));

        assertNotEquals(0, model.size());
    }

    @Test
    void testParseInputStreamString() {
    }

    @Test
    void testParseReader() {
    }

    @Test
    void testParseReaderString() {
    }
}
