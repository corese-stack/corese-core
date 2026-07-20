package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializerFactory;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.BooleanStringSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv.CsvTupleResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.json.JsonBooleanResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.json.JsonTupleResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv.TsvTupleResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XmlBooleanResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XmlTupleResultSerializer;

/**
 * Default factory for the SPARQL result serializers supplied by Corese.
 */
public final class DefaultResultSerializerFactory implements ResultSerializerFactory {
    @Override
    public ResultSerializer createTupleSerializer(ResultFormat format, TupleQueryResult results) {
        if(format == ResultFormat.CSV) {
            return new CsvTupleResultSerializer(results);
        } else if(format == ResultFormat.TSV) {
            return new TsvTupleResultSerializer(results);
        } else if (format == ResultFormat.JSON) {
            return new JsonTupleResultSerializer(results);
        } else if(format == ResultFormat.XML) {
            return new XmlTupleResultSerializer(results);
        }
        throw unsupported(format);
    }

    @Override
    public BooleanResultSerializer createBooleanSerializer(ResultFormat format, boolean results) {
        if(format == ResultFormat.CSV
            || format == ResultFormat.TSV) {
            return new BooleanStringSerializer(results, format);
        } else if (format == ResultFormat.JSON) {
            return new JsonBooleanResultSerializer(results);
        } else if(format == ResultFormat.XML) {
            return new XmlBooleanResultSerializer(results);
        }
        throw unsupported(format);
    }

    @Override
    public ResultSerializer createTupleSerializer(ResultFormat format, TupleQueryResult results, IOOptions options) {
        if(format == ResultFormat.CSV) {
            return new CsvTupleResultSerializer(results, options);
        } else if(format == ResultFormat.TSV) {
            return new TsvTupleResultSerializer(results, options);
        } else if (format == ResultFormat.JSON) {
            return new JsonTupleResultSerializer(results, options);
        } else if (format == ResultFormat.XML) {
            return new XmlTupleResultSerializer(results, options);
        }
        throw unsupported(format);
    }

    @Override
    public BooleanResultSerializer createBooleanSerializer(ResultFormat format, boolean results, IOOptions options) {
        if(format == ResultFormat.CSV
                || format == ResultFormat.TSV) {
            return new BooleanStringSerializer(results, format);
        } else if (format == ResultFormat.JSON) {
            return new JsonBooleanResultSerializer(results, options);
        } else if (format == ResultFormat.XML) {
            return new XmlBooleanResultSerializer(results, options);
        }
        throw unsupported(format);
    }

    private static IllegalArgumentException unsupported(ResultFormat format) {
        return new IllegalArgumentException("Unsupported SPARQL result format: " + format);
    }
}
