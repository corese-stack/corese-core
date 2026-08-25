package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.data.api.io.serializer.option.LineEndingOptions;
import fr.inria.corese.core.next.data.api.support.io.IOConstants;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultIOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializerFactory;
import fr.inria.corese.core.next.query.api.io.serializer.XmlOutputOptions;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv.CsvTupleResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.json.JsonBooleanResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.json.JsonTupleResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.BooleanStringSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv.TsvTupleResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XmlBooleanResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml.XmlTupleResultSerializer;

import java.util.Objects;

/** Default factory for the SPARQL result serializers supplied by Corese. */
public final class DefaultResultSerializerFactory implements ResultSerializerFactory {

    @Override
    public ResultSerializer createTupleSerializer(ResultFormat format, TupleQueryResult results) {
        Objects.requireNonNull(results, "results");
        ResultFormat standard = standardFormat(format);
        if (standard.equals(ResultFormat.CSV)) {
            return new CsvTupleResultSerializer(results);
        }
        if (standard.equals(ResultFormat.TSV)) {
            return new TsvTupleResultSerializer(results);
        }
        if (standard.equals(ResultFormat.JSON)) {
            return new JsonTupleResultSerializer(results);
        }
        return new XmlTupleResultSerializer(results);
    }

    @Override
    public BooleanResultSerializer createBooleanSerializer(ResultFormat format, boolean result) {
        ResultFormat standard = standardFormat(format);
        if (standard.equals(ResultFormat.CSV) || standard.equals(ResultFormat.TSV)) {
            return new BooleanStringSerializer(result, standard);
        }
        if (standard.equals(ResultFormat.JSON)) {
            return new JsonBooleanResultSerializer(result);
        }
        return new XmlBooleanResultSerializer(result);
    }

    @Override
    public ResultSerializer createTupleSerializer(
            ResultFormat format,
            TupleQueryResult results,
            ResultIOOptions options) {
        Objects.requireNonNull(results, "results");
        ResultFormat standard = standardFormat(format);
        validateOptions(standard, options, false);
        if (standard.equals(ResultFormat.CSV)) {
            return new CsvTupleResultSerializer(results, options);
        }
        if (standard.equals(ResultFormat.TSV)) {
            return new TsvTupleResultSerializer(results, options);
        }
        if (standard.equals(ResultFormat.JSON)) {
            return new JsonTupleResultSerializer(results, options);
        }
        return new XmlTupleResultSerializer(results, options);
    }

    @Override
    public BooleanResultSerializer createBooleanSerializer(
            ResultFormat format,
            boolean result,
            ResultIOOptions options) {
        ResultFormat standard = standardFormat(format);
        validateOptions(standard, options, true);
        if (standard.equals(ResultFormat.CSV) || standard.equals(ResultFormat.TSV)) {
            return new BooleanStringSerializer(result, standard);
        }
        if (standard.equals(ResultFormat.JSON)) {
            return new JsonBooleanResultSerializer(result, options);
        }
        return new XmlBooleanResultSerializer(result, options);
    }

    private static ResultFormat standardFormat(ResultFormat format) {
        Objects.requireNonNull(format, "format");
        return ResultFormat.all().stream()
                .filter(format::equals)
                .findFirst()
                .orElseThrow(() -> unsupported(format));
    }

    private static void validateOptions(
            ResultFormat format,
            ResultIOOptions options,
            boolean booleanResult) {
        Objects.requireNonNull(options, "options");
        if ((format.equals(ResultFormat.CSV) || format.equals(ResultFormat.TSV))
                && options instanceof LinksOptions linksOptions
                && !linksOptions.links().isEmpty()) {
            throw new IllegalArgumentException(format.getName() + " cannot represent result links");
        }
        if (!format.equals(ResultFormat.XML)
                && options instanceof XmlOutputOptions xmlOptions
                && !xmlOptions.xmlOutputProperties().isEmpty()) {
            throw new IllegalArgumentException(
                    format.getName() + " does not support XML output properties");
        }
        if (options instanceof LineEndingOptions lineOptions
                && !IOConstants.DEFAULT_LINE_ENDING.equals(lineOptions.getLineEnding())
                && (!(format.equals(ResultFormat.CSV) || format.equals(ResultFormat.TSV))
                    || booleanResult)) {
            throw new IllegalArgumentException(
                    format.getName() + " does not use configurable line endings for this result type");
        }
    }

    private static UnsupportedQueryFeatureException unsupported(ResultFormat format) {
        return new UnsupportedQueryFeatureException("Unsupported SPARQL result format: " + format);
    }
}
