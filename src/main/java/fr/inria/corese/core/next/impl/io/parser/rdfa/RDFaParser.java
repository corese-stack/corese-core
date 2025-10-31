package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class RDFaParser extends AbstractRDFParser {
    protected RDFaParser(Model model, ValueFactory factory) {
        this(model, factory, new RDFaParserOptions.Builder().build());
    }

    protected RDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFa;
    }

    @Override
    public void parse(InputStream in, String baseURIString) {
        try {
            Document document = Jsoup.parse(in, null, baseURIString);

            IRI baseIri = getValueFactory().createIRI(baseURIString);
            processDocument(document, baseIri);
        } catch (Exception e) {
            throw new ParsingErrorException("Error during parsing of HTML document", e);
        }
    }

    /**
     * Intermediary function to configure the processing of a document using some basic HTML traversal to determine if a baseIri has been defined in the document.
     * If the baseIri in argument is the Corese default base IRI, the value stored in the document is used instead.
     * @param document Jsoup HTML document to be processed
     * @param baseIri An IRI object
     */
    private void processDocument(Document document, IRI baseIri) {



        // If the base Iri in argument is not the default baseIri, then we take it, else we use the one in the document
        if(baseIri.stringValue().equals(ParserConstants.getDefaultBaseURI())) {
            // Looking for the <base> node in the document
            IRI baseIriFromXml = baseIri;
            Iterator<Element> baseElementIterator = document.stream().filter(element -> element.nameIs("base")).iterator();
            while(baseElementIterator.hasNext()) {
                Element baseElement = baseElementIterator.next();
                Attribute baseElementHrefAttribute = baseElement.attribute("href");
                if(baseElementHrefAttribute != null) {
                    String baseIriString = baseElementHrefAttribute.getValue();
                    baseIriFromXml = getValueFactory().createIRI(baseIriString);
                }
            };

            baseIri = this.getValueFactory().createIRI(baseIriFromXml.stringValue());
        }

        Iterator<Element> elementIt = document.stream().iterator();
        while (elementIt.hasNext()) {
            Element element = elementIt.next();
            processElement(element, new RDFaEvaluationContextHandler(baseIri), baseIri);
        }
    }

    /**
     *
     * @param element Current element
     * @param context Active context
     * @param recursive Processing generally continues recursively through the entire tree of elements available. However, if an author indicates that some branch of the tree should be treated as an XML literal, no further processing should take place on that branch, and setting this flag to false would have that effect.
     * @param skipElement Flag thet indicates whether the [current element] can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     * @param newSubject A [new subject] value, which once calculated will set the [parent subject] property in an [evaluation context], as well as being used to complete any [incomplete triple]s
     * @see <a href="https://www.w3.org/TR/rdfa-syntax/#s_rdfaindetail">RDFa processing in details<a/>
     */
    private void processElement(Element element, RDFaEvaluationContextHandler context, boolean recursive, boolean skipElement, Resource newSubject, Value currentObject) {


    }

    /**
     * Surcharge function that initialize the flags and subject and objet to their initial values for processing
     * @param element
     * @param context
     * @param newSubject
     */
    private void processElement(Element element, RDFaEvaluationContextHandler context, Resource newSubject) {
        processElement(element, context, true, false, newSubject, null);
    }

    @Override
    public void parse(Reader reader, String baseURI) {
    }

    private Statement incompleteStatementToStatement(RDFaIncompleteStatement incompleteStatement) {
        Objects.requireNonNull(incompleteStatement.getSubject(), "Null subject, IncompleteStatement can only be converted if all its component are non-null.");
        Objects.requireNonNull(incompleteStatement.getPredicate(), "Null predicate, IncompleteStatement can only be converted if all its component are non-null.");
        Objects.requireNonNull(incompleteStatement.getObject(), "Null object, IncompleteStatement can only be converted if all its component are non-null.");

        return this.getValueFactory().createStatement(incompleteStatement.getSubject(), incompleteStatement.getPredicate(), incompleteStatement.getObject());
    }
}
