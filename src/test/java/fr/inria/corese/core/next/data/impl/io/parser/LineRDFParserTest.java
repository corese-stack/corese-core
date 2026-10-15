package fr.inria.corese.core.next.data.impl.io.parser;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.impl.model.LinkedHashModel;
import fr.inria.corese.core.next.io.CoreseIO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LineRDFParserTest {
    static Stream<RDFFormat> formats() {
        return Stream.of(RDFFormat.NTRIPLES, RDFFormat.NQUADS);
    }

    @ParameterizedTest
    @MethodSource("formats")
    void emitsStatementsBeforeReadingTheWholeInput(RDFFormat format) {
        GeneratedReader reader = new GeneratedReader(20_000);
        long[] counts = { 0, 0 };
        Model model = mock(Model.class, withSettings().stubOnly().defaultAnswer(invocation -> {
            if (invocation.getMethod().getName().equals("add")) {
                if (counts[0]++ == 0) {
                    counts[1] = reader.position;
                }
                return true;
            }
            return RETURNS_DEFAULTS.answer(invocation);
        }));
        CoreseIO.parser(format, model, Values.factory()).parse(reader);
        assertEquals(20_000, counts[0]);
        assertTrue(counts[1] < reader.totalCharacters / 2);
        assertFalse(reader.closed);
    }

    @ParameterizedTest
    @MethodSource("formats")
    void acceptsBlankLinesCommentsLineEndingsAndDirectiveTextInsideLiterals(RDFFormat format) {
        String input = "# comment\r\n\r\n<urn:s> <urn:p> \"@prefix @base\" .\r_:a <urn:p> _:a .\n<urn:t> <urn:p> \"escaped\\nvalue\" .";
        var model = CoreseIO.read(new StringReader(input), format);
        assertEquals(3, model.size());
        assertTrue(model.contains(Values.factory().createIRI("urn:s"), Values.factory().createIRI("urn:p"), Values.factory().createLiteral("@prefix @base")));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void rejectsTrailingContentAndReportsThePhysicalLine(RDFFormat format) {
        var parser = CoreseIO.parser(format, new LinkedHashModel(Values.factory()), Values.factory());
        ParsingException failure = assertThrows(ParsingException.class, () -> parser.parse(
                new StringReader("# header\n\n<urn:s> <urn:p> <urn:o> . garbage")));
        assertTrue(failure.getMessage().contains("3:"), failure.getMessage());
        assertThrows(ParsingException.class, () -> parser.parse(new StringReader("@prefix x: <urn:> .")));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void interruptedParsingFailsWithoutConsumingTheInterrupt(RDFFormat format) {
        try {
            Thread.currentThread().interrupt();
            assertThrows(ParsingException.class, () -> CoreseIO.read(
                    new StringReader("<urn:s> <urn:p> <urn:o> .\n"), format));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private static final class GeneratedReader extends Reader {
        private static final String ROW = "<urn:s> <urn:p> <urn:o> .\n";
        private final long totalCharacters;
        private long position;
        private boolean closed;

        private GeneratedReader(long rows) {
            totalCharacters = rows * ROW.length();
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            if (closed) {
                throw new IOException("Reader closed");
            }
            if (length == 0) {
                return 0;
            }
            if (position == totalCharacters) {
                return -1;
            }
            int count = (int) Math.min(length, totalCharacters - position);
            for (int index = 0; index < count; index++) {
                buffer[offset + index] = ROW.charAt((int) (position++ % ROW.length()));
            }
            return count;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
