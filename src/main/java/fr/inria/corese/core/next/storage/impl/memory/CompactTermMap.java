package fr.inria.corese.core.next.storage.impl.memory;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Insertion-ordered term map with representations adapted to small cardinalities.
 *
 * <p>
 * An empty map owns no entry buffer. A singleton stores its key separately and its value directly in {@code entries}.
 * Larger small maps use alternating key and value slots in an {@code Object[]} array. Beyond the array threshold,
 * {@code entries} holds a {@link DenseTermTable}. Dense maps demote at half the promotion threshold to avoid
 * oscillation. Keys and values must be non-null; representation transitions and filtering preserve the order of
 * survivors.
 *
 * <p>
 * Copies preserve the current representation and duplicate its mutable buffers. The mapped copy constructor
 * additionally copies nested mutable values, allowing index branches to own their leaves while sharing immutable terms
 * and statements. Mutation requires exclusive partition ownership under the database monitor. Positional traversal
 * requires an unchanged, retained partition for its lifetime.
 *
 * @param <V> non-null entry value type
 */
class CompactTermMap<V> {
    /**
     * Maximum flat-pair cardinality before dense promotion.
     */
    private static final int ARRAY_LIMIT = 8;
    /**
     * Key used only by the singleton representation.
     */
    private TermKey singletonKey;
    /**
     * Empty, singleton, flat-pair or dense representation of live values.
     */
    private Object entries;
    /**
     * Number of live entries or statements owned by this container.
     */
    private int size;

    /**
     * Creates an empty compact map; buffers are allocated on demand.
     */
    CompactTermMap() {
    }

    /**
     * Copies the container buffers while sharing keys and values.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     */

    CompactTermMap(final CompactTermMap<V> source) {
        singletonKey = source.singletonKey;
        size = source.size;
        if (source.entries instanceof DenseTermTable<?>) {
            entries = new DenseTermTable<>(source.table());
        } else if (size > 1) {
            entries = ((Object[]) source.entries).clone();
        } else {
            entries = source.entries;
        }
    }

    /**
     * Preserve the representation and its independent buffers; map values without lookups.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     * @param values mapper for live values in the copied, independently owned buffers
     * @throws NullPointerException if an explicitly required argument or mapped value is null
     */

    CompactTermMap(final CompactTermMap<V> source, final Function<? super V, ? extends V> values) {
        this(source);
        if (entries instanceof DenseTermTable<?>) {
            table().mapValues(values);
        } else if (size > 1) {
            final Object[] pairs = (Object[]) entries;
            for (int i = 1; i < size * 2; i += 2) {
                pairs[i] = Objects.requireNonNull(values.apply(value(pairs[i])));
            }
        } else if (size == 1) {
            entries = Objects.requireNonNull(values.apply(value(entries)));
        }
    }

    /**
     * Returns the number of live entries.
     *
     * @return live entry count
     */
    int size() {
        return size;
    }

    /**
     * Reports whether this container has no live entries.
     *
     * @return true when no live entries remain
     */
    boolean isEmpty() {
        return size == 0;
    }

    /**
     * Reads the sole value of a singleton representation.
     *
     * @return the sole non-null value
     * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
     */
    V singletonValue() {
        if (size != 1) {
            throw new IllegalStateException("Not a singleton");
        }
        return value(entries);
    }

    /**
     * Replaces the value of an existing key without changing its position.
     *
     * @param key         search or insertion key; null denotes a wildcard only for pattern traversal
     * @param replacement non-null replacement for an existing value
     * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
     */
    void replace(final TermKey key, final V replacement) {
        if (size == 1 && same(key, singletonKey)) {
            entries = replacement;
            return;
        }
        if (entries instanceof DenseTermTable<?>) {
            if (table().replace(key, replacement) == null) {
                throw new IllegalStateException("Missing key");
            }
            return;
        }
        if (size > 1) {
            final Object[] pairs = (Object[]) entries;
            for (int i = 0; i < size * 2; i += 2) {
                if (same(key, (TermKey) pairs[i])) {
                    pairs[i + 1] = replacement;
                    return;
                }
            }
        }
        throw new IllegalStateException("Missing key");
    }

    /**
     * Reads an immutable RDF value or a value held in the container representation.
     *
     * @param value non-null RDF value or entry value
     * @return the stored RDF value or the non-null entry value at the specified position
     */
    @SuppressWarnings("unchecked")
    private V value(final Object value) {
        return (V) value;
    }

    /**
     * Accesses the dense representation; the caller must have established that it is active.
     *
     * @return the active dense representation
     */
    @SuppressWarnings("unchecked")
    private DenseTermTable<V> table() {
        return (DenseTermTable<V>) entries;
    }

    /**
     * Compares term keys using their cached hashes before structural equality.
     *
     * @param left  first canonical term key
     * @param right second canonical term key
     * @return true when both cached hashes and RDF identities match
     */
    private static boolean same(final TermKey left, final TermKey right) {
        return left.hashCode() == right.hashCode() && left.equals(right);
    }

