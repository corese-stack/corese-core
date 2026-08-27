/**
 * Public RDF data contracts.
 *
 * <ul>
 *   <li>{@code term}: RDF values and resources;</li>
 *   <li>{@code model}: statements and RDF models;</li>
 *   <li>{@code namespace}: namespace and prefix mappings;</li>
 *   <li>{@code factory}: creation of RDF values;</li>
 *   <li>{@code literal}: known literal datatypes;</li>
 *   <li>{@code vocabulary}: standard RDF vocabulary terms;</li>
 *   <li>{@code io}: parsing and serialization contracts.</li>
 * </ul>
 *
 * <p>Reusable bases for providers and custom implementations belong to
 * {@code next.data.spi}, rather than to this contract-only namespace.</p>
 *
 * <p>This is the public boundary of the {@code next.data} layer. Implementation
 * details, Corese-legacy adapters, and storage wiring belong in {@code next.data.impl}
 * and must not be referenced from here.</p>
 */
package fr.inria.corese.core.next.data.api;
