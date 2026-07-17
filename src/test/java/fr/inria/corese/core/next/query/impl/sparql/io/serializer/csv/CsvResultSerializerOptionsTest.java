package fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvResultSerializerOptionsTest {

    @Test
    void getLineEnding() {
        CsvResultSerializerOptions options = new CsvResultSerializerOptions.Builder().setLineEnding("tata").build();

        assertEquals("tata", options.getLineEnding());
    }
}
