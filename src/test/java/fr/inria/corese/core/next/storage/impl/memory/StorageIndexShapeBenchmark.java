package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.LongSupplier;

/** Adversarial index layouts: all-dense prefixes and triples with no shared terms. */
@SuppressWarnings({"java:S106", "java:S1215", "java:S1659", "java:S3078", "java:S3776", "java:S3973"})
public final class StorageIndexShapeBenchmark {
    private static volatile long consumed;
    private static IRI iri(String name) { return new SimpleIRI("urn:shape:" + name); }
    private static long heap() { return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static void collect() { System.gc(); System.gc(); }
    private static long gcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionCount())).sum();
    }
    private static long gcMillis() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionTime())).sum();
    }
    private static void measure(String operation, int iteration, LongSupplier action) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()), gc = gcCount(), gcTime = gcMillis(), start = System.nanoTime();
        long result = action.getAsLong();
        long nanos = System.nanoTime() - start;
        consumed ^= result;
        System.out.printf(java.util.Locale.ROOT, "operation,%s,%d,%.6f,%d,%d,%d,%d%n", operation, iteration,
                nanos / 1e6, bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated,
                gcCount() - gc, gcMillis() - gcTime, result);
    }

    public static void main(String[] arguments) {
        String backend = arguments[0], mode = arguments[1], shape = arguments[2];
        int target = Integer.parseInt(arguments[3]), warmups = Integer.parseInt(arguments[4]), repeats = Integer.parseInt(arguments[5]);
        List<Statement> input = new ArrayList<>();
        if (shape.equals("dense")) {
            IRI[] predicates = new IRI[32], objects = new IRI[32];
            for (int i = 0; i < 32; i++) { predicates[i] = iri("p" + i); objects[i] = iri("o" + i); }
            for (int s = 0; s < target / 1024; s++) {
                IRI subject = iri("s" + s);
                for (IRI predicate : predicates) for (IRI object : objects)
                    input.add(Values.factory().createStatement(subject, predicate, object));
            }
        } else if (shape.equals("unique")) {
            for (int i = 0; i < target; i++)
                input.add(Values.factory().createStatement(iri("s" + i), iri("p" + i), iri("o" + i)));
        } else throw new IllegalArgumentException("shape");
        if (input.isEmpty()) throw new IllegalArgumentException("Dense target must be at least 1024");
        System.out.println("environment," + backend + ',' + mode + ',' + shape + ',' + input.size());
        for (int iteration = -warmups; iteration < repeats; iteration++) {
            try (var storage = Storages.create(StorageConfig.builder().type(backend).build())) {
                collect(); long baseline = heap();
                measure("load", iteration, () -> {
                    long added = mode.equals("batch") ? storage.mutations().addAll(input)
                            : input.stream().filter(storage.mutations()::add).count();
                    if (added != input.size()) throw new AssertionError("load");
                    return added;
                });
                collect();
                System.out.println("heap,load," + iteration + ',' + (heap() - baseline) + ",0");
                Statement first = input.getFirst();
                for (int mask = 0; mask < 8; mask++) {
                    var pattern = StatementPattern.of((mask & 1) == 0 ? null : first.getSubject(),
                            (mask & 2) == 0 ? null : first.getPredicate(), (mask & 4) == 0 ? null : first.getObject(), (Resource) null);
                    var expected = new HashSet<Statement>();
                    for (Statement row : input) {
                        if ((pattern.getSubject() == null || pattern.getSubject().equals(row.getSubject()))
                                && (pattern.getPredicate() == null || pattern.getPredicate().equals(row.getPredicate()))
                                && (pattern.getObject() == null || pattern.getObject().equals(row.getObject()))) expected.add(row);
                    }
                    measure("count-mask-" + mask, iteration, () -> {
                        long count = storage.queries().count(pattern);
                        if (count != expected.size()) throw new AssertionError("count");
                        return count;
                    });
                    measure("find-mask-" + mask, iteration, () -> {
                        try (var rows = storage.queries().find(pattern)) {
                            long count = rows.count();
                            if (count != expected.size()) throw new AssertionError("find count");
                            return count;
                        }
                    });
                    try (var rows = storage.queries().find(pattern)) {
                        List<Statement> actual = rows.toList();
                        if (actual.size() != expected.size() || !new HashSet<>(actual).equals(expected))
                            throw new AssertionError("full statement oracle");
                    }
                    System.out.println("verified,find-mask-" + mask + ',' + iteration);
                }
                measure("remove-reinsert-128", iteration, () -> {
                    for (Statement row : input.subList(0, Math.min(128, input.size()))) {
                        if (!storage.mutations().remove(row) || !storage.mutations().add(row)) throw new AssertionError("mutation");
                    }
                    return 128;
                });
                if (storage.queries().count(StatementPattern.matchAll()) != input.size()) throw new AssertionError("final size");
                if (storage.queries() instanceof IndexedStatementStore indexed) MemoryStorageAssertions.verify(indexed);
            }
        }
        System.out.println("consumed," + consumed);
    }
}
