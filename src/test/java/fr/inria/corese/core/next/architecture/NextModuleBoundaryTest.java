package fr.inria.corese.core.next.architecture;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Protects the dependency direction between the top-level {@code next} modules. */
class NextModuleBoundaryTest {

    private static final Path NEXT_SOURCES =
            Path.of("src/main/java/fr/inria/corese/core/next");

    @Test
    void sharedCodeMustNotDependOnDomainModules() throws IOException {
        assertNoImports(
                NEXT_SOURCES.resolve("common"),
                imported -> imported.startsWith("fr.inria.corese.core.next.data.")
                        || imported.startsWith("fr.inria.corese.core.next.storage.")
                        || imported.startsWith("fr.inria.corese.core.next.query."));
    }

    @Test
    void dataMustNotDependOnStorageOrQuery() throws IOException {
        assertNoImports(
                NEXT_SOURCES.resolve("data"),
                imported -> imported.startsWith("fr.inria.corese.core.next.storage.")
                        || imported.startsWith("fr.inria.corese.core.next.query."));
    }

    @Test
    void storageMustNotDependOnQuery() throws IOException {
        assertNoImports(
                NEXT_SOURCES.resolve("storage"),
                imported -> imported.startsWith("fr.inria.corese.core.next.query."));
    }

    @Test
    void publicApisMustNotDependOnImplementations() throws IOException {
        List<Path> apiDirectories = List.of(
                NEXT_SOURCES.resolve("data/api"),
                NEXT_SOURCES.resolve("storage/api"),
                NEXT_SOURCES.resolve("query/api"));
        for (Path apiDirectory : apiDirectories) {
            assertNoImports(apiDirectory, imported -> imported.contains(".next.") && imported.contains(".impl."));
        }
    }

    private static void assertNoImports(Path sourceDirectory, Predicate<String> forbidden)
            throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(source)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("import ")) {
                        String imported = trimmed
                                .substring("import ".length())
                                .replaceFirst("^static\\s+", "")
                                .replace(";", "");
                        if (forbidden.test(imported)) {
                            violations.add(NEXT_SOURCES.relativize(source) + " -> " + imported);
                        }
                    }
                }
            }
        }
        if (!violations.isEmpty()) {
            fail("Forbidden next-module dependencies:\n" + String.join("\n", violations));
        }
    }
}
