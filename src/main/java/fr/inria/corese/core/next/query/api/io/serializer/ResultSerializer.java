package fr.inria.corese.core.next.query.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.serializer.Serializer;

/**
 * Serializer for a SPARQL SELECT or ASK result.
 *
 * <p>Tuple sources are consumed once. Serializers do not close the query result
 * or the caller-owned destination writer.</p>
 */
public interface ResultSerializer extends Serializer {
}
