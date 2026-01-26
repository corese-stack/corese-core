package fr.inria.corese.core.next.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFXMLParserW3CErrorTestViewer {
    private static final Logger logger = LoggerFactory.getLogger(RDFXMLParserTest.class);

    /**
     * Helper method to parse the RDF/XML String
     * @param rdfXml
     * @return model
     * @throws Exception
     */
    private Model parseRdfXml(String rdfXml) throws Exception {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();
        try (InputStream inputStream = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8))) {
            RDFXMLParser parser = new RDFXMLParser(model, valueFactory);
            parser.parse(inputStream);
        }
        return model;
    }

    /**
     * Helper method to print the model.
     * @param model
     */
    /**
     * Helper method to print the model.
     * @param model
     */
    private void printModel(Model model) {
        model.stream().forEach(stmt -> {
            Value obj = stmt.getObject();
            String subjectString = stmt.getSubject().stringValue();
            String predicateString = stmt.getPredicate().stringValue();

            if (obj instanceof Literal literal) {
                String label = String.valueOf(literal.getLabel());
                String languageTag = literal.getLanguage().orElse(null);

                if (languageTag != null) {
                    logger.debug("({}, {}, \"{}\"@{})",
                            subjectString,
                            predicateString,
                            label,
                            languageTag);
                } else {
                    logger.debug("({}, {}, \"{}\")",
                            subjectString,
                            predicateString,
                            label);
                }
            } else {
                logger.debug("({}, {}, {})",
                        subjectString,
                        predicateString,
                        obj.stringValue());
            }
        });
    }
    /**
     * Test datatypestest002 - A parser is not required to know well-formed datatyped literals
     * fr.inria.corese.w3c.rdf11xml.Rdf11XmlDynamicTest#rdf11XmlTests
     */
    @Test
    public void testdatatypestest002RDFXML() throws Exception {
        String rdfXml = """
                <?xml version="1.0"?>
                <rdf:RDF
                             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                             xmlns:eg="http://example.org/vocab#">
                <rdf:Description rdf:about="http://example.org/foo">
                <eg:bar rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">flargh</eg:bar>
                </rdf:Description>
                </rdf:RDF>
                """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
    }

    /**
     * rdf-containers-syntax-vs-schema-error002
     */
    @Test
    public void testrdfcontainerssyntaxvsschemaerror002()throws Exception {
        String rdfXml = """
                 <?xml version="1.0"?>
                <rdf:RDF
                             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                             xmlns:eg="http://example.org/vocab#">
                <rdf:li/>
                </rdf:RDF>
    """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
    }

    /**
     * rdfcontainerssyntaxvsschematest002
     */
    @Test
    public void testrdfcontainerssyntaxvsschematest002()throws Exception {
        String rdfXml = """
                 <?xml version="1.0"?>
                    <rdf:RDF
                             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                             xmlns:foo="http://foo.org/vocab#">
                        <foo:Bar>
                            <rdf:_1>_1</rdf:_1>
                            <rdf:li>1</rdf:li>
                            <rdf:_3>_3</rdf:_3>
                            <rdf:li>2</rdf:li>
                        </foo:Bar>
                    </rdf:RDF>
    """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
    }

    @Test
    public void testrdfelementnotmandatorytest001() throws Exception {
        String rdfXml = """
                    <Book>
                       <title>Dogs in Hats</title>
                    </Book>
    """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
    }

    @Test
    public void rdfmsabouteacherror001() throws Exception {
        String rdfXml = """
                 <?xml version="1.0"?>
                <rdf:RDF>
                    <rdf:Bag rdf:ID="node">
                        <rdf:li rdf:resource="http://example.org/node2"/>
                    </rdf:Bag>
                <rdf:Description rdf:aboutEach="#node">
                    <dc:rights>me</dc:rights>
                </rdf:Description>
                </rdf:RDF>
    """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
    }

    @Test
    public void rdfmsabouteacherror002() throws Exception {
        String rdfXml = """
                 <?xml version="1.0"?>
                    <rdf:RDF
                             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                             xmlns:foo="http://foo.org/vocab#">
                             xmlns:eg="http://eg.org/vocab#">
                             xmlns:dc="http://dc.org/vocab#">
                        <rdf:Description rdf:about="http://example.org/node">
                            <eg:property>foo</eg:property>
                        </rdf:Description>
                        <rdf:Description rdf:aboutEachPrefix="http://example.org/">
                            <dc:creator>me</dc:creator>
                        </rdf:Description>
                    </rdf:RDF>
    """.trim();

        Model model = parseRdfXml(rdfXml);
        printModel(model);
    }

}