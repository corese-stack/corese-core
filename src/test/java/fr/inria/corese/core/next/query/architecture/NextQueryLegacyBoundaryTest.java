package fr.inria.corese.core.next.query.architecture;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;

/**
 * Freezes the legacy dependencies that still exist in {@code next.query}.
 *
 * <p>The allowlist is a baseline, not a list of acceptable long-term dependencies. Existing
 * entries are removed as the decoupling progresses. Any new entry fails this test and must be
 * reviewed explicitly.
 */
class NextQueryLegacyBoundaryTest {

    private static final Path SOURCE_DIRECTORY =
            Path.of("src/main/java/fr/inria/corese/core/next/query");
    private static final Path ALLOWLIST = Path.of(
            "src/test/resources/fr/inria/corese/core/next/query/architecture/"
                    + "legacy-dependency-allowlist.txt");

    private static final Pattern QUALIFIED_REFERENCE = Pattern.compile(
            "fr\\.inria\\.corese\\.core\\.(?:"
                    + "(?:sparql|kgram)(?:\\.[A-Za-z_$][\\w$]*)+"
                    + "|Graph(?:\\.[A-Za-z_$][\\w$]*)*"
                    + "|next\\.data\\.impl\\.adapter(?:\\.[A-Za-z_$][\\w$]*)+"
                    + ")");

    @Test
    void legacyDependenciesMustMatchTheReviewedBaseline() throws IOException {
        Path projectRoot = findProjectRoot();
        TreeSet<String> expected = readAllowlist(projectRoot.resolve(ALLOWLIST));
        TreeSet<String> actual = scanDependencies(projectRoot.resolve(SOURCE_DIRECTORY));

        TreeSet<String> added = new TreeSet<>(actual);
        added.removeAll(expected);

        TreeSet<String> removed = new TreeSet<>(expected);
        removed.removeAll(actual);

        if (!added.isEmpty() || !removed.isEmpty()) {
            fail("Legacy dependency baseline changed.\n"
                    + formatChanges("Unexpected dependencies", "+ ", added)
                    + formatChanges("Dependencies no longer present", "- ", removed)
                    + "Review the change, then update " + ALLOWLIST + ".");
        }
    }

    private static TreeSet<String> scanDependencies(Path sourceRoot) throws IOException {
        List<Path> sourceFiles;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK is required to scan Java sources");
        }

        Map<String, Integer> dependencies = new TreeMap<>();
        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("-proc:none"),
                    null,
                    fileManager.getJavaFileObjectsFromPaths(sourceFiles));

            for (CompilationUnitTree unit : task.parse()) {
                scanCompilationUnit(sourceRoot, unit, dependencies);
            }
        }

        TreeSet<String> result = new TreeSet<>();
        dependencies.forEach((dependency, count) -> result.add(dependency + "|" + count));
        return result;
    }

    private static void scanCompilationUnit(
            Path sourceRoot,
            CompilationUnitTree unit,
            Map<String, Integer> dependencies) {
        Path sourceFile = Path.of(unit.getSourceFile().toUri());
        String relativePath = sourceRoot.relativize(sourceFile).toString().replace('\\', '/');

        for (ImportTree importTree : unit.getImports()) {
            String target = importTree.getQualifiedIdentifier().toString();
            if (isForbiddenDependency(target)) {
                String kind = importTree.isStatic() ? "STATIC_IMPORT" : "IMPORT";
                increment(dependencies, relativePath, kind, target);
            }
        }

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitImport(ImportTree node, Void unused) {
                return null;
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void unused) {
                Matcher matcher = QUALIFIED_REFERENCE.matcher(node.toString());
                if (matcher.matches()) {
                    increment(
                            dependencies,
                            relativePath,
                            "QUALIFIED_REFERENCE",
                            matcher.group());
                    return null;
                }
                return super.visitMemberSelect(node, unused);
            }
        }.scan(unit, null);
    }

    private static boolean isForbiddenDependency(String target) {
        return target.startsWith("fr.inria.corese.core.sparql.")
                || target.startsWith("fr.inria.corese.core.kgram.")
                || target.equals("fr.inria.corese.core.Graph")
                || target.startsWith("fr.inria.corese.core.Graph.")
                || target.startsWith("fr.inria.corese.core.next.data.impl.adapter.");
    }

    private static void increment(
            Map<String, Integer> dependencies,
            String path,
            String kind,
            String target) {
        dependencies.merge(path + "|" + kind + "|" + target, 1, Integer::sum);
    }

    private static TreeSet<String> readAllowlist(Path allowlist) throws IOException {
        TreeSet<String> entries = new TreeSet<>();
        for (String line : Files.readAllLines(allowlist, StandardCharsets.UTF_8)) {
            String entry = line.trim();
            if (!entry.isEmpty() && !entry.startsWith("#")) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Path findProjectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(SOURCE_DIRECTORY))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root containing " + SOURCE_DIRECTORY);
    }

    private static String formatChanges(String title, String prefix, TreeSet<String> changes) {
        if (changes.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(title).append(":\n");
        changes.forEach(change -> result.append(prefix).append(change).append('\n'));
        return result.append('\n').toString();
    }
}
