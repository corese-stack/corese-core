package fr.inria.corese.core.sparql.storage.cache;

/**
 * Interface for implementing cache ICache.java
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @param <K>
 * @param <V>
 * @date 4 févr. 2015
 */
public interface ICache<K, V> {

    int size();

    V get(Object key);

    V put(K key, V value);

    boolean containsKey(Object key);

    void clear();
}
