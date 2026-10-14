package fr.inria.corese.core.next.storage.impl.memory;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Insertion-ordered term map backed by parallel entry arrays and a hash directory.
 *
 * <p>
 * {@code keys}, {@code data} and {@code hashes} have the same capacity. A live entry occupies the same offset in all
 * three arrays; a null key marks a hole. {@code size} counts live entries and {@code used} bounds the occupied prefix,
 * including holes. The power-of-two {@code positions} directory uses linear probing and stores entry offsets plus one,
 * with zero denoting a vacant bucket. Cached full structural hashes reject unequal terms before RDF equality is tested.
 * Keys and values must be non-null.
 *
 * <p>
 * Insertion appends to the ordered arrays. Removal repairs the probe cluster without moving surviving entries; periodic
 * compaction packs entries stably and rebuilds the directory. Batch filtering performs one stable compaction. None of
 * these operations changes the encounter order of surviving entries.
 *
 * <p>
 * Copy constructors clone every owned array while sharing keys and, unless a value mapper is supplied, values. The
 * caller must copy mutable nested values. This class is not thread-safe: mutations require exclusive partition
 * ownership under the database monitor. Iterators are not fail-fast and may only traverse an unchanged table, either
 * under that monitor or through a retained snapshot.
 *
 * <p>
 * Equality and hashing retain the inherited Map entry semantics; capacities and holes are implementation state.
 *
 * @param <V> non-null entry value type
 */
@SuppressWarnings("java:S2160")
final class DenseTermTable<V> extends AbstractMap<TermKey, V> {
    /**
     * Maximum power-of-two directory capacity.
     */
    private static final int MAX_BUCKETS = 1 << 30;
    /**
     * Parallel immutable term keys; null slots denote deleted entries.
     */
    private TermKey[] keys;
    /**
     * Parallel non-null live values at the same offsets as the keys.
     */
    private Object[] data;
    /**
     * Linear-probe directory storing live entry offsets plus one.
     */
    private int[] positions;
    /**
     * Parallel cached structural hashes for filtering RDF equality checks.
     */
    private int[] hashes;
    /**
     * Number of live entries or statements owned by this container.
     */
    private int size;
    /**
     * Bound of the occupied entry prefix, including holes.
     */
    private int used;

    /**
     * Creates an empty dense table with the requested initial capacity.
     */
    DenseTermTable() {
        this(8);
    }

    /**
     * Creates an empty dense table with the requested initial capacity.
     *
     * @param capacity requested initial entry capacity
     * @throws IllegalArgumentException if a capacity, term type or requested isolation level is unsupported
     */
    DenseTermTable(int capacity) {
        if (capacity < 0 || capacity > MAX_BUCKETS / 2) {
            throw new IllegalArgumentException("Invalid capacity");
        }
        capacity = Math.max(8, capacity);
        keys = new TermKey[capacity];
        data = new Object[capacity];
        int buckets = 16;
        while (capacity > (buckets >>> 1)) {
            buckets <<= 1;
        }
        positions = new int[buckets];
        hashes = new int[capacity];
    }

    /**
     * Copies buffers, capacities and holes without rehashing or reinserting entries.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     */

    DenseTermTable(final DenseTermTable<V> source) {
        keys = source.keys.clone();
        data = source.data.clone();
        positions = source.positions.clone();
        hashes = source.hashes.clone();
        size = source.size;
        used = source.used;
    }

    /**
     * Copy the ordered storage/directory, transforming only live values.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     * @param values mapper for live values in the copied, independently owned buffers
     */

    DenseTermTable(final DenseTermTable<V> source, final Function<? super V, ? extends V> values) {
        this(source);
        mapValues(values);
    }

    /**
     * Transform values in owned buffers without changing keys, holes or the directory.
     *
     * @param values mapper for live values in the copied, independently owned buffers
     * @throws NullPointerException if an explicitly required argument or mapped value is null
     */

