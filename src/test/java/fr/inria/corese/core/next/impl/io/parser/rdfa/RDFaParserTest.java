package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.io.serialization.DefaultSerializerFactory;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RDFaParserTest {

    private static final Logger logger = LoggerFactory.getLogger(RDFaParserTest.class);

    private static final ValueFactory factory = new CoreseAdaptedValueFactory();

    @Test
    public void basicDocTest() {
        String testDataString = """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:dc="http://purl.org/dc/elements/1.1/">
<head>
    <base href="http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/" />
	<title>Test 0001</title>
</head>
<body>
	<p>This photo was taken by <span class="author" about="photo1.jpg" property="dc:creator">Mark Birbeck</span>.</p>
</body>
</html>""";

        Model testModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFa, testModel, factory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()));

        IRI subject = factory.createIRI("http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/photo1.jpg");
        IRI predicate = factory.createIRI("http://purl.org/dc/elements/1.1/creator");
        Literal object = factory.createLiteral("Mark Birbeck");

        assertTrue(testModel.contains(subject, predicate, object));
        assertEquals(1, testModel.size());
    }

    @Test
    public void aboutTest() {
        String testDataString = """
                <html>
                  <head>
                  </head>
                  <body>
                    <p xmlns:foaf="http://xmlns.com/foaf/0.1/" about="http://w3id.org/people/pierre-maillot" typeof="foaf:Person">
                      Hello, I'm Pierre.
                    </p>
                  </body>
                </html>""";

        Model testModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFa, testModel, factory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI subject = factory.createIRI("http://w3id.org/people/pierre-maillot");
        IRI object = factory.createIRI("http://xmlns.com/foaf/0.1/Person");

        assertEquals(1, testModel.size());
        assertTrue(testModel.contains(subject, RDF.type.getIRI(), object));
    }

    @Test
    public void basicChainTest() {
        String testDataString = """
                <html>
                  <head>
                  <meta xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"></meta>
                  </head>
                  <body>
                <div about="http://dbpedia.org/resource/Albert_Einstein">
                  <span property="foaf:name">Albert Einstein</span>
                  <span property="dbp:dateOfBirth" datatype="xsd:date">1879-03-14</span>
                  <div rel="dbp:birthPlace" resource="http://dbpedia.org/resource/Germany" />
                  <span about="http://dbpedia.org/resource/Germany"
                    property="dbp:conventionalLongName">Federal Republic of Germany</span>
                </div>
                  </body>
                </html>
                """;

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFa, testModel, factory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI albertEinstein = factory.createIRI("http://dbpedia.org/resource/Albert_Einstein");
        IRI dateOfBirth = factory.createIRI("http://dbpedia.org/property/dateOfBirth");
        IRI foafName = factory.createIRI("http://xmlns.com/foaf/0.1/name");
        IRI birthPlace = factory.createIRI("http://dbpedia.org/property/birthPlace");
        IRI germany = factory.createIRI("http://dbpedia.org/resource/Germany");
        IRI conventionalLongName = factory.createIRI("http://dbpedia.org/property/conventionalLongName");
        Literal aeName = factory.createLiteral("Albert Einstein");
        Literal aeDateOfBirth = factory.createLiteral("1879-03-14", XSD.xsdDate.getIRI());
        Literal gerLongName = factory.createLiteral("Federal Republic of Germany");

        Statement aeNameStatement = factory.createStatement(albertEinstein, foafName, aeName);
        Statement aeDateOfBirthStatement = factory.createStatement(albertEinstein, dateOfBirth, aeDateOfBirth);
        Statement aeBirthPlaceStatement = factory.createStatement(albertEinstein, birthPlace, germany);
        Statement germanyNameStatement = factory.createStatement(germany, conventionalLongName, gerLongName);

        referenceModel.add(aeNameStatement);
        referenceModel.add(aeDateOfBirthStatement);
        referenceModel.add(aeBirthPlaceStatement);
        referenceModel.add(germanyNameStatement);

        DefaultSerializerFactory serializerFactory = new DefaultSerializerFactory();
        RDFSerializer serializer = serializerFactory.createSerializer(RDFFormat.NTRIPLES, testModel, new NTriplesSerializerOptions.Builder().build());
        StringWriter debugWriter = new StringWriter();
        serializer.write(debugWriter);
        logger.debug(debugWriter.toString());

        assertEquals(4, testModel.size());
        assertEquals(referenceModel, testModel);

    }
}
