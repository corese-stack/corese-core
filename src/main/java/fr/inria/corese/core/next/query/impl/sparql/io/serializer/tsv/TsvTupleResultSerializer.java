package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.support.io.IOConstants;
import fr.inria.corese.core.next.query.api.io.format.ResultFormat;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.CharacterSeparatedValuesSerializer;

import java.util.List;

/**
 * TSV serializer for the CSV format of SPARQL results.
 * @see <a href="https://www.w3.org/TR/sparql11-results-csv-tsv/">TSV SPARQL result format recommendation</a>
 */
public class TsvTupleResultSerializer extends CharacterSeparatedValuesSerializer {

    public TsvTupleResultSerializer(TupleQueryResult results, IOOptions options) {
        super(IOConstants.TAB, results, options);
    }

    public TsvTupleResultSerializer(TupleQueryResult results) {
        this(results, new TsvResultSerializerOptions.Builder().build());
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.TSV;
    }

    /**
     * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-results-csv-tsv-20130321/#tsv-terms">TSV result recommandation</a>
     */
    @Override
    protected String valuetoString(Value value) {
        if (value instanceof IRI iriValue) {
            return IOConstants.IRI_START + iriValue.stringValue() + IOConstants.IRI_END;
        }
        if (value instanceof BNode bnodeValue) {
            return IOConstants.BLANK_NODE_PREFIX + bnodeValue.getID();
        }
        if (value instanceof Literal literalValue) {
            return formatLiteral(literalValue);
        }
        return value.stringValue();
    }

    private String formatLiteral(Literal literalValue) {
        String lang = literalValue.getLanguage().orElse(null);
        String stringValue = literalValue.stringValue().replace("\"", "\\\"");

        if (lang != null) {
            return IOConstants.QUOTE + stringValue + IOConstants.QUOTE + IOConstants.AT + lang;
        }

        if (isTypedNonString(literalValue)) {
            return formatTypedLiteral(literalValue, stringValue);
        }

        return IOConstants.QUOTE + stringValue + IOConstants.QUOTE;
    }

    private boolean isTypedNonString(Literal literalValue) {
        return literalValue.getDatatype() != null
                && !literalValue.getDatatype().equals(XSDDatatype.STRING.getIRI())
                && !literalValue.getDatatype().equals(RDFDatatype.LANGSTRING.getIRI());
    }

    private String formatTypedLiteral(Literal literalValue, String stringValue) {
        if (isIntegerDatatype(literalValue)) {
            return String.valueOf(literalValue.integerValue());
        }
        if (literalValue.getCoreDatatype() == XSDDatatype.DOUBLE) {
            return String.valueOf(literalValue.doubleValue());
        }
        if (literalValue.getCoreDatatype() == XSDDatatype.DECIMAL) {
            return String.valueOf(literalValue.decimalValue());
        }
        return IOConstants.QUOTE + stringValue + IOConstants.QUOTE
                + IOConstants.DATATYPE_SEPARATOR + IOConstants.IRI_START
                + literalValue.getDatatype().stringValue() + IOConstants.IRI_END;
    }

    private boolean isIntegerDatatype(Literal literalValue) {
        return literalValue.getCoreDatatype() == XSDDatatype.UNSIGNED_INT
                || literalValue.getCoreDatatype() == XSDDatatype.POSITIVE_INTEGER
                || literalValue.getCoreDatatype() == XSDDatatype.NEGATIVE_INTEGER
                || literalValue.getCoreDatatype() == XSDDatatype.NON_NEGATIVE_INTEGER
                || literalValue.getCoreDatatype() == XSDDatatype.NON_POSITIVE_INTEGER
                || literalValue.getCoreDatatype() == XSDDatatype.INT
                || literalValue.getCoreDatatype() == XSDDatatype.INTEGER;
    }

    @Override
    protected String headerString() {
        List<String> variableList = this.getResults().getBindingNames().stream().map(variableName -> "?" + variableName).toList();
        return String.join(IOConstants.TAB, variableList);
    }
}