    void mapValues(final Function<? super V, ? extends V> values) {
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                data[i] = Objects.requireNonNull(values.apply(value(i)));
            }
        }
    }

    /**
     * Mixes a cached structural hash for the power-of-two probe directory.
     *
     * @param hash cached structural hash of the captured RDF value
     * @return the mixed hash used to select a probe bucket
     */
    private static int spread(final int hash) {
        final int mixed = hash * 0x9E3779B9;
        return mixed ^ (mixed >>> 16);
    }

    /**
     * Finds the probe bucket containing a key or the vacant bucket for its insertion.
     *
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @return the matching bucket or the vacant insertion bucket
     */
    private int slot(final TermKey key) {
        final int hash = key.hashCode();
        final int mask = positions.length - 1;
        int slot = spread(hash) & mask;

        for (;;) {
            final int index = positions[slot] - 1;
            if (index < 0) {
                return slot;
            }
            if (hashes[index] == hash && key.equals(keys[index])) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
    }

    /**
     * Reads an immutable RDF value or a value held in the container representation.
     *
     * @param index live positional offset returned by this container
     * @return the stored RDF value or the non-null entry value at the specified position
     */
    @SuppressWarnings("unchecked")
    private V value(final int index) {
        return (V) data[index];
    }

    /**
     * {@inheritDoc}
     *
     * @return live entry count
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     *
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @return the matching value, or null if absent
     */
    @Override
    public V get(final Object key) {
        if (!(key instanceof TermKey term)) {
            return null;
        }
        final int index = positions[slot(term)] - 1;
        return index < 0 ? null : value(index);
    }

    /**
     * Search an already captured RDF value without constructing a transient TermKey.
     *
     * @param value non-null RDF value or entry value
     * @param hash  cached structural hash of the captured RDF value
     * @return the matching value, or null if absent
     */

    V get(final fr.inria.corese.core.next.data.api.term.Value value, final int hash) {
        final int mask = positions.length - 1;
        int slot = spread(hash) & mask;

        for (;;) {
            final int index = positions[slot] - 1;
            if (index < 0) {
                return null;
            }
            if (hashes[index] == hash && keys[index].matchesLookup(value, hash)) {
                return value(index);
            }
            slot = (slot + 1) & mask;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param key   search or insertion key; null denotes a wildcard only for pattern traversal
     * @param value non-null RDF value or entry value
     * @return the existing value, or null after insertion
     */
    @Override
    public V putIfAbsent(final TermKey key, final V value) {
        return insert(key, value, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param key   search or insertion key; null denotes a wildcard only for pattern traversal
     * @param value non-null RDF value or entry value
     * @return the previous value, or null for a new key
     */
    @Override
    public V put(final TermKey key, final V value) {
        return insert(key, value, null, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param key     search or insertion key; null denotes a wildcard only for pattern traversal
     * @param factory value factory invoked only for an absent key
     * @return the existing or computed value, or null if computation returned null
     */
    @Override
    public V computeIfAbsent(final TermKey key, final Function<? super TermKey, ? extends V> factory) {
        return insert(key, null, factory, false);
    }

    /**
     * Inserts an entry or returns its previous value using the requested replacement policy.
     *
     * @param key     search or insertion key; null denotes a wildcard only for pattern traversal
     * @param value   non-null RDF value or entry value
     * @param factory value factory invoked only for an absent key
     * @param replace whether an existing value may be replaced
     * @return the existing value, the computed absent value, or null after direct insertion
     * @throws NullPointerException if an explicitly required argument or mapped value is null
     */
    private V insert(final TermKey key, final V value, final Function<? super TermKey, ? extends V> factory,
            final boolean replace) {
        int slot = slot(key);
        final int index = positions[slot] - 1;

        if (index >= 0) {
            final V previous = value(index);
            if (replace) {
                data[index] = Objects.requireNonNull(value);
            }
            return previous;
        }
        final V inserted = factory == null ? Objects.requireNonNull(value) : factory.apply(key);
        if (inserted == null) {
            return null;
        }
        if (prepareInsertionSpace()) {
            slot = slot(key);
        }
        keys[used] = key;
        data[used] = inserted;
        hashes[used] = key.hashCode();
        positions[slot] = ++used;
        size++;
        return factory == null ? null : inserted;
    }

    /**
     * Reserves entry and directory capacity without changing surviving entry order.
     *
     * @return true if entry positions were compacted or the directory was rebuilt
     * @throws OutOfMemoryError if the maximum supported directory capacity is exhausted
     */
    private boolean prepareInsertionSpace() {
        boolean reindexed = false;
        if (used == keys.length) {
            if (used - size >= Math.max(1, size >>> 2)) {
                compact();
                reindexed = true;
            } else {
                final int capacity = Math.addExact(keys.length, Math.max(1, keys.length >>> 1));
                keys = Arrays.copyOf(keys, capacity);
                data = Arrays.copyOf(data, capacity);
                hashes = Arrays.copyOf(hashes, capacity);
            }
        }
        if (size + 1 > (positions.length >>> 1)) {
            if (positions.length == MAX_BUCKETS) {
                throw new OutOfMemoryError("Term table capacity exhausted");
            }
            rehash(positions.length << 1);
            reindexed = true;
        }
        return reindexed;
    }

    /**
     * {@inheritDoc}
     *
     * @param key         search or insertion key; null denotes a wildcard only for pattern traversal
     * @param replacement non-null replacement for an existing value
     * @return the previous value, or null if the key was absent
     * @throws NullPointerException if an explicitly required argument or mapped value is null
     */
    @Override
    public V replace(final TermKey key, final V replacement) {
        final int index = positions[slot(key)] - 1;
        if (index < 0) {
            return null;
        }
        final V previous = value(index);
        data[index] = Objects.requireNonNull(replacement);
        return previous;
    }

    /**
     * {@inheritDoc}
     *
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @return number of matching statements removed
     */
    @Override
    public V remove(final Object key) {
        if (!(key instanceof TermKey term)) {
            return null;
        }
        final int hole = slot(term);
        final int index = positions[hole] - 1;

        if (index < 0) {
            return null;
        }
        final V previous = value(index);
        keys[index] = null;
        data[index] = null;
        size--;
        closeProbeGap(hole);
        compactAfterRemoval();
        return previous;
    }

    /**
     * Repairs the linear-probe cluster after a bucket is removed.
     *
     * @param hole vacant bucket whose probe cluster must be repaired
     */
    private void closeProbeGap(int hole) {
        final int mask = positions.length - 1;
        for (int next = (hole + 1) & mask; positions[next] != 0; next = (next + 1) & mask) {
            final int home = spread(hashes[positions[next] - 1]) & mask;
            if (((next - home) & mask) >= ((next - hole) & mask)) {
                positions[hole] = positions[next];
                hole = next;
            }
        }
        positions[hole] = 0;
    }

    /**
     * Applies the existing compaction and buffer-shrink thresholds after a single removal.
     */
    private void compactAfterRemoval() {
        if (size == 0) {
            used = 0;
            if (keys.length > 16) {
                resetBuffers();
            }
        } else if (used >= 64 && size <= used / 2) {
            compact();
            if (size <= keys.length / 4) {
                shrinkEntryBuffers();
            }
        }
    }

    /**
     * Replaces oversized empty buffers with the minimum entry and directory capacities.
     */
    private void resetBuffers() {
        keys = new TermKey[8];
        data = new Object[8];
        hashes = new int[8];
        positions = new int[16];
    }

    /**
     * Shrinks parallel entry arrays together; live offsets and the directory remain valid.
     */
    private void shrinkEntryBuffers() {
        final int capacity = Math.max(8, size * 2);
        keys = Arrays.copyOf(keys, capacity);
        data = Arrays.copyOf(data, capacity);
        hashes = Arrays.copyOf(hashes, capacity);
    }

    /**
     * Packs live parallel entries stably and rebuilds the probe directory.
     */
    private void compact() {
        // Stable packing bounds hole scans. Reuse the directory during churn.
        int target = 0;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                keys[target] = keys[i];
                hashes[target] = hashes[i];
                data[target++] = data[i];
            }
        }
        Arrays.fill(keys, target, used, null);
        Arrays.fill(data, target, used, null);
        used = target;
        Arrays.fill(positions, 0);
        fillPositions();
    }

    /**
     * Stable batch filtering. The predicate must not access or mutate this table.
     *
     * @param predicate non-mutating predicate or captured predicate key, according to the operation
     * @return number of removed entries
     */

    int removeIf(final Predicate<V> predicate) {
        final int previous = size;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null && predicate.test(value(i))) {
                keys[i] = null;
                data[i] = null;
                size--;
            }
        }
        if (size == previous) {
            return 0;
        }
        compact();
        if (size == 0 && keys.length > 16) {
            resetBuffers();
        } else if (size > 0 && size <= keys.length / 4) {
            shrinkEntryBuffers();
        }
        return previous - size;
    }

    /**
     * Replaces the probe directory and indexes all live entry positions.
     *
     * @param capacity requested initial entry capacity
     */
    private void rehash(final int capacity) {
        positions = new int[capacity];
        fillPositions();
    }

    /**
     * Indexes live entry offsets in the current empty probe directory.
     */
    private void fillPositions() {
        final int mask = positions.length - 1;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                int slot = spread(hashes[i]) & mask;
                while (positions[slot] != 0) {
                    slot = (slot + 1) & mask;
                }
                positions[slot] = i + 1;
            }
        }
    }

    /**
     * Traverses matching statements or live values until the visitor requests an early stop.
     *
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     * @return true when a matching value caused the visitor to request an early stop
     */
    boolean visit(final Predicate<V> visitor) {
        for (int i = 0; i < used; i++) {
            if (keys[i] != null && visitor.test(value(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Positions in an immutable, retained partition; holes keep their encounter order.
     *
     * @param from inclusive starting offset of positional traversal
     * @return the next live offset, or -1 at exhaustion
     */

    int nextIndex(int from) {
        while (from < used && keys[from] == null) {
            from++;
        }
        return from < used ? from : -1;
    }

    /**
     * Reads the value at a live position supplied by positional traversal.
     *
     * @param index live positional offset returned by this container
     * @return the non-null value at the supplied live position
     */
    V valueAt(final int index) {
        return value(index);
    }

    /**
     * Accumulates a value-derived count over live entries in encounter order.
     *
     * @param function non-mutating value-to-count function
     * @return sum over live values
     */
    long sum(final ToLongFunction<V> function) {
        long total = 0;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                total += function.applyAsLong(value(i));
            }
        }
        return total;
    }

    /**
     * {@inheritDoc}
     *
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     */
    @Override
    public void forEach(final BiConsumer<? super TermKey, ? super V> visitor) {
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                visitor.accept(keys[i], value(i));
            }
        }
    }

    /**
     * Ordered traversal over an unchanged table; it must not overlap mutation of its buffers.
     *
     * @param <T> ordered traversal item type
     */
    private abstract class OrderedIterator<T> implements Iterator<T> {

        /** Creates an ordered iterator beginning at the first entry offset. */
        private OrderedIterator() {
        }

        /**
         * Next ordered entry offset to inspect.
         */
        private int next;

        /**
         * Reports whether this unchanged ordered traversal has another live item.
         *
         * @return true when another live entry remains
         */
        public boolean hasNext() {
            while (next < used && keys[next] == null) {
                next++;
            }
            return next < used;
        }

        /**
         * Returns the next match in this sequential traversal.
         *
         * @return the next item in encounter order
         * @throws NoSuchElementException if this ordered traversal is exhausted
         */
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return item(next++);
        }

        /**
         * Reads the item at the live offset selected by this iterator.
         *
         * @param index live positional offset returned by this container
         * @return the item at the supplied live offset
         */
        abstract T item(final int index);
    }

    /**
     * {@inheritDoc}
     *
     * @return an insertion-ordered view of live map entries
     */
    @Override
    public Set<Entry<TermKey, V>> entrySet() {
        return new AbstractSet<>() {
            /**
             * Returns the number of live entries.
             *
             * @return live entry count
             */
            public int size() {
                return DenseTermTable.this.size;
            }

            /**
             * Creates an ordered iterator over this unchanged table view.
             *
             * @return an insertion-ordered iterator of the unchanged view
             */
            public Iterator<Entry<TermKey, V>> iterator() {
                return new OrderedIterator<>() {
                    /**
                     * Reads the item at the live offset selected by this iterator.
                     *
                     * @param i live positional offset supplied by traversal
                     * @return the item at the supplied live offset
                     */
                    Entry<TermKey, V> item(final int i) {
                        return new SimpleImmutableEntry<>(keys[i], value(i));
                    }
                };
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return an insertion-ordered view of live values
     */
    @Override
    public Collection<V> values() {
        return new AbstractCollection<>() {
            /**
             * Returns the number of live entries.
             *
             * @return live entry count
             */
            public int size() {
                return DenseTermTable.this.size;
            }

            /**
             * Creates an ordered iterator over this unchanged table view.
             *
             * @return an insertion-ordered iterator of the unchanged view
             */
            public Iterator<V> iterator() {
                return new OrderedIterator<>() {
                    /**
                     * Reads the item at the live offset selected by this iterator.
                     *
                     * @param i live positional offset supplied by traversal
                     * @return the item at the supplied live offset
                     */
                    V item(final int i) {
                        return value(i);
                    }
                };
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return an insertion-ordered view of live term keys
     */
    @Override
    public Set<TermKey> keySet() {
        return new AbstractSet<>() {
            /**
             * Returns the number of live entries.
             *
             * @return live entry count
             */
            public int size() {
                return DenseTermTable.this.size;
            }

            /**
             * Creates an ordered iterator over this unchanged table view.
             *
             * @return an insertion-ordered iterator of the unchanged view
             */
            public Iterator<TermKey> iterator() {
                return new OrderedIterator<>() {
                    /**
                     * Reads the item at the live offset selected by this iterator.
                     *
                     * @param i live positional offset supplied by traversal
                     * @return the item at the supplied live offset
                     */
                    TermKey item(final int i) {
                        return keys[i];
                    }
                };
            }
        };
    }
}
