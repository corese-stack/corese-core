package fr.inria.corese.core.next.data.api.io.format;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FileFormat}.
 *
 * <p>
 * Coverage :
 * </p>
 * <ul>
 * <li>Valid constructor & getters (parametrised)</li>
 * <li>Null / empty argument validation</li>
 * <li>Immutability of internal collections</li>
 * <li>equals / hashCode full contract (symmetry, transitivity, null & type
 * safety)</li>
 * <li>Meaningful toString()</li>
 * </ul>
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileFormatTest {

    /*
     * ---------------------------------------------------------- *
     * Test data (re-usable across parameterised tests) *
     * ----------------------------------------------------------
     */
    private static Stream<Arguments> validFormats() {
        return Stream.of(
                Arguments.of("Turtle", List.of("ttl"), List.of("text/turtle")),
                Arguments.of("N-Triples", List.of("nt"), List.of("application/n-triples")),
                Arguments.of("JSON-LD", List.of("jsonld"), List.of("application/ld+json")),
                Arguments.of("RDF/XML", List.of("rdf", "xml"), List.of("application/rdf+xml")),
                Arguments.of("TriG", List.of("trig"), List.of("application/trig")));
    }

    /*
     * ---------------------------------------------------------- *
     * Constructor & getters *
     * ----------------------------------------------------------
     */
    @ParameterizedTest(name = "Create FileFormat: {0}")
    @MethodSource("validFormats")
    @DisplayName("Constructor populates fields correctly")
    void constructorSetsAllFields(String name,
            List<String> extensions,
            List<String> mimeTypes) {

        FileFormat format = new FileFormat(name, extensions, mimeTypes);

        assertAll("All getters reflect constructor arguments",
                () -> assertEquals(name, format.getName(), "name"),
                () -> assertEquals(extensions, format.getExtensions(), "extensions"),
                () -> assertEquals(mimeTypes, format.getMimeTypes(), "mimeTypes"),
                () -> assertEquals(extensions.getFirst(), format.getDefaultExtension(), "defaultExtension"),
                () -> assertEquals(mimeTypes.getFirst(), format.getDefaultMimeType(), "defaultMimeType"));
    }

    /*
     * ---------------------------------------------------------- *
     * Validation of constructor arguments *
     * ----------------------------------------------------------
     */
    @Nested
    class ConstructorArgumentValidation {

        private final List<String> ext = List.of("ttl");
        private final List<String> mime = List.of("text/turtle");

        @Test
        void nullNameThrowsNpe() {
            assertThrows(NullPointerException.class,
                    () -> new FileFormat(null, ext, mime));
        }

        @Test
        void nullExtensionsThrowsNpe() {
            assertThrows(NullPointerException.class,
                    () -> new FileFormat("Turtle", null, mime));
        }

        @Test
        void nullMimeTypesThrowsNpe() {
            assertThrows(NullPointerException.class,
                    () -> new FileFormat("Turtle", ext, null));
        }

        @Test
        void emptyExtensionsThrowsIae() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FileFormat("Turtle", List.of(), mime));
        }

        @Test
        void emptyMimeTypesThrowsIae() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FileFormat("Turtle", ext, List.of()));
        }
    }

    /*
     * ---------------------------------------------------------- *
     * Immutability & defensive copies *
     * ----------------------------------------------------------
     */
    @Test
    void internalListsAreImmutableAndDefensivelyCopied() {
        // Build mutable lists
        List<String> ext = new ArrayList<>(List.of("ttl"));
        List<String> mime = new ArrayList<>(List.of("text/turtle"));
        FileFormat format = new FileFormat("Turtle", ext, mime);

        // Mutate originals AFTER construction
        ext.add("bad");
        mime.add("bad/type");

        // Verify defensive copy & immutability
        assertEquals(List.of("ttl"), format.getExtensions(), "defensive copy for extensions");
        assertEquals(List.of("text/turtle"), format.getMimeTypes(), "defensive copy for mimeTypes");

        // Returned lists must be unmodifiable
        List<String> extensions = format.getExtensions();
        List<String> mimeTypes = format.getMimeTypes();
        assertAll("Returned lists are unmodifiable",
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> extensions.add("new")),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> mimeTypes.add("new/type")));
    }

    /*
     * ---------------------------------------------------------- *
     * equals & hashCode contract *
     * ----------------------------------------------------------
     */
    @Nested
    class EqualsAndHashCodeContract {

        private final FileFormat base = new FileFormat("Turtle", List.of("ttl"), List.of("text/turtle"));

        @Test
        void symmetryAndCaseInsensitivity() {
            FileFormat sameDifferentCase = new FileFormat("tUrTlE", List.of("ttl"), List.of("text/turtle"));

            assertEquals(base, sameDifferentCase);
            assertEquals(sameDifferentCase, base);
            assertEquals(base.hashCode(), sameDifferentCase.hashCode());
        }

        @Test
        void transitivity() {
            FileFormat a = new FileFormat("Turtle", List.of("ttl"), List.of("text/turtle"));
            FileFormat b = new FileFormat("TURTLE", List.of("ttl"), List.of("text/turtle"));
            FileFormat c = new FileFormat("turtle", List.of("ttl"), List.of("text/turtle"));

            assertAll(
                    () -> assertEquals(a, b),
                    () -> assertEquals(b, c),
                    () -> assertEquals(a, c));
        }

        @Test
        void inequalityWhenAnyFieldDiffers() {
            FileFormat diffName = new FileFormat("N-Triples", List.of("ttl"), List.of("text/turtle"));
            FileFormat diffExt = new FileFormat("Turtle", List.of("nt"), List.of("text/turtle"));
            FileFormat diffMime = new FileFormat("Turtle", List.of("ttl"), List.of("application/n-triples"));

            assertAll(
                    () -> assertNotEquals(base, diffName),
                    () -> assertNotEquals(base, diffExt),
                    () -> assertNotEquals(base, diffMime),
                    () -> assertNotEquals(null, base),
                    () -> assertNotEquals("some string", base));
        }
    }

    /*
     * ---------------------------------------------------------- *
     * toString() *
     * ----------------------------------------------------------
     */
    @Test
    void toStringContainsAllRelevantInformation() {
        FileFormat format = new FileFormat("Turtle", List.of("ttl"), List.of("text/turtle"));

        String out = format.toString();
        assertTrue(
                Pattern.compile("Turtle", Pattern.CASE_INSENSITIVE).matcher(out).find(),
                "name is present");
        assertTrue(out.contains("ttl"), "extension is present");
        assertTrue(out.contains("text/turtle"), "mime type is present");
    }
}
