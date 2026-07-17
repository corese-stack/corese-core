package fr.inria.corese.core.next.query.impl.parser;

/**
 * Marker interface for configuration objects used by {@link QueryParser}
 * implementations.
 *
 * <p>Implementations of this interface define parser-specific options
 * that influence how a query is parsed and validated. Examples of such
 * options may include:</p>
 *
 * <ul>
 *   <li>Base IRI resolution</li>
 *   <li>Strict or permissive parsing modes</li>
 *   <li>Error handling strategies (fail-fast vs. error collection)</li>
 * </ul>
 *
 * <p>This interface does not define any methods directly. It serves as
 * a common supertype for all query parsing configuration classes.</p>
 *
 * @see QueryParser
 */
public interface QueryOptions {
}
