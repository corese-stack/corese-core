package fr.inria.corese.core.next.io;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.model.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CoreseIOWriteOverloadsTest {

    @TempDir
    Path tempDir;

    private static Model buildModel() {
        String turtle = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
        return CoreseIO.read(new StringReader(turtle), RDFFormat.NTRIPLES);
    }

    @Test
    @DisplayName("write(Model, RDFFormat, RDFSerializerOptions, OutputStream) should produce non-empty output")
    void writeWithOptionsToOutputStreamProducesOutput() {
        RDFSerializerOptions opts = RDFSerializerOptions.builder().build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        CoreseIO.write(buildModel(), RDFFormat.NTRIPLES, opts, out);

        assertNotEquals(0, out.size(), "output stream should not be empty");
    }

    @Test
    @DisplayName("write(Model, RDFFormat, RDFSerializerOptions, Path) should create a non-empty file")
    void writeWithOptionsToPathProducesFile() throws Exception {
        RDFSerializerOptions opts = RDFSerializerOptions.builder().build();
        Path dest = tempDir.resolve("output.nt");

        CoreseIO.write(buildModel(), RDFFormat.NTRIPLES, opts, dest);

        assertTrue(Files.exists(dest), "output file should exist");
        assertTrue(Files.size(dest) > 0, "output file should not be empty");
    }

    @Test
    @DisplayName("write with sortSubjects option should sort output lines lexicographically")
    void writeWithOptionsSortsTurtle() {
        Model model = CoreseIO.read(
                new StringReader("""
                        <http://example.org/bbb> <http://example.org/p> <http://example.org/o> .
                        <http://example.org/aaa> <http://example.org/p> <http://example.org/o> .
                        """),
                RDFFormat.NTRIPLES);

        RDFSerializerOptions opts = RDFSerializerOptions.builder().sortSubjects(true).build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        CoreseIO.write(model, RDFFormat.NTRIPLES, opts, out);
        String output = out.toString();

        assertTrue(output.indexOf("aaa") < output.indexOf("bbb"),
                "aaa should appear before bbb when sortSubjects is enabled");
    }
}
