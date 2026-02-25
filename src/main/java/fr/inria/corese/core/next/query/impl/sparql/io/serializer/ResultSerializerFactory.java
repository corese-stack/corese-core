package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.api.io.serializer.Serializer;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.IResultSerializerFactory;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.BooleanStringSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv.CSVSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.json.JSONBooleanSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.json.JSONSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv.TSVSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XMLBooleanSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XMLSerializer;

/**
 * Factory class for SPARQL results serializers
 */
public class ResultSerializerFactory implements IResultSerializerFactory {
    @Override
    public ResultSerializer createSerializer(ResultFormat format, TupleQueryResult results) {
        if(format == ResultFormat.CSV) {
            return new CSVSerializer(results);
        } else if(format == ResultFormat.TSV) {
            return new TSVSerializer(results);
        } else if (format == ResultFormat.JSON) {
            return new JSONSerializer(results);
        } else if(format == ResultFormat.XML) {
            return new XMLSerializer(results);
        }
        return null;
    }

    @Override
    public Serializer createBooleanSerializer(ResultFormat format, boolean results) {
        if(format == ResultFormat.CSV
            || format == ResultFormat.TSV
            || format == FileFormat.PLAIN_TEXT) {
            return new BooleanStringSerializer(results);
        } else if (format == ResultFormat.JSON) {
            return new JSONBooleanSerializer(results);
        } else if(format == ResultFormat.XML) {
            return new XMLBooleanSerializer(results);
        }
        return null;
    }

    @Override
    public ResultSerializer createSerializer(ResultFormat format, TupleQueryResult results, IOOptions options) {
        if(format == ResultFormat.CSV) {
            return new CSVSerializer(results, options);
        } else if(format == ResultFormat.TSV) {
            return new TSVSerializer(results, options);
        } else if (format == ResultFormat.JSON) {
            return new JSONSerializer(results, options);
        } else if (format == ResultFormat.XML) {
            return new XMLSerializer(results, options);
        }
        return null;
    }

    @Override
    public Serializer createBooleanSerializer(ResultFormat format, boolean results, IOOptions options) {
        if(format == ResultFormat.CSV
                || format == ResultFormat.TSV
                || format == FileFormat.PLAIN_TEXT) {
            return new BooleanStringSerializer(results);
        } else if (format == ResultFormat.JSON) {
            return new JSONBooleanSerializer(results, options);
        } else if (format == ResultFormat.XML) {
            return new XMLBooleanSerializer(results, options);
        }
        return null;
    }
}
