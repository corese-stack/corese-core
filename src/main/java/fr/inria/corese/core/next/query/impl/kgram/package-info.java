/**
 * Internal KGRAM execution engine used by the next SPARQL pipeline.
 *
 * <p>This is a deliberately isolated, transitional fork of the historical
 * engine. It is implementation detail, even where copied subpackages retain
 * names such as {@code api}. Applications must use {@code next.query.api} and
 * must not depend on types below this package.</p>
 *
 * <p>The package preserves the KGRAM execution model while it is decomposed
 * into responsibility-based {@code next} components. Its name documents that
 * origin; it is not intended to become another public query API.</p>
 */
package fr.inria.corese.core.next.query.impl.kgram;
