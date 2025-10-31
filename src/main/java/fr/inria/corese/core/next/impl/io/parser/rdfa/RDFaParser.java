package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class RDFaParser extends AbstractRDFParser {
    protected RDFaParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    protected RDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.RDFa;
    }

    @Override
    public void parse(InputStream in, String baseURI) {
        try {
            Document document = Jsoup.parse(in, null, baseURI);

            document.stream().iterator()
        } catch (IOException e) {
            throw new ParsingErrorException("Error during parsing of HTML document", e);
        }


    }

    /**
     *
     * @param element
     * @param context
     * @param recursive Processing generally continues recursively through the entire tree of elements available. However, if an author indicates that some branch of the tree should be treated as an XML literal, no further processing should take place on that branch, and setting this flag to false would have that effect.
     * @param skipElement Flag thet indicates whether the [current element] can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     * @param newSubject A [new subject] value, which once calculated will set the [parent subject] property in an [evaluation context], as well as being used to complete any [incomplete triple]s
     */
    private void processElement(Element element, RDFaEvaluationContextHandler context, boolean recursive, boolean skipElement, Value newSubject) {


    }

    private void processElement(Element element, RDFaEvaluationContextHandler context) {
        processElement(element, context, true, false, this.)
    }

    @Override
    public void parse(Reader reader, String baseURI) {
    }
}
