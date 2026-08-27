package fr.inria.corese.core.next.architecture;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Protects the dependency direction between the top-level {@code next} modules. */
class NextModuleBoundaryTest {

    private static final Path CORE_SOURCES = Path.of("src/main/java/fr/inria/corese/core");
    private static final Path NEXT_SOURCES = CORE_SOURCES.resolve("next");
    private static final Pattern CORESE_TYPE_REFERENCE = Pattern.compile(
            "\\bfr\\.inria\\.corese\\.core(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");

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
    void publicContractsMustNotDependOnImplementations() throws IOException {
        for (Path apiDirectory : publicContractDirectories()) {
            assertNoReferences(
                    apiDirectory,
                    imported -> imported.contains(".next.") && imported.contains(".impl."));
        }
    }

    @Test
    void publicContractsMustNotDependOnTheLegacyPipeline() throws IOException {
        for (Path apiDirectory : publicContractDirectories()) {
            assertNoReferences(
                    apiDirectory,
                    imported -> imported.startsWith("fr.inria.corese.core.")
                            && !imported.startsWith("fr.inria.corese.core.next."));
        }
    }

    @Test
    void legacyCodeMustNotDependOnNextImplementations() throws IOException {
        assertNoReferences(
                CORE_SOURCES,
                source -> !source.startsWith(NEXT_SOURCES),
                imported -> imported.startsWith("fr.inria.corese.core.next.")
                        && imported.contains(".impl.")
                        // KGRAM migration hooks are deliberately outside the next boundary.
                        && !imported.startsWith("fr.inria.corese.core.next.query.impl.kgram."));
    }

    @Test
    void publicApiAndSpiPackagesMustBeDocumented() throws IOException {
        assertPackagesAreDocumented(publicContractDirectories(), "Undocumented public API or SPI packages:");
    }

    @Test
    void nonKgramImplementationPackagesMustBeDocumented() throws IOException {
        assertPackagesAreDocumented(
                nonKgramImplementationDirectories(), "Undocumented non-KGRAM implementation packages:");
    }

    private static void assertPackagesAreDocumented(List<Path> packageRoots, String message)
            throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path packageRoot : packageRoots) {
            try (Stream<Path> paths = Files.walk(packageRoot)) {
                for (Path packageDirectory : paths.filter(Files::isDirectory).toList()) {
                    try (Stream<Path> sources = Files.list(packageDirectory)) {
                        boolean containsTypes = sources.anyMatch(source ->
                                source.toString().endsWith(".java")
                                        && !source.getFileName().toString().equals("package-info.java"));
                        if (containsTypes && !Files.exists(packageDirectory.resolve("package-info.java"))) {
                            violations.add(NEXT_SOURCES.relativize(packageDirectory).toString());
                        }
                    }
                }
            }
        }
        if (!violations.isEmpty()) {
            fail(message + "\n" + String.join("\n", violations));
        }
    }

    private static List<Path> publicContractDirectories() {
        return List.of(
                NEXT_SOURCES.resolve("data/api"),
                NEXT_SOURCES.resolve("data/spi"),
                NEXT_SOURCES.resolve("storage/api"),
                NEXT_SOURCES.resolve("query/api"));
    }

    private static List<Path> nonKgramImplementationDirectories() {
        return List.of(
                NEXT_SOURCES.resolve("data/impl"),
                NEXT_SOURCES.resolve("storage/impl"),
                NEXT_SOURCES.resolve("query/impl/sparql"));
    }

    private static void assertNoImports(Path sourceDirectory, Predicate<String> forbidden)
            throws IOException {
        assertNoImports(sourceDirectory, source -> true, forbidden);
    }

    private static void assertNoImports(
            Path sourceDirectory,
            Predicate<Path> includedSource,
            Predicate<String> forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            for (Path source : paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(includedSource)
                    .toList()) {
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

    private static void assertNoReferences(
            Path sourceDirectory,
            Predicate<String> forbidden) throws IOException {
        assertNoReferences(sourceDirectory, source -> true, forbidden);
    }

    private static void assertNoReferences(
            Path sourceDirectory,
            Predicate<Path> includedSource,
            Predicate<String> forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            for (Path source : paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(includedSource)
                    .toList()) {
                Set<String> references = new LinkedHashSet<>();
                Matcher matcher = CORESE_TYPE_REFERENCE.matcher(Files.readString(source));
                while (matcher.find()) {
                    references.add(matcher.group());
                }
                for (String reference : references) {
                    if (forbidden.test(reference)) {
                        violations.add(NEXT_SOURCES.relativize(source) + " -> " + reference);
                    }
                }
            }
        }
        if (!violations.isEmpty()) {
            fail("Forbidden next-module dependencies:\n" + String.join("\n", violations));
        }
    }
}
