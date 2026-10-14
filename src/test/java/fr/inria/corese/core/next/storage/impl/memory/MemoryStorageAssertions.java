package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Structural checks shared by storage tests and benchmark correctness oracles.
 * Reflection keeps diagnostic access out of production classes. These checks
 * run under the database monitor and must remain outside benchmark timers.
 */
public final class MemoryStorageAssertions {
    private MemoryStorageAssertions() { }

    static boolean add(TripleIndex index, Statement statement) {
        return index.add(statement, statement.getContext());
    }

    static boolean remove(TripleIndex index, Statement statement) {
        return index.remove(new TermKey(statement.getSubject()), new TermKey(statement.getPredicate()),
                new TermKey(statement.getObject()));
    }

    /** Checks counts, contexts, permutation parity and canonical key identity. */
    public static void verify(IndexedStatementStore store) {
        synchronized (field(store, "database")) {
            invoke(store, "checkAccess");
            Object state = invoke(store, "state");
            Map<?, ?> graphs = field(state, "graphs");
            long total = 0;
            for (var entry : graphs.entrySet()) {
                Object partition = entry.getValue();
                TripleIndex spo = field(partition, "spo");
                TripleIndex pos = field(partition, "pos");
                TripleIndex osp = field(partition, "osp");
                verifyTerms(spo, pos, osp);
                Set<Statement> subjects = statements(spo);
                Set<Statement> predicates = statements(pos);
                Set<Statement> objects = statements(osp);
                if (!subjects.equals(predicates) || !subjects.equals(objects)
                        || subjects.size() != spo.size() || predicates.size() != pos.size()
                        || objects.size() != osp.size()
                        || subjects.stream().anyMatch(s -> !Objects.equals(s.getContext(), entry.getKey()))) {
                    throw new IllegalStateException("Inconsistent index partition");
                }
                total += subjects.size();
            }
            if (total != (long) field(state, "size")) {
                throw new IllegalStateException("Inconsistent statement count");
            }
        }
    }

    private static Set<Statement> statements(TripleIndex index) {
        Set<Statement> result = new HashSet<>();
        index.visit(StatementPattern.matchAll(), statement -> { result.add(statement); return false; });
        return result;
    }

    private static void verifyTerms(TripleIndex spo, TripleIndex pos, TripleIndex osp) {
        if (field(spo, "stagingTerms") != null) throw new IllegalStateException("Published bulk cache");
        Map<TermKey, TermKey> canonical = new HashMap<>();
        List<TripleIndex> indexes = List.of(spo, pos, osp);
        for (TripleIndex index : indexes) roots(index).forEach((key, branch) ->
                canonical.putIfAbsent(key, field(branch, "key")));
        for (TripleIndex index : indexes) roots(index).forEach((first, branch) -> {
            verifyCanonical(canonical, first);
            branch.forEach((second, entry) -> {
                verifyCanonical(canonical, second);
                if (entry instanceof CompactTermMap<?> leaf) {
                    leaf.forEach((third, statement) -> verifyCanonical(canonical, third));
                }
            });
        });
    }

    private static void verifyCanonical(Map<TermKey, TermKey> canonical, TermKey key) {
        if (canonical.get(key) != key) throw new IllegalStateException("Non-canonical term key");
    }

    private static Map<TermKey, CompactTermMap<Object>> roots(TripleIndex index) {
        return field(index, "roots");
    }

    @SuppressWarnings({"java:S3011", "unchecked"})
    private static <T> T field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot inspect " + name, e);
        }
    }

    @SuppressWarnings("java:S3011")
    private static Object invoke(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            if (e.getCause() instanceof Error cause) throw cause;
            throw new AssertionError("Diagnostic invocation failed: " + name, e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot inspect " + name, e);
        }
    }
}
