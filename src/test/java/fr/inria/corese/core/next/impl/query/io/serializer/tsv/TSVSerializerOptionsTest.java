package fr.inria.corese.core.next.impl.query.io.serializer.tsv;

import fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv.TSVSerializerOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TSVSerializerOptionsTest {

    @Test
    void getLineEnding() {
        TSVSerializerOptions options = new TSVSerializerOptions.Builder().setLineEnding("tata").build();

        assertEquals("tata", options.getLineEnding());
    }
}