    /**
     * Looks up a value without changing encounter order.
     *
     * @param candidate search key
     * @return the matching value, or null if absent
     */
    V get(final TermKey candidate) {
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return same(candidate, singletonKey) ? value(entries) : null;
        }
        if (entries instanceof DenseTermTable<?>) {
            return table().get(candidate);
        }
        final Object[] pairs = (Object[]) entries;
        for (int i = 0; i < size * 2; i += 2) {
            if (same(candidate, (TermKey) pairs[i])) {
                return value(pairs[i + 1]);
            }
        }
        return null;
    }

    /**
     * Adds a statement or keyed value if it is absent.
     *
     * @param key   search or insertion key; null denotes a wildcard only for pattern traversal
     * @param value non-null RDF value or entry value
     * @return true when an absent entry was added, false when it was already present
     */
    boolean add(final TermKey key, final V value) {
        if (size == 0) {
            singletonKey = key;
            entries = value;
            size = 1;
            return true;
        }
        if (size == 1) {
            if (same(key, singletonKey)) {
                return false;
            }
            entries = new Object[] { singletonKey, entries, key, value };
            singletonKey = null;
            size = 2;
            return true;
        }
        if (entries instanceof DenseTermTable<?>) {
            if (table().putIfAbsent(key, value) != null) {
                return false;
            }
            size++;
            return true;
        }
        return addToArray(key, value);
    }

    /**
     * Adds an absent pair to the flat representation, promoting at the existing threshold.
     *
     * @param key   search or insertion key; null denotes a wildcard only for pattern traversal
     * @param value non-null RDF value or entry value
     * @return true after insertion, false for an existing key
     */
    private boolean addToArray(final TermKey key, final V value) {
        if (get(key) != null) {
            return false;
        }
        Object[] pairs = (Object[]) entries;
        if (size == ARRAY_LIMIT) {
            final DenseTermTable<V> table = new DenseTermTable<>(size + 1);
            for (int i = 0; i < size * 2; i += 2) {
                table.put((TermKey) pairs[i], value(pairs[i + 1]));
            }
            table.put(key, value);
            entries = table;
        } else {
            if (pairs.length == size * 2) {
                entries = pairs = Arrays.copyOf(pairs, pairs.length * 2);
            }
            pairs[size * 2] = key;
            pairs[size * 2 + 1] = value;
        }
        size++;
        return true;
    }

    /**
     * Removes matching content while preserving the order of survivors.
     *
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @return true when an entry was removed, false when it was absent
     */
    boolean remove(final TermKey key) {
        if (size == 0) {
            return false;
        }
        if (size == 1) {
            if (!same(key, singletonKey)) {
                return false;
            }
            singletonKey = null;
            entries = null;
            size = 0;
            return true;
        }
        return entries instanceof DenseTermTable<?> ? removeFromTable(key) : removeFromArray(key);
    }

    /**
     * Removes a key from the dense representation and demotes when its threshold is reached.
     *
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @return true after removal, false for an absent key
     */
    private boolean removeFromTable(final TermKey key) {
        if (table().remove(key) == null) {
            return false;
        }
        size--;
        if (size <= ARRAY_LIMIT / 2) {
            final Object[] pairs = new Object[size * 2];
            int offset = 0;
            for (var entry : table().entrySet()) {
                pairs[offset++] = entry.getKey();
                pairs[offset++] = entry.getValue();
            }
            entries = pairs;
        }
        return true;
    }

    /**
     * Removes a pair from the flat representation with stable shifts and existing shrink rules.
     *
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @return true after removal, false for an absent key
     */
    private boolean removeFromArray(final TermKey key) {
        final Object[] pairs = (Object[]) entries;
        for (int i = 0; i < size * 2; i += 2) {
            if (!same(key, (TermKey) pairs[i])) {
                continue;
            }
            removePairAt(i, pairs);
            return true;
        }
        return false;
    }

    /**
     * Removes one live flat pair, preserving survivor order and the existing shrink thresholds.
     *
     * @param index even offset of the matching live key
     * @param pairs exclusively owned flat key/value buffer
     */
    private void removePairAt(final int index, final Object[] pairs) {
        System.arraycopy(pairs, index + 2, pairs, index, (size * 2) - index - 2);
        pairs[(size * 2) - 2] = null;
        pairs[(size * 2) - 1] = null;
        size--;
        if (size == 1) {
            singletonKey = (TermKey) pairs[0];
            entries = pairs[1];
        } else if (size * 4 <= pairs.length) {
            entries = Arrays.copyOf(pairs, size * 2);
        }
    }

    /**
     * Visits the selected value or all values in encounter order, stopping when the visitor returns true.
     *
     * @param key     search or insertion key; null denotes a wildcard only for pattern traversal
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     * @return true when a selected value caused the visitor to request an early stop
     */
    boolean visitMatching(final TermKey key, final Predicate<V> visitor) {
        if (key != null) {
            final V match = get(key);
            return match != null && visitor.test(match);
        }
        if (size == 0) {
            return false;
        }
        if (size == 1) {
            return visitor.test(value(entries));
        }
        if (entries instanceof DenseTermTable<?>) {
            return table().visit(visitor);
        }
        final Object[] pairs = (Object[]) entries;
        for (int i = 1; i < size * 2; i += 2) {
            if (visitor.test(value(pairs[i]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Delete many values in insertion order without repeated array shifts.
     *
     * @param predicate non-mutating predicate or captured predicate key, according to the operation
     * @return number of removed entries
     */

    int removeIf(final Predicate<V> predicate) {
        if (size == 0) {
            return 0;
        }
        if (size == 1) {
            if (!predicate.test(value(entries))) {
                return 0;
            }
            singletonKey = null;
            entries = null;
            size = 0;
            return 1;
        }
        final int previous = size;
        Object[] pairs;
        if (entries instanceof DenseTermTable<?>) {
            final int removed = table().removeIf(predicate);
            if (removed == 0) {
                return 0;
            }
            size -= removed;
            if (size > ARRAY_LIMIT / 2) {
                return removed;
            }
            pairs = copyTablePairs();
        } else {
            pairs = filterArray(predicate, previous);
            if (size == previous) {
                return 0;
            }
        }
        retainPairs(pairs);
        return previous - size;
    }

    /**
     * Copies dense survivors into the flat representation in encounter order.
     *
     * @return ordered key/value pairs of the current dense survivors
     */
    private Object[] copyTablePairs() {
        final Object[] pairs = new Object[size * 2];
        final Object[] compacted = pairs;
        final int[] offset = { 0 };
        table().forEach((key, value) -> {
            compacted[offset[0]++] = key;
            compacted[offset[0]++] = value;
        });
        return pairs;
    }

    /**
     * Compacts surviving flat pairs and clears the vacated references.
     *
     * @param predicate non-mutating predicate or captured predicate key, according to the operation
     * @param previous  live cardinality before filtering
     * @return ordered surviving pairs, possibly in a smaller buffer
     */
    private Object[] filterArray(final Predicate<V> predicate, final int previous) {
        Object[] pairs = (Object[]) entries;
        int target = 0;
        for (int i = 0; i < previous * 2; i += 2) {
            if (predicate.test(value(pairs[i + 1]))) {
                continue;
            }
            pairs[target++] = pairs[i];
            pairs[target++] = pairs[i + 1];
        }
        size = target / 2;
        if (size == previous) {
            return pairs;
        }
        Arrays.fill(pairs, target, previous * 2, null);
        if (size > 1 && size * 4 <= pairs.length) {
            pairs = Arrays.copyOf(pairs, size * 2);
        }
        return pairs;
    }

    /**
     * Selects the empty, singleton or flat representation for an ordered array of survivors.
     *
     * @param pairs ordered key/value pairs for live survivors
     */
    private void retainPairs(final Object[] pairs) {
        entries = switch (size) {
        case 0 -> {
            singletonKey = null;
            yield null;
        }
        case 1 -> {
            singletonKey = (TermKey) pairs[0];
            yield pairs[1];
        }
        default -> pairs;
        };
    }

    /**
     * Accumulates a value-derived count over live entries in encounter order.
     *
     * @param function non-mutating value-to-count function
     * @return sum over live values
     */
    long sum(final ToLongFunction<V> function) {
        if (size == 0) {
            return 0;
        }
        if (size == 1) {
            return function.applyAsLong(value(entries));
        }
        long result = 0;
        if (entries instanceof DenseTermTable<?>) {
            return table().sum(function);
        }
        final Object[] pairs = (Object[]) entries;
        for (int i = 1; i < size * 2; i += 2) {
            result += function.applyAsLong(value(pairs[i]));
        }
        return result;
    }

    /**
     * Visits every live entry in encounter order; the callback must not mutate this container.
     *
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     */
    void forEach(final BiConsumer<TermKey, V> visitor) {
        if (size == 1) {
            visitor.accept(singletonKey, value(entries));
        } else if (size > 1) {
            if (entries instanceof DenseTermTable<?>) {
                table().forEach(visitor);
            } else {
                final Object[] pairs = (Object[]) entries;
                for (int i = 0; i < size * 2; i += 2) {
                    visitor.accept((TermKey) pairs[i], value(pairs[i + 1]));
                }
            }
        }
    }

    /**
     * Allocation-free positional traversal, only while the enclosing partition is retained.
     *
     * @param from inclusive starting offset of positional traversal
     * @return the next live offset, or -1 at exhaustion
     */

    int nextIndex(final int from) {
        if (entries instanceof DenseTermTable<?>) {
            return table().nextIndex(from);
        }
        return from < size ? from : -1;
    }

    /**
     * Reads the value at a live position supplied by positional traversal.
     *
     * @param index live positional offset returned by this container
     * @return the non-null value at the supplied live position
     */
    V valueAt(final int index) {
        if (entries instanceof DenseTermTable<?>) {
            return table().valueAt(index);
        }
        if (size == 1) {
            return value(entries);
        }
        return value(((Object[]) entries)[index * 2 + 1]);
    }
}
