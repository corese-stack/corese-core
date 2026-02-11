package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv.CSVSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv.TSVSerializer;

public class ResultSerializerFactory implements fr.inria.corese.core.next.query.api.io.serializer.ResultSerializerFactory {
    @Override
    public ResultSerializer createSerializer(ResultFormat format, TupleQueryResult results) {
        if(format == ResultFormat.CSV) {
            return new CSVSerializer(results);
        } else if(format == ResultFormat.TSV) {
            return new TSVSerializer(results);
        }
        return null;
    }

    @Override
    public ResultSerializer createSerializer(ResultFormat format, TupleQueryResult results, IOOptions options) {
        if(format == ResultFormat.CSV) {
            return new CSVSerializer(results, options);
        } else if(format == ResultFormat.TSV) {
            return new TSVSerializer(results, options);
        }
        return null;
    }
}
