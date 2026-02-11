package fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.data.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializer;

/**
 * CSV serializer for the CSV format of SPARQL results.
 * @see <a href="https://www.w3.org/TR/sparql11-results-csv-tsv/">CSV SPARQL result format recommendation</a>
 */
public class CSVSerializer extends CharacterSeparatedValuesSerializer {

    public CSVSerializer(TupleQueryResult results, IOOptions options) {
        super(SerializationConstants.COMMA, results, options);
    }

    public CSVSerializer(TupleQueryResult results) {
        this(results, new CSVSerializerOptions.Builder().build());
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.CSV;
    }

    /**
     * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-results-csv-tsv-20130321/#csv-terms">CSV SPARQL result format recommendation</a>
     */
    @Override
    protected String valuetoString(Value value) {
        if(value instanceof BNode bnodeValue) {
            return SerializationConstants.BLANK_NODE_PREFIX + bnodeValue.getID();
        }
        if(value.stringValue().contains(IOConstants.QUOTE)
            || value.stringValue().contains(IOConstants.COMMA)
            || value.stringValue().contains(IOConstants.CARRIAGE_RETURN)
            || value.stringValue().contains(IOConstants.LINE_FEED)) {
            if(value.stringValue().contains(IOConstants.QUOTE)) {
                return "\"" + value.stringValue().replaceAll("\"", "\"\"") + "\"";
            }
            return "\"" + value.stringValue() + "\"";
        }
        return value.stringValue();
    }
}
