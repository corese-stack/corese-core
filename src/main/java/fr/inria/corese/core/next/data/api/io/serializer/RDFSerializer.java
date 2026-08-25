package fr.inria.corese.core.next.data.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;

/**
 * Serializer of RDF statements to a specified {@link RDFFormat}.
 *
 * <p>The source is consumed once. The serializer does not close the destination
 * writer. A serializer created from a one-shot statement source may not be
 * reused.</p>
 */
public interface RDFSerializer extends Serializer {

}
