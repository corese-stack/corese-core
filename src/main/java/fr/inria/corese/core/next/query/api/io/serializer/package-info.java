/**
 * Contracts for serializing SPARQL tuple and boolean results.
 *
 * <p>Implementations select a serializer by {@link
 * fr.inria.corese.core.next.query.api.io.format.ResultFormat} and report
 * unsupported formats explicitly. Most callers should configure serialization
 * with {@link fr.inria.corese.core.next.query.api.io.serializer.ResultSerializerOptions};
 * capability interfaces for advanced implementations live in the
 * {@code option} subpackage.</p>
 */
package fr.inria.corese.core.next.query.api.io.serializer;
