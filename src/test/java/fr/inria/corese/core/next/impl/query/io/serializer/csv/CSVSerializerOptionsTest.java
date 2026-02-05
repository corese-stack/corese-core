package fr.inria.corese.core.next.impl.query.io.serializer.csv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CSVSerializerOptionsTest {

    @Test
    void getLineEnding() {
        CSVSerializerOptions options = new CSVSerializerOptions.Builder().setLineEnding("tata").build();

        assertEquals("tata", options.getLineEnding());
    }
}