package fr.inria.corese.core.next.query.impl.io.serializer.csv;

import fr.inria.corese.core.next.api.base.io.FileFormat;
import fr.inria.corese.core.next.api.base.io.ResultFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializer;
import fr.inria.corese.core.next.kgram.core.Mappings;

public class CSVSerializer extends CharacterSeparatedValuesSerializer {

    public CSVSerializer(Mappings results, IOOptions options) {
        super(",", results, options);
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.CSV;
    }
}
