package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.common.literal.RDF;
import fr.inria.corese.core.next.data.impl.common.literal.XSD;
import fr.inria.corese.core.next.data.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializer;

import java.util.List;

/**
 * TSV serializer for the CSV format of SPARQL results.
 * @see <a href="https://www.w3.org/TR/sparql11-results-csv-tsv/">TSV SPARQL result format recommendation</a>
 */
public class TSVSerializer extends CharacterSeparatedValuesSerializer {

    public TSVSerializer(TupleQueryResult results, IOOptions options) {
        super(SerializationConstants.TAB, results, options);
    }

    public TSVSerializer(TupleQueryResult results) {
        this(results, new TSVSerializerOptions.Builder().build());
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
        if(value instanceof IRI iriValue) {
            return SerializationConstants.IRI_START + iriValue.stringValue() + SerializationConstants.IRI_END;
        }
        if(value instanceof BNode bnodeValue) {
            return SerializationConstants.BLANK_NODE_PREFIX + bnodeValue.getID();
        }
        if(value instanceof Literal literalValue) {
            String delimiter = SerializationConstants.QUOTE;
            String stringValue = literalValue.stringValue();
            stringValue = stringValue.replace("\"", "\\\"");
            if(literalValue.getLanguage().isPresent()) {
                return delimiter + stringValue + delimiter + SerializationConstants.AT + literalValue.getLanguage().get();
            } else if(literalValue.getDatatype() != null
                    && literalValue.getDatatype() != XSD.STRING.getIRI()
                    && literalValue.getDatatype() != RDF.LANGSTRING.getIRI()) {
                if(literalValue.getCoreDatatype() == XSD.UNSIGNED_INT
                        || literalValue.getCoreDatatype() == XSD.POSITIVE_INTEGER
                        || literalValue.getCoreDatatype() == XSD.NEGATIVE_INTEGER
                        || literalValue.getCoreDatatype() == XSD.NON_NEGATIVE_INTEGER
                        || literalValue.getCoreDatatype() == XSD.NON_POSITIVE_INTEGER
                        || literalValue.getCoreDatatype() == XSD.INT
                        || literalValue.getCoreDatatype() == XSD.INTEGER) {
                    return String.valueOf(literalValue.integerValue());
                }
                if(literalValue.getCoreDatatype() == XSD.DOUBLE) {
                    return String.valueOf(literalValue.doubleValue());
                }
                if(literalValue.getCoreDatatype() == XSD.DECIMAL) {
                    return String.valueOf(literalValue.decimalValue());
                }
                return delimiter + stringValue + delimiter + SerializationConstants.DATATYPE_SEPARATOR + SerializationConstants.IRI_START + literalValue.getDatatype().stringValue() + SerializationConstants.IRI_END;
            }
            return delimiter + stringValue + delimiter;
        }
        return value.stringValue();
    }

    @Override
    protected String headerString() {
        List<String> variableList = this.getResults().getBindingNames().stream().map(variableName -> "?" + variableName).toList();
        return String.join(SerializationConstants.TAB, variableList);
    }
}
