package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.eval.Stack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Identity-preserving copy of a completed native query graph. A copy operation is
 * confined to one thread and discarded after construction. Unknown mutable types
 * fail explicitly rather than being shared between the image and an execution.
 */
public final class PlanCopy {
    private final IdentityHashMap<Object, Object> copied = new IdentityHashMap<>();

    /**
     * Registers a shell before following cyclic graph links.
     *
     * @param source original object
     * @param target independent copy under construction
     */
    public void remember(Object source, Object target) {
        copied.put(source, target);
    }

    /**
     * Copies a compiled object, preserving sharing only within the new graph.
     *
     * @param <T> source contract type
     * @param source object or null
     * @return independent copy, or the same immutable scalar/value
     * @throws IllegalArgumentException if the graph contains an unsupported mutable type
     */
    @SuppressWarnings({"unchecked", "java:S9391"}) // Preserve source contracts and avoid a stream allocation for every copied container.
    public <T> T copy(T source) {
        if (source == null || source instanceof String || source instanceof Enum<?>
                || source instanceof Integer || source instanceof Long || source instanceof Boolean || source instanceof DatatypeValue) {
            return source;
        }
        Object existing = copied.get(source);
        if (existing != null) {
            return (T) existing;
        }
        if (source instanceof PlanCopyable<?> value) {
            return (T) value.copyPlan(this);
        }
        if (source instanceof Stack stack) {
            Stack result = new Stack();
            remember(source, result);
            for (Exp value : stack) result.add(copy(value));
            return (T) result;
        }
        if (source instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            remember(source, result);
            for (Object value : list) result.add(copy(value));
            return (T) result;
        }
        if (source instanceof Map<?, ?> map) {
            Map<Object, Object> result = new HashMap<>();
            remember(source, result);
            map.forEach((key, value) -> result.put(copy(key), copy(value)));
            return (T) result;
        }
        throw new IllegalArgumentException("Unsupported mutable plan component: " + source.getClass().getName());
    }
}
