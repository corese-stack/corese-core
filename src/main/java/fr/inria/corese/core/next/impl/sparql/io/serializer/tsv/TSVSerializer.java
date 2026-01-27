package fr.inria.corese.core.next.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.api.base.io.FileFormat;
import fr.inria.corese.core.next.api.base.io.ResultFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializer;
import fr.inria.corese.core.next.kgram.core.Mappings;

public class TSVSerializer  extends CharacterSeparatedValuesSerializer {

    public TSVSerializer(Mappings results, IOOptions options) {
        super("\t", results, options);
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.TSV;
    }
}
