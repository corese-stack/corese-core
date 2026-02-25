package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TSVSerializerOptionsTest {

    @Test
    void getLineEnding() {
        TSVSerializerOptions options = new TSVSerializerOptions.Builder().setLineEnding("tata").build();

        assertEquals("tata", options.getLineEnding());
    }
}