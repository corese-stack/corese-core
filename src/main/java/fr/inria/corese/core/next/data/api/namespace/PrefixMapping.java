package fr.inria.corese.core.next.data.api.namespace;

import java.util.Map;
import java.util.Set;

/**
 * Mutable collection of {@link Namespace} bindings used to expand and compact
 * prefixed RDF names across formats.
 */
public interface PrefixMapping {

    /**
     * Gets the namespace IRI for a given prefix.
     *
     * @param prefix the prefix to look up
     * @return the namespace IRI
     * or null if the prefix is not registered
     * @throws IllegalArgumentException if prefix is null
     */
    String getNamespace(String prefix);

    /**
     * Gets a prefix for a given namespace IRI.
     * If multiple prefixes are mapped to the same namespace,
     * the result is implementation-dependent.
     *
     * @param namespace the namespace IRI to look up
     * @return the prefix (e.g., "ex"), or null if the namespace is not registered
     * @throws IllegalArgumentException if namespace is null
     */
    String getPrefix(String namespace);

    /**
     * Checks if a prefix is registered.
     *
     * @param prefix the prefix to check
     * @return true if the prefix exists in mappings, false otherwise
     */
    boolean hasPrefix(String prefix);

    /**
     * Checks if a namespace has a prefix
     *
     * @param namespace the namespace to check
     * @return true if the namespace exists in mappings, false otherwise
     */
    boolean hasNamespace(String namespace);

    /**
     * Returns all registered prefixes.
     * Order of iteration is implementation-dependent but should be consistent
     * (typically insertion order).
     *
     * @return an immutable set of all prefixes (including empty prefix if registered)
     */
    Set<String> getPrefixes();

    /**
     * Returns all registered namespace IRIs.
     *
     * @return an immutable set of all namespace IRIs
     */
    Set<String> getNamespaces();

    /**
     * Returns all namespace mappings as an immutable map.
     *
     * @return a map where keys are prefixes and values are namespace IRIs
     */
    Map<String, String> getPrefixMap();

    /**
     * Returns all namespaces mappings as an unmodifiable map.
     *
     * @return an unmodifiable map where keys are namespaces IRIs and values are prefixes
     */
    Map<String, String> getNamespaceMap();


    /**
     * Sets or updates a prefix mapping.
     * If the prefix already exists, its old namespace is unregistered.
     *
     * @param prefix    the prefix
     * @param namespace the namespace IRI
     * @throws IllegalArgumentException if prefix or namespace is invalid/null
     * @throws IllegalArgumentException if prefix doesn't match XML NCName rules
     *                                  (except empty string which is always valid)
     */
    void setPrefix(String prefix, String namespace);

    /**
     * Sets a namespace using a Namespace object.
     * Equivalent to setPrefix(namespace.getPrefix(), namespace.getName()).
     *
     * @param namespace the Namespace object to register
     * @throws IllegalArgumentException if namespace is null or invalid
     */
    void setNamespace(Namespace namespace);

    /**
     * Removes a prefix mapping.
     *
     * @param prefix the prefix to remove
     * @return true if the prefix was registered and removed, false otherwise
     */
    boolean removePrefix(String prefix);

    /**
     * Removes all prefix mappings.
     * This does not affect the default namespace.
     */
    void clear();

    /**
     * Gets the default namespace (used when no prefix is specified).
     *
     * @return the default namespace IRI, or null if not set
     */
    String getDefaultNamespace();

    /**
     * Sets the default namespace.
     * This is typically used when parsing/serializing prefixed names without a prefix.
     *
     * @param namespace the default namespace IRI, or null to unset
     */
    void setDefaultNamespace(String namespace);

    /**
     * Expands a prefixed name to a full IRI.
     *
     * @param prefixedName the prefixed name to expand (e.g., "ex:name")
     * @return the expanded full IRI, or null if prefix is not registered
     * @throws IllegalArgumentException if prefixedName is null
     */
    String expandPrefix(String prefixedName);

    /**
     * Compresses a full IRI to a prefixed name if possible.
     * Uses the longest matching namespace prefix.
     *
     * @param iri the full IRI to compress
     * @return the prefixed name if a matching namespace is found,
     * otherwise the original IRI
     * @throws IllegalArgumentException if iri is null
     */
    String compressIRI(String iri);

    /**
     * Checks if a prefix is valid according to XML NCName rules.
     *
     * @param prefix the prefix to validate
     * @return true if the prefix is valid, false otherwise
     */
    boolean isValidPrefix(String prefix);

    /**
     * Copies all prefix mappings from another handler.
     * Existing mappings in this handler are overwritten if prefixes match.
     *
     * @param other the handler to copy from
     * @throws IllegalArgumentException if other is null
     */
    void copyFrom(PrefixMapping other);

    /**
     * Returns the number of registered prefix mappings.
     *
     * @return the count of prefix-namespace pairs
     */
    int size();

    /**
     * Checks if there are no registered prefix mappings.
     *
     * @return true if size() == 0, false otherwise
     */
    boolean isEmpty();

    /**
     * Creates a deep copy of this handler.
     * The returned handler is independent and can be modified without
     * affecting the original.
     *
     * @return a new PrefixMapping instance with same mappings
     */
    @SuppressWarnings("java:S2975")
    PrefixMapping clone();
}
