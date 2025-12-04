package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaEvaluationContext;

import java.util.Optional;

public abstract class AbstractRDFaParser extends AbstractRDFParser {

    protected RDFaEvaluationContext currentContext;

    protected AbstractRDFaParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    protected AbstractRDFaParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    /**
     * Resolves the string representation of a resource found in attributes of an element, be it an IRI, <ahref="https://www.w3.org/TR/rdfa-syntax/#s_curieprocessing">CURIE</a> or relative URI
     *
     * @param stringResource the resource as stored in the attribute of the HTML element
     * @param context        the context of the element evalation
     * @return the full IRI if it is a relative IRI, full IRI or CURIE, nothing otherwise
     */
    protected Optional<Resource> resolveStringResource(String stringResource, RDFaEvaluationContext context) {
        String resultString = stringResource;
        if (resultString.startsWith("[") && resultString.endsWith("]")) {
            resultString = resultString.replaceFirst("\\[", "");
            resultString = resultString.replaceFirst("]", "");
        }


        if (stringUriIsCURIE(resultString)) { // CURIE
            int colonIndex = resultString.indexOf(":");
            String prefixString = resultString.substring(0, colonIndex);
            String localNameString = resultString.substring(colonIndex + 1);
            // Basic resolution following https://www.w3.org/TR/rdfa-syntax/#s_convertingcurietouri
            if (context.hasIriMapping(prefixString)) {
                IRI namespaceIRI = context.getIriMapping(prefixString);

                return Optional.of(this.getValueFactory().createIRI(namespaceIRI.stringValue(), localNameString));
            } else if (prefixString.isEmpty()) { // CURIE is relative to the base URI
                return Optional.of(this.getValueFactory().createIRI(context.getBaseIri().stringValue(), localNameString));
            } else {
                throw new ParsingErrorException("CURIE " + stringResource + " uses unknown prefix");
            }
        } else if (IRIUtils.isStandardIRI(resultString)) {  // Full IRI
            return Optional.of(this.getValueFactory().createIRI(resultString));

        } else if (resultString.startsWith("_:")) {  // Blank Node
            int colonIndex = resultString.indexOf(":");
            String localNameString = resultString.substring(colonIndex + 1);
            return Optional.of(this.getValueFactory().createBNode(localNameString));
        } else if (IRIUtils.isStandardIRI(context.getBaseIri().stringValue() + resultString)) {
            String concatenatedRelativeUri = context.getBaseIri().stringValue() + resultString;
            return Optional.of(getValueFactory().createIRI(concatenatedRelativeUri));
        }
        return Optional.empty();
    }

    /**
     * Equivalent to test if it has a colon, and it is not a blank node
     *
     * @param stringIri
     * @return
     */
    protected boolean stringUriIsCURIE(String stringIri) {
        int colonIndex = stringIri.indexOf(":");
        return colonIndex > -1 && !stringIri.contains("://") && !stringIri.startsWith("_:") && !stringIri.startsWith("[_:");
    }
}
