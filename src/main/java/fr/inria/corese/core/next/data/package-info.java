/**
 * Core RDF data model, value representation, and terminology for Corese v5.
 *
 * <h2>Three-Tier Package Architecture</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.data.api}: Public API contracts (interfaces and models)
 *       intended for end-users, query evaluators, and client applications. Includes {@code term},
 *       {@code model}, {@code factory}, {@code namespace}, and {@code literal}.</li>
 *   <li>{@link fr.inria.corese.core.next.data.spi}: Service-provider interfaces (SPI) intended for
 *       backend implementers, custom datatypes, and parser/serializer plugins.</li>
 *   <li>{@link fr.inria.corese.core.next.data.impl}: Internal, encapsulated default implementations
 *       ({@code SimpleModel}, {@code SimpleStatement}, internal dictionary encoders).</li>
 * </ul>
 *
 * <h2>Entry Points</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.data.Values}: Static convenience factory for creating
 *       IRIs, blank nodes, typed literals, statements, and RDF-star triple terms.</li>
 *   <li>{@link fr.inria.corese.core.next.data.api.model.Model}: Mutable and immutable in-memory
 *       RDF graphs supporting collection-like set operations, namespace mappings, and filtered views.</li>
 * </ul>
 *
 * <h2>Thread Safety and Immutability</h2>
 * <p>All RDF terms ({@link fr.inria.corese.core.next.data.api.term.IRI},
 * {@link fr.inria.corese.core.next.data.api.term.Literal},
 * {@link fr.inria.corese.core.next.data.api.term.BNode}, and
 * {@link fr.inria.corese.core.next.data.api.model.Statement}) are strictly immutable and thread-safe.
 * {@link fr.inria.corese.core.next.data.api.model.Model} instances are not thread-safe unless
 * explicitly decorated or backed by a transactional storage manager.</p>
 */
package fr.inria.corese.core.next.data;
