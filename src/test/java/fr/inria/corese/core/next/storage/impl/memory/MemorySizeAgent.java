package fr.inria.corese.core.next.storage.impl.memory;

import java.lang.instrument.Instrumentation;

/** Standalone profiler agent; not loaded by production or JUnit. */
public final class MemorySizeAgent {
    private static Instrumentation instrumentation;

    private MemorySizeAgent() {
        // Utility / agent class
    }

    // The JVM agent entry point requires this two-parameter signature, even without agent options.
    @SuppressWarnings("java:S1172")
    public static void premain(String arguments, Instrumentation agent) {
        instrumentation = agent;
    }

    static long size(Object value) {
        if (instrumentation == null) throw new IllegalStateException("Start with the profiling -javaagent jar");
        return instrumentation.getObjectSize(value);
    }
}
