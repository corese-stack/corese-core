package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TsvResultSerializerOptionsTest {

    @Test
    void getLineEnding() {
        TsvResultSerializerOptions options = new TsvResultSerializerOptions.Builder().setLineEnding("tata").build();

        assertEquals("tata", options.getLineEnding());
    }
}
